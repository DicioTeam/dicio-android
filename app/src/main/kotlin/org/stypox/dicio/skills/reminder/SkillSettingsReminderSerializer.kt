package org.stypox.dicio.skills.reminder

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsReminderSerializer : Serializer<SkillSettingsReminder> {
    override val defaultValue: SkillSettingsReminder = SkillSettingsReminder.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsReminder {
        try {
            return SkillSettingsReminder.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsReminder, output: OutputStream) {
        t.writeTo(output)
    }
}
