package io.eigr.synapsys.benchmarks;

import org.jetbrains.annotations.NotNull;

import io.eigr.synapsys.core.actor.Actor;
import io.eigr.synapsys.core.actor.Context;
import kotlin.Pair;

public class SimpleActor extends Actor<Integer, Message, String> {

    public SimpleActor(@NotNull String id, Integer initialState) {
        super(id, initialState);
    }

    @Override
    public @NotNull Pair<Context<Integer>, String> onReceive(@NotNull Message message, @NotNull Context<Integer> ctx) {
        return new Pair<>(
                ctx.withState(ctx.getState() + 1),
                "Processed: " + message.text() + " with new state: " + ctx.getState()
        );
    }
}
