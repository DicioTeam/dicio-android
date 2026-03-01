package org.stypox.dicio.error

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The user actions that can cause an error.
 * @implNote Taken with some modifications from NewPipe, file error/UserAction.java
 */
@Parcelize
enum class UserAction(val message: String) : Parcelable {
    UNKNOWN("Unknown error"),
    STT_POPUP_SPEECH_TO_TEXT("Stt popup speech to text"),
    GENERIC_EVALUATION("Evaluation"),
    SKILL_EVALUATION("Skill evaluation"),
    WAKE_DOWNLOADING("Downloading wake word model"),
    WAKE_LOADING("Loading wake word model"),
    FLASHLIGHT("Turning flashlight on or off");
}
