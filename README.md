# KCommand

This project offers a flexible implementation of an event bus design pattern in Kotlin, with an emphasis on the integration of middleware. It supports diverse communication demands between any types of components and enables additional processing layers through middleware, enhancing the modularity and adaptability of your application.

## Installation

Add this to your `build.gradle.kts`

```kotlin
dependencies {
    implementation("io.github.theunic:kcommand-core")
}
```

## Usage

### Simple message bus

```kotlin
import io.github.theunic.kcommand.core.SimpleMessageBus

fun main() {
    val messageBus = SimpleMessageBus<String, String>()
    messageBus.subscribe(String::class) { it.uppercase() }
    val result = messageBus.handle("hello world!")
    println(result.await()) // => "HELLO WORLD!"
}
```

### DefaultMessageBus

The `DefaultMessageBus` makes use of corroutines, suspended functions and Kotlin's FLOW API to handle messages. It introduces the concept of Transports in order to decouple the message sending from the Message Bus allowing for other message distribution mechanisms to be able to distribute messages.

```kotlin
import io.github.theunic.kcommand.core.DefaultMessageBus
import kotlinx.coroutines.runBlocking

fun main() {
    val messageBus = DefaultMessageBus<String, Int>()
    messageBus.subscribe(String::class) { it.length }
    runBlocking {
        val result = messageBus.handle("hello world!")
        println(result.await()) // => 12
    }
}
```

#### Using multiples transports

Sometimes, you want your DefaultMessageBus to communicate through multiple transports at once (for example, a Kafka transport for some messages and a local fallback for others). In that scenario, you can create an AggregatorTransport that internally holds references to several sub-transports. The AggregatorTransport implements the Transport interface and delegates to each sub-transport under the hood, including a local transport as a fallback if none is specified:

```kotlin
// Example aggregator configuration
import kotlinx.serialization.Serializable

// Suppose you have different sub-transports:
val kafkaTransport = KafkaTransport()
val rabbitTransport = RabbitMQTransport()

@Serializable
sealed class MyMessage {
    @Serializable
    data class KafkaMessage(val message: String) : MyMessage()
    @Serializable
    data class RabbitMQMessage(val message: String) : MyMessage()
    @Serializable
    data class DefaultMessage(val message: String) : MyMessage()
}

// Create an aggregator transport with them:
val aggregatorTransport = AggregatorTransport(
    transports = mapOf(
        "kafka" to kafkaTransport,
        "rabbit" to rabbitTransport
    ),
    transportResolver = { message: MyMessage ->
        // Decide which transport name to use (kafka? rabbit? or null for local)
        when (message) {
            is MyMessage.KafkaMessage -> "kafka"
            is MyMessage.RabbitMQMessage -> "rabbit"
            else -> null
        }
    }
)

// Then, create the DefaultMessageBus using that aggregator:
val messageBus = DefaultMessageBus<MyMessage, Int>(
    transport = aggregatorTransport
)

runBlocking {
    // A KafkaMessage
    messageBus.handle(MyMessage.KafkaMessage("Hello Kafka"))
    // A RabbitMQMessage
    messageBus.handle(MyMessage.RabbitMQMessage("Hello Rabbit"))
    // A local message
    messageBus.handle(MyMessage.DefaultMessage("local processing"))
}
```

With this approach, the DefaultMessageBus sees only one Transport (the aggregator), but it can route different messages to different sub-transports based on your custom logic (annotation, message type, etc.). If neither your logic nor the map contains a match, the aggregator’s built-in local transport will handle the message

## Transports

A Transport is responsible for either sending messages somewhere (e.g., publishing them to a remote broker) or receiving them (e.g., consuming from a queue/topic) and ultimately passing them into the bus.

1. LocalTransport: Processes messages entirely in-process (no remote broker).
2. KafkaStreamsRemoteTransport: Uses Kafka Streams to consume and/or publish messages to Kafka.
3. AggregatorTransport: A composite transport that can hold multiple sub-transports (Kafka, Rabbit, local fallback, etc.) and decide at runtime which one to use.

Each Transport implements

```kotlin
interface Transport<M : Any, R : Any> {
    suspend fun send(message: M): Either<Unit, CompletableDeferred<R>>
    fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>>
}
```

* `send(message)` 👉 may push the message to a remote system or process it locally (depending on the implementation).
* `reive()` 👉 provides a Flow of incoming messages from that transport (e.g., from Kafka, Rabbit, or a local channel).

Below are some of the provided transports (besides the AggregatorTransport, which is documented above with DefaultMessageBus):

### LocalTransport

The simplest transport is LocalTransport, which processes all messages in-process without any remote broker. It’s ideal for testing or purely local scenarios

```kotlin
// Example usage with LocalTransport
val localTransport = LocalTransport<String, Int>()

// Create the DefaultMessageBus specifying the local transport
val messageBus = DefaultMessageBus<String, Int>(
    transport = localTransport
)

// Subscribe locally
messageBus.subscribe(String::class) { it.length }

// Dispatch a message
runBlocking {
    val result = messageBus.handle("A local message")
    println(result.await()) // => 14
}
```

Since everything remains in-process, there’s no remote queue or topic to consume. This transport is perfect for simpler testing or “single JVM” usage.

### KafkaStreamsRemoteTransport

**_KafkaStreamsRemoteTransport_** leverages Kafka Streams to both publish messages to Kafka and consume them from Kafka topics

#### Installation

Kafka transport lives in it's own separate package

```kotlin
dependencies {
    // ...
    implementation("io.github.theunic:kcommand-core")
    implementation("io.github.theunic:kcommand-kafka-transport")
}
```

#### Usage example

```kotlin
// Suppose we have some config and a "MessageRegistry" for serialization
val kafkaConfig = KafkaTransportConfig(
    applicationId = "my-kafka-streams-app",
    bootstrapServers = "localhost:9092",
    // other settings if needed
)

val kafkaTransport = KafkaStreamsRemoteTransport<String, Int>(
    config = kafkaConfig
    // possibly pass a serializer or registry if needed
)

// Create the DefaultMessageBus
val messageBus = DefaultMessageBus<String, Int>(
    transport = kafkaTransport
)

// Subscribing locally is optional, or you can rely on external handling
messageBus.subscribe(String::class) { it.length }

// Now when you do:
runBlocking {
    val deferred = messageBus.handle("Hello Kafka")
    println(deferred.await()) // logic depends on the transport's approach
}

// Meanwhile, KafkaStreamsRemoteTransport will consume messages from the configured topics
// and re-inject them into the bus, if you collect the flow or run it in parallel.
```

## Contributing

Contributions of all sizes are welcome.

To contribute:

1. Fork the project.
2. Create your feature branch: git checkout -b my-new-feature.
3. Commit your changes: git commit -am 'Add some feature'.
4. Push to the branch: git push origin my-new-feature.
5. Submit a pull request.

Before your PR can be approved, please ensure that it does not fail any CI checks.

## License

This project is licensed under a standard MIT license. Check the LICENSE.md file for more details.
