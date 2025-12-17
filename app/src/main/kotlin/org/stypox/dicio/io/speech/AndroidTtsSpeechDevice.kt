package org.stypox.dicio.io.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import org.dicio.skill.context.SpeechOutputDevice
import org.stypox.dicio.R
import org.stypox.dicio.io.audio.AudioFocusManager
import java.util.Locale

class AndroidTtsSpeechDevice(
    private var context: Context,
    locale: Locale,
    private val audioFocusManager: AudioFocusManager
) : SpeechOutputDevice {
    private var textToSpeech: TextToSpeech? = null
    private var initializedCorrectly = false
    private val runnablesWhenFinished: MutableList<Runnable> = ArrayList()
    private var lastUtteranceId = 0

    init {
        textToSpeech = TextToSpeech(context) { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.run {
                    val errorCode = setLanguage(locale)
                    if (errorCode >= 0) { // errors are -1 or -2
                        initializedCorrectly = true
                        setOnUtteranceProgressListener(object :
                            UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) {
                                // Ensure audio focus is maintained while TTS is speaking
                                audioFocusManager.onTtsStarted()
                            }
                            override fun onDone(utteranceId: String) {
                                if ("dicio_$lastUtteranceId" == utteranceId) {
                                    // Release audio focus when the last utterance finishes
                                    audioFocusManager.releaseFocus()
                                    // run only when the last enqueued utterance is finished
                                    for (runnable in runnablesWhenFinished) {
                                        runnable.run()
                                    }
                                    runnablesWhenFinished.clear()
                                }
                            }

                            @Suppress("OVERRIDE_DEPRECATION")
                            @Deprecated("")
                            override fun onError(utteranceId: String) {
                                // Release audio focus on error
                                audioFocusManager.releaseFocus()
                            }
                        })
                    } else {
                        Log.e(TAG, "Unsupported language: $errorCode")
                        handleInitializationError(R.string.android_tts_unsupported_language)
                    }
                }
            } else {
                Log.e(TAG, "TTS error: $status")
                handleInitializationError(R.string.android_tts_error)
            }
        }
    }

    override fun speak(speechOutput: String) {
        if (initializedCorrectly) {
            lastUtteranceId += 1
            textToSpeech?.speak(
                speechOutput, TextToSpeech.QUEUE_ADD, null,
                "dicio_$lastUtteranceId"
            )
        } else {
            Toast.makeText(context, speechOutput, Toast.LENGTH_LONG).show()
        }
    }

    override fun stopSpeaking() {
        textToSpeech?.stop()
        // Don't release audio focus here - it gets called when STT starts listening
        // to interrupt any ongoing TTS, but we want to maintain focus through the interaction.
        // Focus will be released when TTS finishes naturally in onDone() or on cleanup()
    }

    override val isSpeaking: Boolean
        get() = textToSpeech?.isSpeaking == true

    override fun runWhenFinishedSpeaking(runnable: Runnable) {
        if (isSpeaking) {
            runnablesWhenFinished.add(runnable)
        } else {
            runnable.run()
        }
    }

    override fun cleanup() {
        textToSpeech?.apply {
            shutdown()
            textToSpeech = null
        }
        // Release audio focus on cleanup
        audioFocusManager.releaseFocus()
    }

    private fun handleInitializationError(@StringRes errorString: Int) {
        Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show()
        cleanup()
    }

    companion object {
        val TAG: String = AndroidTtsSpeechDevice::class.simpleName!!
    }
}
