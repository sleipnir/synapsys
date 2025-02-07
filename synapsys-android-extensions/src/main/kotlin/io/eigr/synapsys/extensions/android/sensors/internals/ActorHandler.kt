package io.eigr.synapsys.extensions.android.sensors.internals

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.Context
import io.eigr.synapsys.extensions.android.sensors.actor.SensorActor
import io.eigr.synapsys.extensions.android.sensors.events.SensorData

class ActorHandler<S : Any, M : SensorData>(id: String?, initialState: S) : Actor<S, M, Unit>(id, initialState) {

    private lateinit var parentActor: SensorActor<S,M>

    internal fun setParentActor(parent: SensorActor<S,M>) {
        this.parentActor = parent
    }

    override fun onReceive(message: M, ctx: Context<S>): Pair<Context<S>, Unit> {
        return parentActor.onReceive(message, ctx)
    }
}