package org.stypox.dicio.skills.homeassistant

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsHomeAssistantSerializer : Serializer<SkillSettingsHomeAssistant> {
    override val defaultValue: SkillSettingsHomeAssistant = SkillSettingsHomeAssistant.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsHomeAssistant {
        try {
            return SkillSettingsHomeAssistant.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsHomeAssistant, output: OutputStream) {
        t.writeTo(output)
    }
}
