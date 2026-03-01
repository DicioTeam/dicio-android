package org.stypox.dicio.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.stypox.dicio.R
import org.stypox.dicio.settings.datastore.InputDevice
import org.stypox.dicio.settings.datastore.Language
import org.stypox.dicio.settings.datastore.SpeechOutputDevice
import org.stypox.dicio.settings.datastore.SttPlaySound
import org.stypox.dicio.settings.datastore.Theme
import org.stypox.dicio.settings.datastore.WakeDevice
import org.stypox.dicio.settings.ui.BooleanSetting
import org.stypox.dicio.settings.ui.IntSetting
import org.stypox.dicio.settings.ui.ListSetting


@Composable
fun languageSetting() = ListSetting(
    title = stringResource(R.string.pref_language),
    icon = Icons.Default.Language,
    description = stringResource(R.string.pref_language_summary),
    possibleValues = listOf(
        ListSetting.Value(Language.LANGUAGE_SYSTEM, stringResource(R.string.pref_language_system)),
        ListSetting.Value(Language.LANGUAGE_CS, "Čeština"),
        ListSetting.Value(Language.LANGUAGE_DE, "Deutsch"),
        ListSetting.Value(Language.LANGUAGE_EN, "English"),
        ListSetting.Value(Language.LANGUAGE_EN_IN, "English (India)"),
        ListSetting.Value(Language.LANGUAGE_ES, "Español"),
        ListSetting.Value(Language.LANGUAGE_EL, "Ελληνικά"),
        ListSetting.Value(Language.LANGUAGE_FR, "Français"),
        ListSetting.Value(Language.LANGUAGE_IT, "Italiano"),
        ListSetting.Value(Language.LANGUAGE_NL, "Nederlands"),
        ListSetting.Value(Language.LANGUAGE_PL, "Polski"),
        ListSetting.Value(Language.LANGUAGE_RU, "Русский"),
        ListSetting.Value(Language.LANGUAGE_SL, "Slovenščina"),
        ListSetting.Value(Language.LANGUAGE_SV, "Svenska"),
        ListSetting.Value(Language.LANGUAGE_UK, "Українська"),
        ListSetting.Value(Language.LANGUAGE_TR, "Türkçe"),
    ),
)

@Composable
fun themeSetting() = ListSetting(
    title = stringResource(R.string.pref_theme),
    icon = Icons.Default.ColorLens,
    description = stringResource(R.string.pref_theme_summary),
    possibleValues = listOf(
        ListSetting.Value(
            value = Theme.THEME_SYSTEM,
            name = stringResource(R.string.pref_theme_system),
            icon = Icons.Default.PhoneAndroid,
        ),
        ListSetting.Value(
            value = Theme.THEME_SYSTEM_DARK_BLACK,
            name = stringResource(R.string.pref_theme_system_dark_black),
            icon = Icons.Default.PhoneAndroid,
        ),
        ListSetting.Value(
            value = Theme.THEME_LIGHT,
            name = stringResource(R.string.pref_theme_light),
            icon = Icons.Default.LightMode,
        ),
        ListSetting.Value(
            value = Theme.THEME_DARK,
            name = stringResource(R.string.pref_theme_dark),
            icon = Icons.Default.Cloud,
        ),
        ListSetting.Value(
            value = Theme.THEME_BLACK,
            name = stringResource(R.string.pref_theme_black),
            icon = Icons.Default.DarkMode,
        ),
    ),
)

@Composable
fun dynamicColors() = BooleanSetting(
    title = stringResource(R.string.pref_dynamic_colors_title),
    icon = Icons.Default.InvertColors,
    descriptionOff = stringResource(R.string.pref_dynamic_colors_summary),
    descriptionOn = stringResource(R.string.pref_dynamic_colors_summary),
)

@Composable
fun inputDevice() = ListSetting(
    title = stringResource(R.string.pref_input_method),
    icon = Icons.Default.Mic,
    description = stringResource(R.string.pref_input_method_summary),
    possibleValues = listOf(
        ListSetting.Value(
            value = InputDevice.INPUT_DEVICE_VOSK,
            name = stringResource(R.string.pref_input_method_vosk),
            description = stringResource(R.string.pref_input_method_vosk_summary),
            icon = Icons.Default.Mic,
        ),
        ListSetting.Value(
            value = InputDevice.INPUT_DEVICE_EXTERNAL_POPUP,
            name = stringResource(R.string.pref_input_method_external_popup),
            description = stringResource(R.string.pref_input_method_external_popup_summary),
            icon = Icons.Default.PictureInPictureAlt,
        ),
        ListSetting.Value(
            value = InputDevice.INPUT_DEVICE_NOTHING,
            name = stringResource(R.string.pref_input_method_text),
            icon = Icons.Default.KeyboardAlt,
        ),
    ),
)

@Composable
fun wakeDevice() = ListSetting(
    title = stringResource(R.string.pref_wake_method),
    icon = Icons.Default.Hearing,
    description = stringResource(R.string.pref_wake_method_summary),
    possibleValues = listOf(
        ListSetting.Value(
            value = WakeDevice.WAKE_DEVICE_OWW,
            name = stringResource(R.string.pref_wake_method_openwakeword),
        ),
        ListSetting.Value(
            value = WakeDevice.WAKE_DEVICE_NOTHING,
            name = stringResource(R.string.pref_wake_method_disabled),
        )
    )
)

@Composable
fun speechOutputDevice() = ListSetting(
    title = stringResource(R.string.pref_speech_output_method),
    icon = Icons.Default.SpeakerPhone,
    description = stringResource(R.string.pref_speech_output_method_summary),
    possibleValues = listOf(
        ListSetting.Value(
            value = SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_ANDROID_TTS,
            name = stringResource(R.string.pref_speech_output_method_android),
            icon = Icons.Default.SpeakerPhone,
        ),
        ListSetting.Value(
            value = SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_TOAST,
            name = stringResource(R.string.pref_speech_output_method_toast),
            icon = Icons.Default.BreakfastDining,
        ),
        ListSetting.Value(
            value = SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_SNACKBAR,
            name = stringResource(R.string.pref_speech_output_method_snackbar),
            icon = Icons.Default.Minimize,
        ),
        ListSetting.Value(
            value = SpeechOutputDevice.SPEECH_OUTPUT_DEVICE_NOTHING,
            name = stringResource(R.string.pref_speech_output_method_nothing),
        ),
    ),
)

@Composable
fun sttSilenceDuration() = IntSetting(
    title = stringResource(R.string.pref_stt_silence_duration_title),
    icon = Icons.Default.HourglassEmpty,
    description = @Composable { stringResource(R.string.pref_stt_silence_duration_description, it) },
    minimum = 1,
    maximum = 7,
)

@Composable
fun sttAutoFinish() = BooleanSetting(
    title = stringResource(R.string.pref_stt_auto_finish_title),
    icon = Icons.AutoMirrored.Filled.Send,
    descriptionOff = stringResource(R.string.pref_stt_auto_finish_summary_off),
    descriptionOn = stringResource(R.string.pref_stt_auto_finish_summary_on),
)

@Composable
fun sttPlaySound() = ListSetting(
    title = stringResource(R.string.pref_stt_play_sound_title),
    icon = Icons.Default.Campaign,
    description = stringResource(R.string.pref_stt_play_sound_summary),
    possibleValues = listOf(
        ListSetting.Value(
            value = SttPlaySound.STT_PLAY_SOUND_NOTIFICATION,
            name = stringResource(R.string.pref_stt_play_sound_notification),
            icon = Icons.Default.Notifications,
        ),
        ListSetting.Value(
            value = SttPlaySound.STT_PLAY_SOUND_ALARM,
            name = stringResource(R.string.pref_stt_play_sound_alarm),
            icon = Icons.Default.Alarm,
        ),
        ListSetting.Value(
            value = SttPlaySound.STT_PLAY_SOUND_MEDIA,
            name = stringResource(R.string.pref_stt_play_sound_media),
            icon = Icons.Default.MusicNote,
        ),
        ListSetting.Value(
            value = SttPlaySound.STT_PLAY_SOUND_NONE,
            name = stringResource(R.string.pref_stt_play_sound_none),
        ),
    ),
)
