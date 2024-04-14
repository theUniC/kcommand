# KCommand

This is a Kotlin-based project that extends the functionality of an event bus design pattern through sealed classes. It embraces the concept of sharing information between unrelated components, objects, or processes. And offers a great deal of flexibility by means of a layer of middleware that process messages sent to a Message Bus.

## Installation

TBD

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