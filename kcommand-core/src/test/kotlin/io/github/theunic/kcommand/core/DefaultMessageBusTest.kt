package io.github.theunic.kcommand.core

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

class DefaultMessageBusTest : BehaviorSpec({
    val middlewares = listOf<Middleware<String, Int>>()
    val messageBus = DefaultMessageBus(middlewares)

    given("an empty message bus") {
        `when`("a message is handled") {
            val message = "testCommand"
            val resultDeferred = CompletableDeferred<Int>()
            resultDeferred.complete(42)

            then("it should complete the deferred with the correct result") {
                runTest {
                    val result = messageBus.handle(message)
                    result.complete(42)
                    result.await() shouldBe 42
                }
            }
        }
    }

    given("a message bus with one subscription") {
        val messageHandler: suspend (String) -> Int = { it.length }
        messageBus.subscribe("test", messageHandler)

        `when`("a message is handled") {
            val message = "hello"

            then("it should handle the message using the subscribed handler") {
                runTest {
                    val result = messageBus.handle(message)
                    result.await() shouldBe 5
                }
            }
        }
    }

    given("a message bus with middleware") {
        val modifyingMiddleware =
            object : Middleware<String, Int> {
                override suspend fun handle(
                    message: String,
                    next: suspend (String) -> CompletableDeferred<Int>,
                ): CompletableDeferred<Int> {
                    return next(message.uppercase())
                }
            }
        val newMiddlewares = listOf(modifyingMiddleware)
        val newMessageBus = DefaultMessageBus(newMiddlewares)
        val messageHandler = { command: String -> command.length }
        newMessageBus.subscribe("test", messageHandler)

        `when`("a message goes through middleware") {
            val message = "hello"

            then("it should be modified by the middleware before handling") {
                runTest {
                    val result = newMessageBus.handle(message)
                    result.await() shouldBe "HELLO".length // Modified to uppercase before handling
                }
            }
        }
    }
})
