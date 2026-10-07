package org.stypox.dicio.util

import android.os.Build
import org.stypox.dicio.io.input.parakeet.ParakeetInputDevice
import org.stypox.dicio.io.input.vosk.VoskInputDevice
import org.stypox.dicio.settings.datastore.InputDevice
import java.util.Locale

fun resolveDefaultInputDevice(
    locale: Locale,
    sdkInt: Int = Build.VERSION.SDK_INT,
): InputDevice {
    val voskSupported = LocaleUtils.resolveSupportedLocale(
        locale,
        VoskInputDevice.MODEL_URLS.keys
    ) != null
    val parakeetSupported = sdkInt >= Build.VERSION_CODES.N &&
        LocaleUtils.resolveSupportedLocale(locale, ParakeetInputDevice.MODEL_URLS.keys) != null

    return when {
        voskSupported -> InputDevice.INPUT_DEVICE_VOSK
        parakeetSupported -> InputDevice.INPUT_DEVICE_PARAKEET
        else -> InputDevice.INPUT_DEVICE_VOSK
    }
}

fun resolveInputDeviceSetting(
    setting: InputDevice,
    locale: Locale,
    sdkInt: Int = Build.VERSION.SDK_INT,
): InputDevice {
    return when (setting) {
        InputDevice.UNRECOGNIZED,
        InputDevice.INPUT_DEVICE_UNSET -> resolveDefaultInputDevice(locale, sdkInt)
        InputDevice.INPUT_DEVICE_PARAKEET ->
            if (sdkInt >= Build.VERSION_CODES.N) setting else InputDevice.INPUT_DEVICE_VOSK
        else -> setting
    }
}
