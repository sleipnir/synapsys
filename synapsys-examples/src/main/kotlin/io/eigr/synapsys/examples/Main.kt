package io.eigr.synapsys.examples

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.core.actor.ActorSystem
import io.eigr.synapsys.core.actor.Context

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.slf4j.LoggerFactory
import kotlin.time.measureTime

data class Message(val text: String?)

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

        val newCtx = ctx.update(ctx.state!! + 1)
        return ctx to "Processed: ${message.text} with new state: ${newCtx.state}"
    }
}

fun main() = runBlocking {
    ActorSystem.create()

    var actors = listOf<ActorPointer<Any>>()
    val creationTime = measureTime {
        actors = (0..2).map { i ->
            ActorSystem.createActor("my-actor-$i", 0) { id, initialState ->
                MyActor(
                    id,
                    initialState
                )
            } as ActorPointer<Any>
        }
    }

    val executionTime = measureTime {
        actors.forEach { actor ->
            repeat(10000) {
                actor.send(Message("Hello"))

                // or use ask pattern
                //val resp = actor.ask<String>(Message("Hello"))
                //println("Actor response is $resp")
            }
        }
    }

    delay(120000)
    println("Creation time: $creationTime")
    println("Execution time: $executionTime")
}


