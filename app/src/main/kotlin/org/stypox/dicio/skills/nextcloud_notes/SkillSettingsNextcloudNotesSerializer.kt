package org.stypox.dicio.skills.nextcloud_notes

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsNextcloudNotesSerializer : Serializer<SkillSettingsNextcloudNotes> {
    override val defaultValue: SkillSettingsNextcloudNotes = SkillSettingsNextcloudNotes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsNextcloudNotes {
        try {
            return SkillSettingsNextcloudNotes.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsNextcloudNotes, output: OutputStream) {
        t.writeTo(output)
    }
}
