package io.eigr.synapsys.core.internals.serialization

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.eigr.synapsys.core.internals.MessageSerializer
import io.protostuff.LinkedBuffer
import io.protostuff.ProtobufIOUtil
import io.protostuff.Schema
import io.protostuff.runtime.RuntimeSchema

class ProtobufMessageSerializer : MessageSerializer {
    private val buffer = LinkedBuffer.allocate(512)
    private val schemaCache = mutableMapOf<Class<*>, Schema<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> serialize(obj: T): ByteArray {
        val clazz = obj!!.javaClass
        val schema = getSchema(clazz) as Schema<T>

        val bytes = ProtobufIOUtil.toByteArray(obj, schema, buffer)
        buffer.clear()

        return Any.newBuilder()
            .setTypeUrl(typeUrlFor(clazz))
            .setValue(ByteString.copyFrom(bytes))
            .build()
            .toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(bytes: ByteArray, clazz: Class<T>): T {
        val any = Any.parseFrom(bytes)
        val targetClass = classForTypeUrl(any.typeUrl)

        if (!clazz.isAssignableFrom(targetClass)) {
            throw IllegalArgumentException("Type mismatch: ${targetClass.name} is not assignable to ${clazz.name}")
        }

        val schema = getSchema(targetClass) as Schema<T>
        val message = schema.newMessage()
        ProtobufIOUtil.mergeFrom(any.value.toByteArray(), message, schema)

        return message
    }

    private fun typeUrlFor(clazz: Class<*>): String {
        return "type.googleapis.com/${clazz.name}"
    }

    private fun classForTypeUrl(typeUrl: String): Class<*> {
        val className = typeUrl.substringAfterLast('/')
        return Class.forName(className)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getSchema(clazz: Class<T>): Schema<T> {
        return schemaCache.getOrPut(clazz) {
            RuntimeSchema.createFrom(clazz)
        } as Schema<T>
    }
}