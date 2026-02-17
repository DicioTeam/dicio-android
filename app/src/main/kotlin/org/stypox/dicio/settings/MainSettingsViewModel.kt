package org.stypox.dicio.settings

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.stypox.dicio.di.WakeDeviceWrapper
import org.stypox.dicio.io.wake.oww.OpenWakeWordDevice
import org.stypox.dicio.settings.datastore.InputDevice
import org.stypox.dicio.settings.datastore.Language
import org.stypox.dicio.settings.datastore.SpeechOutputDevice
import org.stypox.dicio.settings.datastore.SttPlaySound
import org.stypox.dicio.settings.datastore.Theme
import org.stypox.dicio.settings.datastore.UserSettings
import org.stypox.dicio.settings.datastore.WakeDevice
import org.stypox.dicio.util.toStateFlowDistinctBlockingFirst
import org.stypox.dicio.settings.datastore.FallbackSkill
import javax.inject.Inject

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    application: Application,
    private val wakeDeviceWrapper: WakeDeviceWrapper?,
    private val dataStore: DataStore<UserSettings>
) : AndroidViewModel(application) {
    // run blocking because the settings screen cannot start if settings have not been loaded yet
    val settingsState = dataStore.data
        .toStateFlowDistinctBlockingFirst(viewModelScope)

    private fun updateData(transform: (UserSettings.Builder) -> Unit) {
        viewModelScope.launch {
            dataStore.updateData {
                it.toBuilder()
                    .apply(transform)
                    .build()
            }
        }
    }

    val isHeyDicio: StateFlow<Boolean> = wakeDeviceWrapper?.isHeyDicio ?: MutableStateFlow(true)

    fun addOwwUserWakeFile(uri: Uri) {
        viewModelScope.launch {
            OpenWakeWordDevice.addUserWakeFile(getApplication(), uri)
            wakeDeviceWrapper?.reinitializeToReleaseResources()
        }
    }

    fun removeOwwUserWakeFile() {
        viewModelScope.launch {
            OpenWakeWordDevice.removeUserWakeFile(getApplication())
            wakeDeviceWrapper?.reinitializeToReleaseResources()
        }
    }

    fun setLanguage(value: Language) =
        updateData { it.setLanguage(value) }
    fun setTheme(value: Theme) =
        updateData { it.setTheme(value) }
    fun setDynamicColors(value: Boolean) =
        updateData { it.setDynamicColors(value) }
    fun setInputDevice(value: InputDevice) =
        updateData { it.setInputDevice(value) }
    fun setWakeDevice(value: WakeDevice) =
        updateData { it.setWakeDevice(value) }
    fun setSpeechOutputDevice(value: SpeechOutputDevice) =
        updateData { it.setSpeechOutputDevice(value) }
    fun setSttPlaySound(value: SttPlaySound) =
        updateData { it.setSttPlaySound(value) }
    fun setAutoFinishSttPopup(value: Boolean) =
        updateData { it.setAutoFinishSttPopup(value) }
    fun setFallbackSkill(value: FallbackSkill) =
        updateData { it.setFallbackSkill(value) }
}
