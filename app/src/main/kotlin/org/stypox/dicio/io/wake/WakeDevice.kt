package org.stypox.dicio.io.wake

import kotlinx.coroutines.flow.StateFlow

interface WakeDevice {
    val state: StateFlow<WakeState>

    fun download()

    /**
     * This is blocking and should be called only from the background service.
     * @return `true` if the wake word was detected
     */
    fun processFrame(audio16bitPcm: ShortArray): Boolean

    /**
     * @return the size of audio frames passed to [processFrame]
     */
    fun frameSize(): Int

    fun destroy()

    /**
     * @return `true` only if the model has been loading and is using up precious system resources.
     * Otherwise, return `false`, so that the user of the [WakeDevice] knows it doesn't need to
     * [destroy] this instance to save resources (and possibly recreate it later if needed). This is
     * especially important in case an error occurred during loading, to avoid the error from being
     * destroyed along with the model as soon as [WakeService.onDestroy] ((([WakeService] is stopped
     * if there was an error loading the model))) calls
     * [org.stypox.dicio.di.WakeDeviceWrapper.reinitializeToReleaseResources].
     */
    fun isOccupyingResources(): Boolean

    /**
     * Returns `true` if the wake word is "Hey Dicio", `false` if a custom model is being used
     */
    fun isHeyDicio(): Boolean
}
