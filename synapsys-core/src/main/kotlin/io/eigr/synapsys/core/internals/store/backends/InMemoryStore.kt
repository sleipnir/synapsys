package io.eigr.synapsys.core.internals.store.backends

import io.eigr.synapsys.core.internals.store.Store
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class InMemoryStore<S : Any> : Store<S> {
    private val store = ConcurrentHashMap<String, S>()

    override suspend fun save(id: String, state: S) {
        store[id] = state
    }

    override suspend fun <S> load(id: String, clazz: Class<S>): S? {
        return store[id] as S?
    }
}