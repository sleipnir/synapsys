# Synapsys 🧠⚡
A lightweight, fast, and efficient **stateful** actor system for resource-constrained environments! (Working in progress)

## Why Synapsys? 🤔
Unlike heavyweight frameworks like **Akka**, **Synapsys** is designed to be **lightweight and blazing fast**, making it perfect for **small devices**, **embedded systems**, **and applications that need a minimal footprint**. 🚀

It provides a **simple and intuitive API** for building concurrent, stateful actors while keeping things efficient.

> "Big brains in small packages!" – Synapsys motto 😆

## Features 🌟

✅ **Lightweight** – Optimized for minimal resource usage.

✅ **Fast** – Built for high-performance message processing.

✅ **Simple API** – Easy to use and extend.

✅ **Concurrency Made Easy** – Uses [Erlang](https://blog.appsignal.com/2024/04/23/deep-diving-into-the-erlang-scheduler.html) inspired preemptive scheduler.

✅ **Stateful Actors** – Actors persist and maintain their state across messages.

---

## Quickstart Guide 🏁

1️⃣ **Install Synapsys**

To use Synapsys, add the following dependency to your **Gradle (Kotlin DSL)**:

```kotlin
dependencies {
    implementation("io.eigr.synapsys:synapsys-core:0.1.0")
}
```

Or for **Maven users**:

```xml
<dependency>
    <groupId>io.eigr.synapsys</groupId>
    <artifactId>synapsys-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

2️⃣ **Define a Stateful Actor** 🎭

Synapsys actors are ***stateful by default***, meaning they retain their state across multiple messages:

```kotlin
import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.Context
import org.slf4j.LoggerFactory

data class Message(private val text: String?)

class MyActor(id: String?, initialState: Int?) : Actor<Int, Message, String>(id, initialState) {
    private val log = LoggerFactory.getLogger(MyActor::class.java)

    override fun onReceive(message: Message, ctx: Context<Int>): Pair<Context<Int>, String> {
        log.info("Received message on Actor {}: {} with previous state: {}", id, message, ctx.state)
        val newCtx = ctx.update(ctx.state!! + 1)
        return ctx to "Processed: $message with new state: ${newCtx.state}"
    }
}
```

---

3️⃣ **Run the Actor System** 🚀

```kotlin
import io.eigr.synapsys.core.actor.ActorSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val actors: listOf<ActorPointer<Any>>() = (0..1000).map { i ->
        ActorSystem.createActor("my-actor-$i", 0) { id, initialState ->
            MyActor(
                id,
                initialState
            )
        } as ActorPointer<Any>
    }

    ActorSystem.start()

    actors.forEach { actor ->
        repeat(100) {
            actor.send(Message("Hello"))

            // or use ask pattern
            //val resp = actor.ask<String>(Message("Hello"))
            //println("Actor response is $resp")
        }
    }

    delay(120000)
}
```

---

## Why Stateful Actors Matter 🧠

Unlike traditional message processing models, stateful actors allow you to:

✅ **Keep track of internal state** across multiple messages.

✅ **Reduce database interactions** by maintaining state in-memory.

✅ **Simplify business logic** with event-driven processing.

But what makes **Synapsys** even more powerful is that **actors can persist their state in different ways**:

* **In-memory** for high-speed ephemeral processing.

* **Embedded databases** like **SQLite** for lightweight persistence.

* **Traditional databases** (e.g., PostgreSQL, MySQL) for long-term storage.

This makes **Synapsys** perfect for use cases like:

* A **chat system** where users reconnect and keep their conversation history.

* A **bank account service** where transactions update and persist balances.

* An **IoT controller** that maintains device states even after a restart.

---

## Performance 🔥

Synapsys is built for speed and efficiency. Here's what you get out of the box:

⚡ **Low-latency message processing**

⚡ **Efficient memory usage**

⚡ **Scales effortlessly across multiple actors**

⚡ **State persistence for long-running actors**

Run the example and see the results for yourself! 🚀

---

## Contributing 🤝

We ❤️ contributions! Found a bug? Want to add a feature? Open an issue or submit a pull request.

📜 License: MIT

---

Give it a ⭐ if you like it! 🎉