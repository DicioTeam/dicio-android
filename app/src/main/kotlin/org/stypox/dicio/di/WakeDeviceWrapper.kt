package org.stypox.dicio.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.stypox.dicio.io.wake.WakeDevice
import org.stypox.dicio.io.wake.WakeState
import org.stypox.dicio.io.wake.oww.OpenWakeWordDevice
import org.stypox.dicio.settings.datastore.UserSettings
import org.stypox.dicio.settings.datastore.WakeDevice.UNRECOGNIZED
import org.stypox.dicio.settings.datastore.WakeDevice.WAKE_DEVICE_NOTHING
import org.stypox.dicio.settings.datastore.WakeDevice.WAKE_DEVICE_OWW
import org.stypox.dicio.settings.datastore.WakeDevice.WAKE_DEVICE_UNSET
import org.stypox.dicio.util.distinctUntilChangedBlockingFirst
import javax.inject.Singleton

interface WakeDeviceWrapper {
    /**
     * @see [WakeDevice.state]
     */
    val state: StateFlow<WakeState?>

    /**
     * @see [WakeDevice.isHeyDicio]
     */
    val isHeyDicio: StateFlow<Boolean>

    /**
     * @see [WakeDevice.download]
     */
    fun download()

    /**
     * @see [WakeDevice.processFrame]
     */
    fun processFrame(audio16bitPcm: ShortArray): Boolean

    /**
     * @see [WakeDevice.frameSize]
     */
    fun frameSize(): Int

    /**
     * Destroys and initializes from scratch the current [WakeDevice].
     */
    fun reinitialize()

    /**
     * Same as [reinitialize], but runs only if the current [WakeDevice] is using up precious
     * resources, otherwise does nothing to prevent [WakeState.ErrorLoading]s from being destroyed
     * together with the (failing) [WakeDevice].
     * @see [WakeDevice.isOccupyingResources]
     */
    fun reinitializeToReleaseResources()
}

typealias DataStoreWakeDevice = org.stypox.dicio.settings.datastore.WakeDevice

class WakeDeviceWrapperImpl(
    @param:ApplicationContext private val appContext: Context,
    dataStore: DataStore<UserSettings>,
    private val okHttpClient: OkHttpClient,
) : WakeDeviceWrapper {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentSetting: DataStoreWakeDevice
    private var lastFrameHadWrongSize = false

    // null means that the user has not enabled any STT input device
    private val _state: MutableStateFlow<WakeState?> = MutableStateFlow(null)
    override val state: StateFlow<WakeState?> = _state
    private val _isHeyDicio: MutableStateFlow<Boolean>
    override val isHeyDicio: StateFlow<Boolean>
    private val currentDevice: MutableStateFlow<WakeDevice?>

    init {
        // Run blocking, because the data store is always available right away since LocaleManager
        // also initializes in a blocking way from the same data store.
        val (firstWakeDeviceSetting, nextWakeDeviceFlow) = dataStore.data
            .map { it.wakeDevice }
            .distinctUntilChangedBlockingFirst()

        currentSetting = firstWakeDeviceSetting
        val firstWakeDevice = buildInputDevice(firstWakeDeviceSetting)
        currentDevice = MutableStateFlow(firstWakeDevice)
        _isHeyDicio = MutableStateFlow(firstWakeDevice?.isHeyDicio() ?: true)
        isHeyDicio = _isHeyDicio

        scope.launch {
            currentDevice.collectLatest { newWakeDevice ->
                _isHeyDicio.emit(newWakeDevice?.isHeyDicio() ?: true)
                if (newWakeDevice == null) {
                    _state.emit(null)
                } else {
                    newWakeDevice.state.collect { _state.emit(it) }
                }
            }
        }

        scope.launch {
            nextWakeDeviceFlow.collect(::changeWakeDeviceTo)
        }
    }

    private fun changeWakeDeviceTo(setting: DataStoreWakeDevice) {
        Log.d(TAG, "changeWakeDeviceTo($setting) called")
        currentSetting = setting
        val newWakeDevice = buildInputDevice(setting)
        lastFrameHadWrongSize = false
        currentDevice.update { prevWakeDevice ->
            prevWakeDevice?.destroy()
            newWakeDevice
        }
    }

    private fun buildInputDevice(setting: DataStoreWakeDevice): WakeDevice? {
        return when (setting) {
            UNRECOGNIZED,
            WAKE_DEVICE_UNSET,
            WAKE_DEVICE_OWW -> OpenWakeWordDevice(appContext, okHttpClient)
            WAKE_DEVICE_NOTHING -> null
        }
    }

    override fun download() {
        currentDevice.value?.download()
    }

    override fun processFrame(audio16bitPcm: ShortArray): Boolean {
        val device = currentDevice.value
            ?: throw IllegalArgumentException("No wake word device is enabled")

        if (audio16bitPcm.size != device.frameSize()) {
            if (lastFrameHadWrongSize) {
                // a single badly-sized frame may happen when switching wake device, so we can
                // tolerate it, but otherwise it is a programming error and should be reported
                throw IllegalArgumentException("Wrong audio frame size: expected ${
                    device.frameSize()} samples but got ${audio16bitPcm.size}")
            }
            lastFrameHadWrongSize = true
            return false

        } else {
            // process the frame only if it has the correct size
            lastFrameHadWrongSize = false
            return device.processFrame(audio16bitPcm)
        }
    }

    override fun frameSize(): Int {
        return currentDevice.value?.frameSize() ?: 0
    }

    override fun reinitialize() {
        changeWakeDeviceTo(currentSetting)
    }

    override fun reinitializeToReleaseResources() {
        if (currentDevice.value?.isOccupyingResources() ?: true) {
            reinitialize()
        }
    }

    companion object {
        const val TAG: String = "WakeDeviceWrapper"
    }
}

@Module
@InstallIn(SingletonComponent::class)
class WakeDeviceWrapperModule {
    @Provides
    @Singleton
    fun provideWakeDeviceWrapper(
        @ApplicationContext appContext: Context,
        dataStore: DataStore<UserSettings>,
        okHttpClient: OkHttpClient,
    ): WakeDeviceWrapper {
        return WakeDeviceWrapperImpl(appContext, dataStore, okHttpClient)
    }
}
