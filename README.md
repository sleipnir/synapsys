# Synapsys 🧠⚡
A lightweight, fast, and efficient actor system for resource-constrained environments! (Working in progress)

## Why Synapsys? 🤔
Unlike heavyweight frameworks like Akka, Synapsys is designed to be lightweight and blazing fast, making it perfect for small devices, embedded systems, and applications that need a minimal footprint. 🚀

It provides a simple and intuitive API for building concurrent, stateful actors while keeping things efficient.

> "Big brains in small packages!" – Synapsys motto 😆

## Features 🌟

✅ **Lightweight** – Optimized for minimal resource usage.

✅ **Fast** – Built for high-performance message processing.

✅ **Simple API** – Easy to use and extend.

✅ **Concurrency Made Easy** – Uses [Erlang](https://blog.appsignal.com/2024/04/23/deep-diving-into-the-erlang-scheduler.html) inspired preemptive scheduler.

---

## Quickstart Guide 🏁

1️⃣ Install Synapsys
To use Synapsys, add the following dependency to your **Gradle (Kotlin DSL)**:

```kotlin
dependencies {
    implementation("io.eigr.synapsys:synapsys-core:1.0.0")
}
```

Or for **Maven users**:

```xml
<dependency>
    <groupId>io.eigr.synapsys</groupId>
    <artifactId>synapsys-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

2️⃣ Define an Actor 🎭
Actors in Synapsys process messages and manage their own state:

```kotlin
import io.eigr.synapsys.core.actor.Actor
import org.slf4j.LoggerFactory

data class Message(private val text: String?)

class MyActor(id: String?, initialState: Int?) : Actor<Int, Message, String>(id, initialState) {
    private val log = LoggerFactory.getLogger(MyActor::class.java)

    override fun onReceive(message: Message, state: Int): Pair<Int, String> {
        log.info("Received message on Actor {}: {} with state: {}", id, message, state)
        val newState = state + 1
        return Pair(newState, "Processed: $message with new state: $newState")
    }
}
```

---

3️⃣ Run the Actor System 🚀

```kotlin
import io.eigr.synapsys.core.actor.ActorSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val actors = (0..1000).map { i ->
        ActorSystem.createActor("my-actor-$i", 0) { id, initialState ->
            MyActor(id, initialState)
        }
    }

    ActorSystem.start()

    actors.forEach { actor ->
        repeat(10) {
            ActorSystem.sendMessage(actor.id, Message("Hello"))
        }
    }

    delay(10000)
}
```

---

## Performance 🔥

Synapsys is built for speed and efficiency. Here's what you get out of the box:

⚡ **Low-latency message processing**

⚡ **Efficient memory usage**

⚡ **Scales effortlessly across multiple actors**

Run the example and see the results for yourself! 🚀

---

## Contributing 🤝

We ❤️ contributions! Found a bug? Want to add a feature? Open an issue or submit a pull request.

📜 License: MIT

---

Give it a ⭐ if you like it! 🎉