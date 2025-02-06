package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.serialization.ProtobufMessageSerializer

data class Config(
    val maxReductions: Int = 50,
    val storeClass: String = "io.eigr.synapsys.core.internals.store.backends.InMemoryStore",
    val mailboxClass: String = "io.eigr.synapsys.core.internals.mailbox.transport.ChannelMailbox",
    val serializer: MessageSerializer = ProtobufMessageSerializer()
)
