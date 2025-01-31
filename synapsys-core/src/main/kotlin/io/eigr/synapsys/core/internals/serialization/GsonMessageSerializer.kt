package io.eigr.synapsys.core.internals.serialization

import com.google.gson.Gson
import io.eigr.synapsys.core.internals.MessageSerializer
import java.nio.charset.StandardCharsets

class GsonMessageSerializer : MessageSerializer {
    private val gson = Gson()
    private val charset = StandardCharsets.UTF_8

    override fun <T> serialize(obj: T): ByteArray {
        return gson.toJson(obj).toByteArray(charset)
    }

    override fun <T> deserialize(bytes: ByteArray, clazz: Class<T>): T {
        val jsonString = String(bytes, charset)
        return gson.fromJson(jsonString, clazz)
    }
}