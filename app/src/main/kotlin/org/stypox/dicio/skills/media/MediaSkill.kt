package org.stypox.dicio.skills.media

import android.media.AudioManager
import android.view.KeyEvent
import androidx.core.content.ContextCompat.getSystemService
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Media

class MediaSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Media>)
    : StandardRecognizerSkill<Media>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Media): SkillOutput {
        val audioManager = getSystemService(ctx.android, AudioManager::class.java)
            ?: return MediaOutput(performedAction = null) // no media session found

        when (inputData) {
            is Media.Play -> {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            }
            is Media.Pause -> {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            }
            is Media.Previous -> {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            }
            is Media.Next -> {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            }
            is Media.VolumeUp -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            is Media.VolumeDown -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            is Media.VolumeUpTimes -> {
                val times = extractNumberFromString(ctx, inputData.times)
                repeat(times) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        if (it == 0) AudioManager.FLAG_SHOW_UI else 0
                    )
                }
            }
            is Media.VolumeDownTimes -> {
                val times = extractNumberFromString(ctx, inputData.times)
                repeat(times) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        if (it == 0) AudioManager.FLAG_SHOW_UI else 0
                    )
                }
            }
        }

        return MediaOutput(performedAction = inputData)
    }

    private fun extractNumberFromString(ctx: SkillContext, input: String?): Int {
        if (input.isNullOrBlank()) {
            return 1
        }

        return ctx.parserFormatter!!
            .extractNumber(input)
            .mixedWithText
            .asSequence()
            .filterIsInstance<org.dicio.numbers.unit.Number>()
            .filter { it.isInteger }
            .map { it.integerValue().toInt() }
            .firstOrNull()
            ?.coerceIn(1, 10) // Limit to 1-10 steps for safety
            ?: 1
    }

    companion object {
        val TAG: String = MediaSkill::class.simpleName!!
    }
}
