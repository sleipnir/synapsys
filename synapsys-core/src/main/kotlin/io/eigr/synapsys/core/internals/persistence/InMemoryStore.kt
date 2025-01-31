package io.eigr.synapsys.core.internals.persistence

class InMemoryStore<S> : Store<S> {
    private val store = mutableMapOf<String, S>()

    override suspend fun save(id: String, state: S) {
        store[id] = state
    }

    override suspend fun load(id: String): S? {
        return store[id]
    }
}