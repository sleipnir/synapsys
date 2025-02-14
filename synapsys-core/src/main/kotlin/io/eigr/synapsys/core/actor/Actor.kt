package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.store.Store
import io.eigr.synapsys.core.internals.store.backends.InMemoryStore

/**
 * Abstract base class representing an Actor in the Synapsys framework.
 * Actors are conceptual entities that process messages asynchronously while maintaining internal state.
 *
 * @param S Type of the actor's state. Must be a non-nullable type.
 * @param M Type of messages this actor can receive.
 * @param R Type of results produced by message processing.
 * @property id Optional unique identifier for this actor instance. Typically required for state persistence.
 * @property state Current [Context] containing the actor's state. Managed internally but accessible for inspection.
 *
 * @constructor Creates an Actor instance with optional initial state.
 * @param id Optional unique identifier for the actor. Required if state persistence is used.
 * @param initialState Initial state of the actor. Can be null for stateless actors or state initialization via persistence.
 * @param store Optional persistence mechanism for the actor's state. Defaults to [InMemoryStore].
 *
 * @see Context
 * @see Store
 */
abstract class Actor<S : Any, M : Any, R>(val id: String?, var initialState: S?, var store: Store<S>? = InMemoryStore()) {
    private val log = loggerFor(Actor::class.java)
    lateinit var system: ActorSystem

    /**
     * Current operational context containing the actor's state.
     * Initialized with either the provided initial state or loaded from persistence.
     */
    internal var state: Context<S>? = null

    /**
     * stores java class of state
     */
    private val stateClass: Class<S>? = initialState?.javaClass

    /**
     * Lifecycle hook called when actor is about to start.
     */
    open fun onStart(ctx: Context<S>): Context<S> {
        return ctx
    }

    /**
     * Lifecycle hook called when actor is about to stop.
     */
    open fun onStop() {}

    /**
     * Core message handling method that must be implemented by concrete actors.
     *
     * @param message Incoming message to process
     * @param ctx Current [Context] containing actor state when message was received
     * @return Pair containing updated [Context] and processing result
     * @throws Exception Implementations should declare specific exceptions they might throw
     */
    abstract fun onReceive(message: M, ctx: Context<S>): Pair<Context<S>, R>

    /**
     * Lifecycle hook called when actor starts.
     * Loads persisted state if available and id is provided.
     *
     * @throws IllegalStateException If id is null when attempting to load state
     */
    internal suspend fun rehydrate()  {
        log.info("[{}] Rehydrating actor state", id)
        val oldState = store?.load(id!!, stateClass!!)
        if (oldState != null) {
            this.state = Context(oldState, system)
            this.state
        } else {
            this.state = Context(this.initialState, system)
            this.state
        }

        log.info("[{}] Rehydrated actor state: {}", id, state!!.state)
    }

    /**
     * Updates actor state and persists it atomically.
     *
     * @param state New state value
     * @return The newly persisted state
     * @throws IllegalStateException If id is null when attempting to save state
     */
    internal suspend fun mutate(state: S): S {
        this.state = Context(state, system)
        store?.save(id!!, state)
        return state
    }
}