package org.stypox.dicio.skills.music

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Music

class MusicSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Music>) :
    StandardRecognizerSkill<Music>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Music): SkillOutput {
        val (song, artist) = when (inputData) {
            is Music.Query -> Pair(inputData.song, inputData.artist)
        }

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, song)
            putExtra(SearchManager.QUERY, song)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // also search for the artist if given
        if (artist != null)
            intent.apply {
                putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
                putExtra(SearchManager.QUERY, "$song $artist")
            }

        val packageManager: PackageManager = ctx.android.packageManager
        val componentName = intent.resolveActivity(packageManager)
        if (componentName == null) {
            return MusicOutput(appName = null, packageName = null)
        }
        ctx.android.startActivity(intent)

        val applicationInfo = packageManager.getApplicationInfo(componentName.packageName, 0)
        return MusicOutput(
            appName = applicationInfo.loadLabel(packageManager).toString(),
            packageName = applicationInfo.packageName,
        )
    }
}
