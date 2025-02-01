package io.eigr.synapsys.core.internals.persistence

interface Store<S : Any> {
    suspend fun save(id: String, state: S)
    suspend fun load(id: String): S?
}