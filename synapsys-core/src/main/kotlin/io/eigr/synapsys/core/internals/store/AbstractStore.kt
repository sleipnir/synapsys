package io.eigr.synapsys.core.internals.store

import io.eigr.synapsys.core.internals.MessageSerializer

abstract class AbstractStore<S : Any>(
    protected val serializer: MessageSerializer
) : Store<S> {

    protected abstract suspend fun persistData(id: String, bytes: ByteArray)
    protected abstract suspend fun retrieveData(id: String): ByteArray?

    override suspend fun save(id: String, state: S) {
        val bytes = serializer.serialize(state)
        persistData(id, bytes)
    }

    override suspend fun <S> load(id: String, clazz: Class<S>): S? {
        val bytes = retrieveData(id) ?: return null
        return serializer.deserialize(bytes, clazz)
    }
}