package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.BaseActorAdapter
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.transport.ChannelMailbox
import io.eigr.synapsys.core.internals.persistence.InMemoryStore
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler
import io.eigr.synapsys.core.internals.serialization.ProtobufMessageSerializer

object ActorSystem {
    private val executors: MutableMap<String, ActorExecutor<*>> = mutableMapOf()
    private lateinit var scheduler: Scheduler

    fun <S, M : Any, R> createActor(
        id: String,
        initialState: S,
        actorFactory: (String, S) -> Actor<S, M, R>
    ): ActorPointer<M> {
        val actor = actorFactory(id, initialState)
        actor.store = InMemoryStore()
        val adapter = BaseActorAdapter(actor)
        val mailbox = Mailbox<M>(ChannelMailbox(ProtobufMessageSerializer()))
        val executor = ActorExecutor(adapter, mailbox)

        executors[id] = executor
        return ActorPointer(id, executor)
    }

    fun start() {
        scheduler = Scheduler(50)
        executors.values.forEach { executor ->
            scheduler.enqueue(executor as ActorExecutor<Any>)
        }
    }
}
