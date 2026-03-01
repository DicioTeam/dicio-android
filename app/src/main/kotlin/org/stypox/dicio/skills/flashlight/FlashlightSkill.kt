package org.stypox.dicio.skills.flashlight

import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Flashlight

@RequiresApi(Build.VERSION_CODES.M)
class FlashlightSkill(
    correspondingSkillInfo: FlashlightInfo,
    data: StandardRecognizerData<Flashlight>
) : StandardRecognizerSkill<Flashlight>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Flashlight): SkillOutput {
        val cameraManager = ctx.android.getSystemService<CameraManager>()
        val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            ?: return FlashlightOutput.Error(null)

        try {
            val turnedOn = when (inputData) {
                Flashlight.TurnOff -> false
                Flashlight.TurnOn -> true
            }
            cameraManager.setTorchMode(cameraId, turnedOn)
            return FlashlightOutput.Success(turnedOn = turnedOn)
        } catch (throwable: Throwable) {
            return FlashlightOutput.Error(throwable)
        }
    }
}
