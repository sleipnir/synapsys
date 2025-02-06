package io.eigr.synapsys.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import io.eigr.synapsys.core.actor.ActorPointer;
import io.eigr.synapsys.core.actor.ActorSystem;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SynapsysBenchmark {

    private ActorPointer<Message> actor;

    @Setup
    public void setup() {
        ActorSystem system = ActorSystem.INSTANCE;
        system.create();

        actor = system.actorOf(
                "benchmark-actor",
                0,
                null,
                SimpleActor::new
        );

    }
    @Benchmark
    public void benchmarkAsk(Blackhole bh) {
       // bh.consume(actor.ask<String>(new Message("Hello")));
    }

    @Benchmark
    public void benchmarkSend(Blackhole bh) {
        //bh.consume(actor.send(new Message("Hello")));
    }
}