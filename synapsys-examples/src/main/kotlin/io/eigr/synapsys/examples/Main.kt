package io.eigr.synapsys.examples

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.core.actor.ActorSystem
import io.eigr.synapsys.core.actor.Context
import io.eigr.synapsys.core.actor.RestartStrategy
import io.eigr.synapsys.core.actor.Supervisor
import io.eigr.synapsys.core.actor.SupervisorStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

fun main() = runBlocking {
    ActorSystem.create()

    // Multi message Actor Example using ask pattern:
    val actorPointer: ActorPointer<Message> = ActorSystem.actorOf(
        "multi-message-actor", 0
    ) { id, initialState -> MultiMessageActor(id, initialState) }

    val helloResp = actorPointer.ask<String>(Hello("Hello", "Adriano"))
    val byeResp = actorPointer.ask<String>(Bye("Tchau", "Adriano"))

    println("Hello response $helloResp")
    println("Bye response $byeResp")

    // Custom Supervisor example:
    val myCustomSupervisor = Supervisor(
        "custom-supervisor",
        strategy = SupervisorStrategy(RestartStrategy.OneForOne, estimatedMaxRetries = 10)
    )

    val failingActor = ActorSystem.actorOf(
        id = "failing-actor",
        initialState = 0,
        myCustomSupervisor,
    ) { id, state ->
        object : Actor<Int, String, String>(id, state) {
            override fun onReceive(message: String, ctx: Context<Int>): Pair<Context<Int>, String> {
                println("Actor $id received: $message")
                if ((0..2).random() == 0) {
                    throw RuntimeException("Simulated failure")
                }
                return ctx.withState(ctx.state?.plus(1) ?: 0) to "Processed: $message"
            }
        }
    }

    repeat(1000) {
        failingActor.send("Hello")
    }

    // Creation of many actors and async message passing example:
    var actors = listOf<ActorPointer<Any>>()
    val creationTime = measureTime {
        actors = (0..80000).map { i ->
            ActorSystem.actorOf("my-actor-$i", 0) { id, initialState ->
                MyActor(
                    id, initialState
                )
            } as ActorPointer<Any>
        }
    }

    val executionTime = measureTime {
        actors.forEach { actor ->
            repeat(50) {
                actor.send(Message("Hello"))
            }
        }
    }
    delay(120000)
    println("Creation time: $creationTime")
    println("Execution time: $executionTime")
}

open class Message(open val text: String?)
class Hello(override val text: String?, val name: String) : Message(text)
class Bye(override val text: String?, val name: String) : Message(text)

class MultiMessageActor(id: String?, initialState: Int?) :
    Actor<Int, Message, String>(id, initialState) {
    override fun onReceive(message: Message, ctx: Context<Int>): Pair<Context<Int>, String> {
        when (message) {
            is Hello -> {
                val newCtx = ctx.withState(ctx.state!! + 1)
                return ctx to "Hello ${message.name} with new state: ${newCtx.state}"
            }

            is Bye -> {
                val newCtx = ctx.withState(ctx.state!! + 1)
                return ctx to "Bye bye ${message.name} with new state: ${newCtx.state}"
            }

            else -> {
                val newCtx = ctx.withState(ctx.state!! + 1)
                return ctx to "${message.text} with new state: ${newCtx.state}"
            }
        }
    }
}

class MyActor(id: String?, initialState: Int?) : Actor<Int, Message, String>(id, initialState) {
    private val log = LoggerFactory.getLogger(MyActor::class.java)

    override fun onReceive(message: Message, ctx: Context<Int>): Pair<Context<Int>, String> {
        log.info(
            "Received message on Actor {}: {} with state: {} on thread {}",
            id,
            message,
            ctx.state,
            Thread.currentThread()
        )

        val newCtx = ctx.withState(ctx.state!! + 1)
        return ctx to "Processed: ${message.text} with new state: ${newCtx.state}"
    }
}

