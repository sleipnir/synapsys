package io.eigr.synapsys.extensions.android.sensors

import io.eigr.synapsys.core.actor.ActorSystem
import io.eigr.synapsys.core.actor.Config
import io.eigr.synapsys.core.actor.Context
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler
import io.eigr.synapsys.core.internals.scheduler.WorkingStealingScheduler
import io.eigr.synapsys.extensions.android.sensors.actor.SensorActor
import io.eigr.synapsys.extensions.android.sensors.events.SensorData

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidScheduler(config: Config) : Scheduler {
    private val sensorScope = CoroutineScope(Dispatchers.IO)
    private val workingStealingScheduler = WorkingStealingScheduler(config.maxReductions)
    private val sensorActors = mutableMapOf<String, ActorExecutor<*>>()
    private lateinit var _system: ActorSystem

    override fun enqueue(actorExecutor: ActorExecutor<*>) {
        // Enqueue the actor executor on the sensor scope if instance of actor is SensorActor
        when (val actor = actorExecutor.actor.getActor<Any, Any, Any>()) {
            is SensorActor<*, *> -> {
                sensorActors[actorExecutor.actor.id] = actorExecutor
                handleSensorActor(actor)
            }
            else -> workingStealingScheduler.enqueue(actorExecutor)
        }
    }

    override fun removeActor(actorId: String): Boolean {
        if (sensorActors.containsKey(actorId)) {
            sensorActors.remove(actorId)
            return true
        }

        return workingStealingScheduler.removeActor(actorId)
    }

    override fun cleanAllWorkerQueues() {
        sensorActors.clear()
        workingStealingScheduler.cleanAllWorkerQueues()
    }

    override fun setSystem(actorSystem: ActorSystem) {
        this._system = actorSystem
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S : Any> handleSensorActor(sensorActor: SensorActor<S, *>) {
        val ctx = Context(
            internalState = sensorActor.initialState,
            actorSystem = _system
        )

        sensorScope.launch {
            (sensorActor as SensorActor<S, SensorData>).onStart(ctx)
        }
    }
}