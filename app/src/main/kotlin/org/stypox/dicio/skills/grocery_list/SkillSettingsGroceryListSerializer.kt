package org.stypox.dicio.skills.grocery_list

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SkillSettingsGroceryListSerializer : Serializer<SkillSettingsGroceryList> {
    override val defaultValue: SkillSettingsGroceryList = SkillSettingsGroceryList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SkillSettingsGroceryList {
        try {
            return SkillSettingsGroceryList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", exception)
        }
    }

    override suspend fun writeTo(t: SkillSettingsGroceryList, output: OutputStream) {
        t.writeTo(output)
    }
}