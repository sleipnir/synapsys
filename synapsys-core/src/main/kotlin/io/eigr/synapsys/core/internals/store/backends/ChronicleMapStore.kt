package io.eigr.synapsys.core.internals.store.backends

import io.eigr.synapsys.core.internals.store.Store
import net.openhft.chronicle.map.ChronicleMap
import java.io.File

@Suppress("UNCHECKED_CAST")
class ChronicleMapStore<S : Any>(
    private val valueClass: Class<S>,
    maxEntries: Long = 10_000,
    averageKeySize: Double = 50.00,
    averageValueSize: Double = 1024.00
) : Store<S> {
    private val chronicleMap: ChronicleMap<String, S> = ChronicleMap
        .of(String::class.java, valueClass)
        .name("actor-store")
        .entries(maxEntries)
        .averageKeySize(averageKeySize)
        .averageValueSize(averageValueSize)
        .createPersistedTo(File("chronicle-data"))

    override suspend fun save(id: String, state: S) {
        chronicleMap[id] = state
    }

    override suspend fun <S> load(id: String, clazz: Class<S>): S? {
        return chronicleMap[id] as S
    }

    fun close() {
        chronicleMap.close()
    }
}