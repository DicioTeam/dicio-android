package org.stypox.dicio.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.dicio.skill.context.SpeechOutputDevice
import org.stypox.dicio.io.audio.AudioFocusManager
import org.stypox.dicio.io.speech.AndroidTtsSpeechDevice
import org.stypox.dicio.io.speech.NothingSpeechDevice
import org.stypox.dicio.io.speech.SnackbarSpeechDevice
import org.stypox.dicio.io.speech.ToastSpeechDevice
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_ANDROID_TTS
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_NOTHING
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_SNACKBAR
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_TOAST
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_UNSET
import org.stypox.dicio.settings.datastore.SpeechOutputDevice.UNRECOGNIZED
import org.stypox.dicio.settings.datastore.UserSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechOutputDeviceWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<UserSettings>,
    private val localeManager: LocaleManager,
    private val audioFocusManager: AudioFocusManager,
    // this is always instantiated, but will do nothing if
    // it is not the speech device chosen by the user
    private val snackbarSpeechDevice: SnackbarSpeechDevice,
) : SpeechOutputDevice {

    // instantiate SpeechOutputDevices on the main thread
    private val scope = CoroutineScope(Dispatchers.Main)
    private var wrappedSpeechDevice: SpeechOutputDevice = NothingSpeechDevice()

    init {
        scope.launch {
            dataStore.data
                .combine(localeManager.locale) { userSettings, locale ->
                    Pair(userSettings.speechOutputDevice, locale)
                }
                .distinctUntilChanged()
                .collect { (setting, locale) ->
                    // TODO avoid using locale here, but delegate listening to locale changes to
                    //  AndroidTtsSpeechDevice, or in alternative make it so that
                    //  SttInputDeviceWrapper works the same way
                    val prevDevice = wrappedSpeechDevice
                    wrappedSpeechDevice = when (setting) {
                        null,
                        UNRECOGNIZED,
                        SPEECH_OUTPUT_DEVICE_UNSET,
                        SPEECH_OUTPUT_DEVICE_ANDROID_TTS -> AndroidTtsSpeechDevice(context, locale, audioFocusManager)
                        SPEECH_OUTPUT_DEVICE_NOTHING -> NothingSpeechDevice()
                        SPEECH_OUTPUT_DEVICE_TOAST -> ToastSpeechDevice(context)
                        SPEECH_OUTPUT_DEVICE_SNACKBAR -> snackbarSpeechDevice
                    }
                    prevDevice.cleanup()
                }
        }
    }


    override fun speak(speechOutput: String) {
        wrappedSpeechDevice.speak(speechOutput)
    }

    override fun stopSpeaking() {
        wrappedSpeechDevice.stopSpeaking()
    }

    override val isSpeaking: Boolean
        get() = wrappedSpeechDevice.isSpeaking

    override fun runWhenFinishedSpeaking(runnable: Runnable) {
        wrappedSpeechDevice.runWhenFinishedSpeaking(runnable)
    }

    override fun cleanup() {
        // never called, nothing to do
        Log.w(TAG, "Unexpected call to SpeechOutputDeviceWrapper.cleanup()")
    }

    companion object {
        val TAG: String = SpeechOutputDeviceWrapper::class.simpleName!!
    }
}
