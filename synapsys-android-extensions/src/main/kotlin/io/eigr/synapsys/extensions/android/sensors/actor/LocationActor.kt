package io.eigr.synapsys.extensions.android.sensors.actor

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.extensions.android.sensors.events.SensorData
import io.eigr.synapsys.extensions.android.sensors.internals.ActorHandler
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import android.content.Context as AndroidContext
import io.eigr.synapsys.core.actor.Context as ActorContext

/**
 * Represents an actor that listens for location updates and forwards them as sensor data messages.
 *
 * @param S The type of state managed by the actor.
 * @param M The type of sensor data messages handled by the actor.
 * @param id Unique identifier for the actor.
 * @param initialState Initial state of the actor.
 * @param androidContext Android application context for accessing system services.
 * @param provider The location provider (e.g., GPS, Network).
 * @param minTimeMs Minimum time interval (in milliseconds) for location updates.
 * @param minDistanceM Minimum distance (in meters) for location updates.
 */
open class LocationActor<S : Any, M : SensorData>(
    id: String,
    initialState: S?,
    private val androidContext: AndroidContext,
    private val provider: String = LocationManager.GPS_PROVIDER,
    private val minTimeMs: Long = 5000,
    private val minDistanceM: Float = 10f
) : Actor<S, M, Unit>(
    id = "location-actor-${id}-$provider",
    initialState = initialState
),
    LocationListener {

    private val log = LoggerFactory.getLogger(LocationActor::class.java)

    /** Lazy initialization of the location manager. */
    private val locationManager by lazy {
        androidContext.getSystemService(AndroidContext.LOCATION_SERVICE) as LocationManager
    }

    /** Pointer to the target actor that processes location data. */
    private var targetActor: ActorPointer<*>? = null

    /**
     * Starts the actor and registers for location updates.
     *
     * @param ctx The actor's execution context.
     * @return Updated actor context.
     */
    @SuppressLint("MissingPermission")
    override fun onStart(ctx: ActorContext<S>): ActorContext<S> {
        try {
            log.info("Registering sensor {}", provider)
            targetActor = ctx.system.actorOf(
                id = "processor-$id",
                initialState = initialState!!
            ) { id, state ->
                ActorHandler<S, M>(id, state).apply {
                    parentActor = this@LocationActor
                }
            }

            locationManager.requestLocationUpdates(
                provider,
                minTimeMs,
                minDistanceM,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            log.error("Location permission not granted")
        }

        return ctx
    }

    /**
     * Stops the actor and unregisters location updates.
     */
    @SuppressLint("MissingPermission")
    override fun onStop() {
        locationManager.removeUpdates(this)
    }

    /**
     * Handles received messages.
     *
     * @param message The received sensor data message.
     * @param ctx The actor's execution context.
     * @return A pair containing the updated actor context and unit.
     */
    override fun onReceive(message: M, ctx: ActorContext<S>): Pair<ActorContext<S>, Unit> {
        return this.onReceive(message, ctx)
    }

    /**
     * Called when a new location update is received.
     *
     * @param event The location update event.
     */
    @Suppress("UNCHECKED_CAST")
    override fun onLocationChanged(event: Location) {
        val message = buildMessage(event)
        runBlocking {
            targetActor?.send(message as M)
        }
    }

    /**
     * Converts a location event into a sensor data message.
     *
     * @param event The location event.
     * @return The corresponding sensor data message.
     */
    private fun buildMessage(event: Location): SensorData = SensorData.LocationData(
        latitude = event.latitude,
        longitude = event.longitude,
        altitude = event.altitude,
        accuracy = event.accuracy,
        speed = event.speed,
        bearing = event.bearing,
        timestamp = event.time,
    )
}

