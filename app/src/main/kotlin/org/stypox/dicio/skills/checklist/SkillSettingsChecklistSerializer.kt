package org.stypox.dicio.skills.checklist

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsChecklistSerializer : Serializer<SkillSettingsChecklist> {
    override val defaultValue: SkillSettingsChecklist = SkillSettingsChecklist.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsChecklist {
        try {
            return SkillSettingsChecklist.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsChecklist, output: OutputStream) {
        t.writeTo(output)
    }
}
