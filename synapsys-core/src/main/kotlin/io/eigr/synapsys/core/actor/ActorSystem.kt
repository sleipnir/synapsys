package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.BaseActorAdapter
import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.persistence.Store
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler
import java.lang.reflect.Constructor

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

    fun <S : Any, M : Any, R : Any> actorOf(
        id: String,
        initialState: S,
        supervisor: Supervisor? = Supervisor(
            id = "root-supervisor",
            strategy = SupervisorStrategy(RestartStrategy.OneForOne, 5)
        ),
        actorFactory: (String, S) -> Actor<S, M, R>
    ): ActorPointer<M> {
        if (!this::scheduler.isInitialized) {
            throw IllegalStateException("Make sure you create ActorSystem first")
        }

        val executor = createActorExecutor(id, initialState, config, supervisor, actorFactory)

        supervisor?.addChild<S, M, R>(executor, actorFactory)
        supervisor?.setConfig(config)
        supervisor?.setScheduler(scheduler)

        if (executors.putIfAbsent(id, executor) == null) {
            scheduler.enqueue(executor)
        }

        return ActorPointer(id, executor)
    }

    internal fun <S : Any, M : Any, R : Any> createActorExecutor(
        id: String,
        initialState: S,
        config: Config,
        supervisor: Supervisor?,
        actorFactory: (String, S) -> Actor<S, M, R>
    ): ActorExecutor<M> {
        val actor = actorFactory(id, initialState)
        actor.store = createStore(config.storeClass)

        val adapter = BaseActorAdapter(actor)
        val mailbox = createMailbox<M>(config)

        return ActorExecutor(adapter, mailbox, supervisor?.getMessageChannel())
    }

    private fun <M : Any> createMailbox(config: Config): Mailbox<M> {
        val instance = createMailboxInstance<MailboxAbstractQueue<M>>(
            config.mailboxClass,
            config.serializer
        )
        return Mailbox(queue = instance as MailboxAbstractQueue<M>)
    }

    private fun <T> createMailboxInstance(className: String, serializer: MessageSerializer): T? {
        return try {
            val clazz = Class.forName(className)
            val constructor: Constructor<*> = clazz.getConstructor(MessageSerializer::class.java)
            val instance = constructor.newInstance(serializer)
            if (MailboxAbstractQueue::class.java.isInstance(instance)) {
                @Suppress("UNCHECKED_CAST")
                instance as T
            } else {
                throw IllegalArgumentException("Class $className does not implement MailboxAbstractQueue")
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Error loading MailboxAbstractQueue implementation ($className)",
                e
            )
        }
    }

    private fun <S : Any> createStore(storeClass: String): Store<S> {
        return createStoreInstance(storeClass, Store::class.java) as Store<S>
    }

    private fun <S> createStoreInstance(className: String, superType: Class<S>): S? {
        return try {
            val clazz = Class.forName(className)
            val instance = clazz.getDeclaredConstructor().newInstance()
            if (superType.isInstance(instance)) {
                @Suppress("UNCHECKED_CAST")
                instance as S
            } else {
                throw IllegalArgumentException("Class $className does not implement ${superType.name}")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error loading implementation of $className", e)
        }
    }
}
