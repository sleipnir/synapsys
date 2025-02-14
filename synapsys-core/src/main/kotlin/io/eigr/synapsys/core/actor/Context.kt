package io.eigr.synapsys.core.actor

/**
 * Immutable container class for managing an actor's state within the Synapsys framework.
 * Provides controlled access to state while enabling functional-style updates through copying.
 *
 * @param S Type of the contained state. Must be a non-nullable type.
 * @property state Current read-only view of the actor's state. May be null for uninitialized state.
 *
 * @constructor Creates a Context instance with optional initial state.
 * @param internalState Initial state value. Can be null for empty initialization.
 *
 * @see Actor
 */
class Context<S : Any>(private var internalState: S?, private val actorSystem : ActorSystem) {

    /**
     * Read-only access to the current state value.
     * Returns null if state hasn't been initialized.
     */
    val state get() = internalState



    val system get() = actorSystem

    /**
     * Creates a new Context instance with updated state while maintaining immutability.
     * Original instance remains unchanged.
     *
     * @param newState State value for the new Context instance
     * @return New Context instance containing the updated state
     *
     * @sample
     * val original = Context("old")
     * val updated = original.withState("new")
     * // original.state remains "old"
     * // updated.state is now "new"
     */
    fun withState(newState: S): Context<S> {
        this.internalState = newState
        return this
    }
}