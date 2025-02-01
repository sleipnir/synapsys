package io.eigr.synapsys.core.internals.mailbox

data class AskMessage<M : Any>(
    val id: String,
    val message: M
)
