package io.eigr.synapsys.extensions.store.backends.room

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.store.AbstractStore

class RoomStore<S : Any>(
    serializer: MessageSerializer,
    database: ActorStateDatabase
) : AbstractStore<S>(serializer) {

    private val dao = database.dao()

    override suspend fun persistData(id: String, bytes: ByteArray) {
        dao.save(ActorStateEntity(id, bytes))
    }

    override suspend fun retrieveData(id: String): ByteArray? {
        return dao.load(id)?.data
    }
}