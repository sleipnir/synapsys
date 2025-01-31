package io.creativesource.synapsys.examples

import io.creativesource.synapsys.core.actor.Actor
import io.creativesource.synapsys.core.actor.ActorSystem
import io.creativesource.synapsys.core.internals.BaseActor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

data class Message(private val text: String?)

class MyActor(id: String?, initialState: Int?) : Actor<Int, Message, String>(id, initialState) {
    private val log = LoggerFactory.getLogger(MyActor::class.java)

    override fun onReceive(message: Message, state: Int): Pair<Int, String> {
        log.info(
            "Received message on Actor {}: {} with state: {} on thread {}",
            id,
            message,
            state,
            Thread.currentThread()
        )
        val newState = state + 1
        return Pair(newState, "Processed: $message with new state: $newState")
    }
}

fun main() = runBlocking {
    var actors = listOf<BaseActor>()

    val creationTime = measureTime {
        actors = (0..8).map { i ->
            ActorSystem.createActor("my-actor-$i", 0) { id, initialState ->
                MyActor(
                    id,
                    initialState
                )
            }
        }

        ActorSystem.start()
    }


    val executionTime = measureTime {
        actors.forEach { actor ->
            repeat(1000) {
                ActorSystem.sendMessage(actor.id, Message("Hello"))
            }
        }
    }

    delay(10000)
    println("Creation time: $creationTime")
    println("Execution time: $executionTime")
}


