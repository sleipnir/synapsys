package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.mailbox.transport.ChannelMailbox
import io.eigr.synapsys.core.internals.persistence.InMemoryStore
import io.eigr.synapsys.core.internals.persistence.Store
import io.eigr.synapsys.core.internals.serialization.ProtobufMessageSerializer

data class Config(
    val maxReductions: Int = 50,
    val store: Store<Any> = InMemoryStore(),
    val serializer: MessageSerializer = ProtobufMessageSerializer(),
    val mailbox: MailboxAbstractQueue<Any> = ChannelMailbox(serializer)
)