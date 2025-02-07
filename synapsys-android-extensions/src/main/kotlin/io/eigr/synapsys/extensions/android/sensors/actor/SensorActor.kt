package io.eigr.synapsys.extensions.android.sensors.actor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.extensions.android.sensors.events.SensorData
import io.eigr.synapsys.extensions.android.sensors.internals.ActorHandler
import kotlinx.coroutines.runBlocking
import android.content.Context as AndroidContext
import io.eigr.synapsys.core.actor.Context as ActorContext

open class SensorActor<S : Any, M : SensorData>(
    id: String,
    initialState: S?,
    private val androidContext: AndroidContext,
    private val sensorType: Int,
    private val samplingPeriod: Int = SensorManager.SENSOR_DELAY_NORMAL,
) : Actor<S, M, Unit>(
    id = "sensor-actor-${id}-${sensorType}",
    initialState = initialState
),
    SensorEventListener {

    private val sensorManager by lazy {
        androidContext.getSystemService(AndroidContext.SENSOR_SERVICE) as SensorManager
    }

    private lateinit var targetActor: ActorPointer<*>

    override fun onStart(ctx: ActorContext<S>): ActorContext<S> {
        targetActor = ctx.system.actorOf(
            id = "processor-$id",
            initialState = initialState!!) {id, state -> ActorHandler(id, state) }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        sensor?.let {
            sensorManager.registerListener(this, it, samplingPeriod)
        } ?: {
            //ctx.system.log.error("Sensor $sensorType not available")
        }

        return ctx
    }

    override fun onStop() {
        sensorManager.unregisterListener(this)
    }

    override fun onReceive(message: M, ctx: ActorContext<S>): Pair<ActorContext<S>, Unit> {
        return ctx to Unit
    }

    @Suppress("UNCHECKED_CAST")
    override fun onSensorChanged(event: SensorEvent) {
        val message = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> SensorData.AccelerometerData(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestamp = event.timestamp
            )

            Sensor.TYPE_GYROSCOPE -> SensorData.GyroscopeData(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestamp = event.timestamp
            )

            Sensor.TYPE_HEART_RATE -> SensorData.HeartRateData(
                bpm = event.values[0],
                timestamp = event.timestamp
            )

            else -> SensorData.RawSensorData(
                sensorType = event.sensor.type,
                values = event.values.copyOf(),
                timestamp = event.timestamp
            )
        }

        runBlocking { targetActor.send(message as M) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
