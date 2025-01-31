package io.creativesource.synapsys.core.actor

import io.creativesource.synapsys.core.internals.ActorAdapter
import io.creativesource.synapsys.core.internals.BaseActor
import io.creativesource.synapsys.core.internals.mailbox.Mailbox
import io.creativesource.synapsys.core.internals.mailbox.transport.ChannelMailbox
import io.creativesource.synapsys.core.internals.persistence.InMemoryStore
import io.creativesource.synapsys.core.internals.scheduler.ActorExecutor
import io.creativesource.synapsys.core.internals.scheduler.Scheduler
import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer

object ActorSystem {
    private val executors: MutableMap<String, ActorExecutor<*>> = mutableMapOf()
    private lateinit var scheduler: Scheduler

    fun <S, M : Any, R> createActor(
        id: String,
        initialState: S,
        actorFactory: (String, S) -> Actor<S, M, R>
    ): BaseActor {
        val actor = actorFactory(id, initialState)
        actor.store = InMemoryStore()
        val adapter = ActorAdapter(actor)
        val mailbox = Mailbox<M>(ChannelMailbox(ProtobufMessageSerializer()))
        val executor = ActorExecutor(adapter, mailbox)

        executors[id] = executor
        return adapter
    }

    fun start() {
        scheduler = Scheduler(10)
        executors.values.forEach { executor ->
            scheduler.enqueue(executor as ActorExecutor<Any>)
        }
    }

    suspend fun sendMessage(actorId: String, message: Any) {
        println("[ActorSystem] Sending message: $message to actor: $actorId")
        (executors[actorId] as? ActorExecutor<Any>)?.send(message)
    }
}
