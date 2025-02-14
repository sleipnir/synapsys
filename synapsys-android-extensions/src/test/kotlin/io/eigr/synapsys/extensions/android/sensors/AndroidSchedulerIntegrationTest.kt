package io.eigr.synapsys.extensions.android.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorSystem
import io.eigr.synapsys.core.actor.Config
import io.eigr.synapsys.core.actor.RestartStrategy
import io.eigr.synapsys.core.actor.Supervisor
import io.eigr.synapsys.core.actor.SupervisorStrategy
import io.eigr.synapsys.core.internals.BaseActorAdapter
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.mailbox.transport.ChannelMailbox
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.extensions.android.sensors.actor.SensorActor
import io.eigr.synapsys.extensions.android.sensors.events.SensorData
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.slf4j.LoggerFactory
import io.eigr.synapsys.core.actor.Context as ActorContext
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(manifest = RoboConfig.NONE)
class AndroidSchedulerIntegrationTest {
    private lateinit var system: ActorSystem
    private val config = Config(maxReductions = 100)
    private val androidContext = ApplicationProvider.getApplicationContext<Context>()
    private val scheduler = AndroidScheduler(config)
    private lateinit var shadowSensorManager: ShadowSensorManager

    @Before
    fun setup() {
        system = ActorSystem.create(config, scheduler)
        shadowSensorManager = Shadows.shadowOf(
            androidContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        )
    }

    @After
    fun tearDown() {
        scheduler.cleanAllWorkerQueues()
    }

    @Test
    fun `enqueue should add SensorActor to sensorActors map`() {
        val sensorActor = TestSensorActor(
            id = "sensor-1",
            initialState = State(0),
            context = androidContext
        )

        scheduler.enqueue(createExecutor(sensorActor))

        sensorActor.id?.let { scheduler.removeActor(it) }?.let { assertTrue(it) }
    }

    @Test
    fun `enqueue should delegate non-sensor actors to working stealing scheduler`() {
        val regularActor = RegularActor("regular-1", 0)
        val executor = createExecutor(regularActor)

        scheduler.enqueue(executor)

        regularActor.id?.let { scheduler.removeActor(it) }?.let { assertTrue(it) }
    }

    @Test
    fun `cleanAllWorkerQueues should remove all actors`() {
        val sensorExecutor = createExecutor(
            TestSensorActor(id = "sensor-1", initialState = State(0), context = androidContext)
        )

        val regularExecutor = createExecutor(RegularActor(id = "regular-1", initialState = 0))

        scheduler.enqueue(sensorExecutor)
        scheduler.enqueue(regularExecutor)
        scheduler.cleanAllWorkerQueues()

        assertFalse(scheduler.removeActor("sensor-1"))
        assertFalse(scheduler.removeActor("regular-1"))
    }

    @Test
    fun `should deliver sensor events to actor`() = runTest {

        val testSensor = ShadowSensor.newInstance(TYPE_ACCELEROMETER)
        shadowSensorManager.addSensor(testSensor)

        val testActor = TestSensorActor(
            id = "sensor-1",
            initialState = State(0),
            context = androidContext
        )

        //system.actorOf(id = "sensor-1", initialState = Unit) { id, state -> TestSensorActor(id, state, androidContext)}
        scheduler.enqueue(createExecutor(testActor))

        for (i in 1..1000) {
            shadowSensorManager.sendSensorEventToListeners(
                createSensorEvent(
                    testSensor,
                    floatArrayOf(1.0f, 2.0f, 3.0f)
                )
            )
        }

        runBlocking {
            delay(5000)
        }
    }

    private fun createSensorEvent(sensor: Sensor, values: FloatArray): SensorEvent {
        return ShadowSensorManager.createSensorEvent(values.size, TYPE_ACCELEROMETER)
    }

    private fun <S : Any, M : Any, R : Any> createExecutor(actor: Actor<S, M, R>): ActorExecutor<*> {
        actor.system = system

        val adapter = BaseActorAdapter(actor, actor.system)
        val mailbox = Mailbox(queue = ChannelMailbox<M>() as MailboxAbstractQueue<M>)
        val supervisor = Supervisor(
            id = "root-supervisor",
            strategy = SupervisorStrategy(RestartStrategy.OneForOne, 5)
        )

        return ActorExecutor(adapter, mailbox, supervisor.getMessageChannel())
    }

    data class State(var count: Int = 0)

    class TestSensorActor(
        id: String,
        initialState: State,
        context: Context,
        sensorType: Int = TYPE_ACCELEROMETER,
        samplingPeriod: Int = SensorManager.SENSOR_DELAY_NORMAL
    ) : SensorActor<State, SensorData>(
        id = id,
        initialState = initialState,
        androidContext = context,
        sensorType = sensorType,
        samplingPeriod = samplingPeriod
    ) {
        private val log = LoggerFactory.getLogger(TestSensorActor::class.java)

        override fun onReceive(
            message: SensorData,
            ctx: ActorContext<State>
        ): Pair<ActorContext<State>, Unit> {
            log.info("Message data: {}", message)
            val state: Int = ctx.state?.count?.plus(1) ?: 0
            log.info("New State: {}", state)
            return ctx.withState(newState = State(count = state)) to Unit
        }
    }

    class RegularActor<S : Any>(
        id: String,
        initialState: S?
    ) : Actor<S, String, String>(id, initialState) {
        override fun onReceive(
            message: String,
            ctx: ActorContext<S>
        ): Pair<ActorContext<S>, String> {
            println("Received message: $message")
            return ctx to "processed: $message"
        }
    }
}