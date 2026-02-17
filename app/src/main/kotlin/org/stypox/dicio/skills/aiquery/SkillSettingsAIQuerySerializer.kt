package org.stypox.dicio.skills.aiquery

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsAIQuerySerializer : Serializer<SkillSettingsAIQuery> {
    override val defaultValue: SkillSettingsAIQuery = SkillSettingsAIQuery.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsAIQuery {
        try {
            return SkillSettingsAIQuery.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsAIQuery, output: OutputStream) {
        t.writeTo(output)
    }
}