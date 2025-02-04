package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.BaseActorAdapter
import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.persistence.Store
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler
import java.lang.reflect.Constructor

/**
 * Central entry point for creating and managing actors in the Synapsys framework.
 * Provides factory methods for actor system initialization, actor creation, and dependency configuration.
 *
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>System-wide configuration management</li>
 *   <li>Actor lifecycle management</li>
 *   <li>Dependency injection for persistence and messaging</li>
 *   <li>Supervision hierarchy setup</li>
 * </ul>
 *
 * @see Actor
 * @see Supervisor
 * @see Mailbox
 * @see Store
 */
object ActorSystem {
    private val executors: MutableMap<String, ActorExecutor<*>> = mutableMapOf()
    private lateinit var scheduler: Scheduler
    private lateinit var config: Config

    /**
     * Initializes the actor system with default configuration.
     * @throws IllegalStateException if called multiple times
     */
    fun create() {
        config = Config()
        scheduler = Scheduler(config.maxReductions)
    }

    /**
     * Initializes the actor system with custom configuration.
     * @param config Custom configuration parameters
     * @throws IllegalStateException if called multiple times
     */
    fun create(config: Config) {
        this.config = config
        scheduler = Scheduler(config.maxReductions)
    }

    /**
     * Creates and registers a new actor instance.
     *
     * @param id Unique identifier for the actor
     * @param initialState Initial state value for the actor
     * @param supervisor Optional supervision strategy (defaults to root supervisor)
     * @param actorFactory Factory function for actor creation
     * @return ActorPointer for message sending
     * @throws IllegalStateException if system not initialized
     *
     * @sample
     * val system = ActorSystem.create()
     * val counterRef = system.actorOf("counter", 0) { id, state ->
     *     object : Actor<Int, Command, Result>(id, state) {
     *         override fun onReceive(msg: Command, ctx: Context<Int>) = ...
     *     }
     * }
     */
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

        val executor = createActorExecutor(
            id = id,
            initialState = initialState,
            config = config,
            supervisor = supervisor,
            actorFactory = actorFactory)

        supervisor?.addChild<S, M, R>(executor, actorFactory)
        supervisor?.setConfig(config)
        supervisor?.setScheduler(scheduler)

        if (executors.putIfAbsent(id, executor) == null) {
            scheduler.enqueue(executor)
        }

        return ActorPointer(id, executor)
    }

    /**
     * @internal
     * Factory method for actor execution components
     */
    internal fun <S : Any, M : Any, R : Any> createActorExecutor(
        id: String,
        initialState: S,
        config: Config,
        supervisor: Supervisor?,
        actorFactory: (String, S) -> Actor<S, M, R>,
        mailbox: Mailbox<out M> = createMailbox(config)
        ): ActorExecutor<out M> {
        val actor = actorFactory(id, initialState)
        actor.store = createStore(config.storeClass)

        val adapter = BaseActorAdapter(actor)

        return ActorExecutor(adapter, mailbox, supervisor?.getMessageChannel())
    }

    /**
     * @internal
     * Creates a configured mailbox instance using reflection
     */
    private fun <M : Any> createMailbox(config: Config): Mailbox<M> {
        val instance = createMailboxInstance<MailboxAbstractQueue<M>>(
            config.mailboxClass,
            config.serializer
        )
        return Mailbox(queue = instance as MailboxAbstractQueue<M>)
    }

    /**
     * @internal
     * Reflection-based instantiation of mailbox implementations
     */
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

    /**
     * @internal
     * Creates a persistence store instance using reflection
     */
    private fun <S : Any> createStore(storeClass: String): Store<S> {
        return createStoreInstance(storeClass, Store::class.java) as Store<S>
    }

    /**
     * @internal
     * Reflection-based instantiation of store implementations
     */
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
