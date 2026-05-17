package org.stypox.dicio.skills.media

import org.dicio.skill.context.SkillContext
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.sentences.Sentences.Media
import org.stypox.dicio.util.getString

class MediaOutput(
    private val performedAction: Media?
) : HeadlineSpeechSkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = when (performedAction) {
        null -> ctx.getString(R.string.skill_media_no_media_session)
        is Media.Play -> ctx.getString(R.string.skill_media_playing)
        is Media.Pause -> ctx.getString(R.string.skill_media_pausing)
        is Media.Previous -> ctx.getString(R.string.skill_media_previous)
        is Media.Next -> ctx.getString(R.string.skill_media_next)
        is Media.VolumeUp -> ctx.getString(R.string.skill_media_volume_up)
        is Media.VolumeDown -> ctx.getString(R.string.skill_media_volume_down)
        is Media.VolumeUpTimes -> ctx.getString(R.string.skill_media_volume_up)
        is Media.VolumeDownTimes -> ctx.getString(R.string.skill_media_volume_down)
    }
}
