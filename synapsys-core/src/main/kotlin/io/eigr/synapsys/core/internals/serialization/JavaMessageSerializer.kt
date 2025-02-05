package io.eigr.synapsys.core.internals.serialization

import io.eigr.synapsys.core.internals.MessageSerializer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class JavaMessageSerializer : MessageSerializer {

    override fun <T> serialize(obj: T): ByteArray {
        val bos = ByteArrayOutputStream()
        ObjectOutputStream(bos).use { it.writeObject(obj) }
        return bos.toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(bytes: ByteArray, clazz: Class<T>): T {
        val bis = ByteArrayInputStream(bytes)
        ObjectInputStream(bis).use { return it.readObject() as T }
    }
}