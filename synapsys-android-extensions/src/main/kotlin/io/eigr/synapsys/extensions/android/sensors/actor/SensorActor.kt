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
import org.slf4j.LoggerFactory
import android.content.Context as AndroidContext
import io.eigr.synapsys.core.actor.Context as ActorContext

/**
 * Actor that listens to Android sensor events and propagates data to another actor.
 *
 * @param S The type of the actor state.
 * @param M The type of sensor data messages.
 * @param id The unique identifier of the actor.
 * @param initialState The initial state of the actor.
 * @param androidContext The Android application context.
 * @param sensorType The type of sensor being monitored.
 * @param samplingPeriod The frequency of sensor updates.
 */
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

    private val log = LoggerFactory.getLogger(SensorActor::class.java)

    /** Lazy initialization of the SensorManager. */
    private val sensorManager by lazy {
        androidContext.getSystemService(AndroidContext.SENSOR_SERVICE) as SensorManager
    }

    /** Reference to the target actor that processes sensor data. */
    private var targetActor: ActorPointer<*>? = null

    /**
     * Starts the sensor actor by registering it with the sensor manager and creating a target processing actor.
     *
     * @param ctx The actor execution context.
     * @return The updated actor context.
     */
    override fun onStart(ctx: ActorContext<S>): ActorContext<S> {
        log.info("Registering sensor {}", sensorType)

        targetActor = ctx.system.actorOf(
            id = "processor-$id",
            initialState = initialState!!
        ) { id, state -> ActorHandler<S, M>(id, state).apply { parentActor = this@SensorActor } }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        sensor?.let {
            sensorManager.registerListener(this, it, samplingPeriod)
        } ?: log.warn("Sensor $sensorType not available")

        return ctx
    }

    /**
     * Stops the sensor actor by unregistering it from the sensor manager.
     */
    override fun onStop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Handles incoming messages (not used in this implementation).
     *
     * @param message The incoming message.
     * @param ctx The actor execution context.
     * @return A pair containing the updated context and a Unit value.
     */
    override fun onReceive(message: M, ctx: ActorContext<S>): Pair<ActorContext<S>, Unit> {
        return this.onReceive(message, ctx)
    }

    /**
     * Callback method triggered when a new sensor reading is available.
     * Converts the sensor event data into a SensorData object and sends it to the target actor.
     *
     * @param event The sensor event.
     */
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

        runBlocking {
            targetActor?.send(message as M)
        }
    }

    /**
     * Callback method triggered when the sensor accuracy changes. Not used in this implementation.
     *
     * @param sensor The sensor whose accuracy changed.
     * @param accuracy The new accuracy value.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
