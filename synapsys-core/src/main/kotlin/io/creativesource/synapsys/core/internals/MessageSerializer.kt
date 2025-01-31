package io.creativesource.synapsys.core.internals

interface MessageSerializer {
    fun <T> serialize(obj: T): ByteArray
    fun <T> deserialize(bytes: ByteArray, clazz: Class<T>): T
}