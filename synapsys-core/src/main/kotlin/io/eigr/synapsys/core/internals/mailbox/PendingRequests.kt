package io.eigr.synapsys.core.internals.mailbox

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

object PendingRequests {
    private val pendingResponses = ConcurrentHashMap<String, CompletableDeferred<Any>>()

    fun <R> createRequest(id: String): CompletableDeferred<R> {
        val deferred = CompletableDeferred<R>()
        pendingResponses[id] = deferred as CompletableDeferred<Any>
        return deferred
    }

    fun completeRequest(id: String, result: Any?) {
        pendingResponses.remove(id)?.complete(result!!)
    }

    fun failRequest(id: String, error: Throwable) {
        pendingResponses.remove(id)?.completeExceptionally(error)
    }
}
