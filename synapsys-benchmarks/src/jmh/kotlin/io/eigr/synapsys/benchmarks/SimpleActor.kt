package io.eigr.synapsys.benchmarks

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.Context
import org.slf4j.LoggerFactory

class SimpleActor(id: String?, initialState: Int?) : Actor<Int, Message, String>(id, initialState) {
    private val log = LoggerFactory.getLogger(SimpleActor::class.java)

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