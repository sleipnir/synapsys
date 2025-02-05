package io.eigr.synapsys.core.internals.store

interface Store<S : Any> {
    suspend fun save(id: String, state: S)
    suspend fun <S> load(id: String, clazz: Class<S>): S?
}