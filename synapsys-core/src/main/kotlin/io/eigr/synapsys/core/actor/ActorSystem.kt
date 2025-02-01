package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.BaseActorAdapter
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.persistence.Store
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler

object ActorSystem {
    private val executors: MutableMap<String, ActorExecutor<*>> = mutableMapOf()
    private lateinit var scheduler: Scheduler
    private lateinit var config: Config

    fun create() {
        config = Config()
        scheduler = Scheduler(config.maxReductions)
    }

    fun create(config: Config) {
        this.config = config
        scheduler = Scheduler(config.maxReductions)
    }

    fun <S : Any, M : Any, R> actorOf(
        id: String,
        initialState: S,
        actorFactory: (String, S) -> Actor<S, M, R>
    ): ActorPointer<M> {
        if (this.scheduler == null) {
            throw IllegalStateException("Make sure you create ActorSystem first")
        }

        val actor = actorFactory(id, initialState)
        actor.store = config.store as Store<S>
        val adapter = BaseActorAdapter(actor)
        val mailbox = Mailbox(config.mailbox as MailboxAbstractQueue<M>)

        val executor = ActorExecutor(adapter, mailbox)

        if (!executors.contains(id)) {
            executors[id] = executor
            scheduler.enqueue(executor)
        }

        return ActorPointer(id, executor)
    }
}
