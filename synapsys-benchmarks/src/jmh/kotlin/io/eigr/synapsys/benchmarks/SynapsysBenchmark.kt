package io.eigr.synapsys.benchmarks

import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.core.actor.ActorSystem
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class SynapsysBenchmark {

    private lateinit var actor: ActorPointer<Message>

    @Setup
    fun setup() {
        runBlocking {
            ActorSystem.create()
            actor = ActorSystem.actorOf("benchmark-actor", 0) { id, initialState ->
                SimpleActor(id, initialState)
            }
        }
    }

    @Benchmark
    fun benchmarkAsk(): String {
        return runBlocking { actor.ask<String>(Message("Hello")) }
    }

    @Benchmark
    fun benchmarkSend() {
        runBlocking { actor.send(Message("Hello")) }
    }
}
