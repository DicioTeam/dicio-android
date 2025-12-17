package org.stypox.dicio.io.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio focus for the voice assistant to duck other audio playback during interactions.
 * Requests audio focus when STT starts listening and maintains it through TTS playback,
 * releasing it only when the entire interaction is complete.
 */
@Singleton
class AudioFocusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    
    // Required listener - Android needs this even for transient focus
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.v(TAG, "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Another app took focus, we lost it
                hasFocus = false
            }
        }
    }

    /**
     * Requests audio focus to duck other audio. This should be called when STT starts listening.
     * Uses AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to allow other audio to continue at reduced volume.
     */
    @Synchronized
    fun requestFocus() {
        if (hasFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(false)
                .build()

            focusRequest = focusReq
            val result = audioManager.requestAudioFocus(focusReq)
            hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            
            if (!hasFocus) {
                Log.w(TAG, "Audio focus request failed with result: $result")
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            
            if (!hasFocus) {
                Log.w(TAG, "Audio focus request failed (legacy) with result: $result")
            }
        }
    }

    /**
     * Releases audio focus, allowing other audio to return to normal volume.
     * This should be called when TTS finishes or the interaction is canceled.
     */
    @Synchronized
    fun releaseFocus() {
        if (!hasFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { 
                audioManager.abandonAudioFocusRequest(it)
            }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasFocus = false
    }

    /**
     * Called when TTS starts speaking to ensure audio focus is maintained.
     * This is a safety check in case focus was somehow lost between STT and TTS.
     */
    @Synchronized
    fun onTtsStarted() {
        if (!hasFocus) {
            Log.d(TAG, "TTS started without audio focus, requesting now")
            requestFocus()
        }
    }

    companion object {
        private val TAG = AudioFocusManager::class.simpleName
    }
}
