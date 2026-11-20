package org.stypox.dicio.skills.timer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsTimerSerializer : Serializer<SkillSettingsTimer> {
    override val defaultValue: SkillSettingsTimer = SkillSettingsTimer.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsTimer {
        try {
            return SkillSettingsTimer.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsTimer, output: OutputStream) {
        t.writeTo(output)
    }
}
