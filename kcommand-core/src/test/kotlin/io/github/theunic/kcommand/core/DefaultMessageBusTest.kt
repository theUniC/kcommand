package io.github.theunic.kcommand.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

class DefaultMessageBusTest : BehaviorSpec({
    val middlewares = listOf<Middleware<String, Int>>()
    val messageBus = DefaultMessageBus(middlewares)

    given("A message bus with no subscriptions") {
        `when`("a message is sent to the bus") {
            val message = "testCommand"

            then("it should throw an exception") {
                runTest {
                    shouldThrow<IllegalArgumentException> {
                        val result = messageBus.handle(message)
                        result.await()
                    }
                }
            }
        }
    }

    given("A message bus with a subscription") {
        val messageHandler: suspend (String) -> Int = { it.length }
        messageBus.subscribe("test", messageHandler)

        `when`("a message sent to the message bus") {
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
            object : Middleware<String, String> {
                override suspend fun handle(
                    message: String,
                    next: suspend (String) -> CompletableDeferred<String>,
                ): CompletableDeferred<String> {
                    return next(message.uppercase())
                }
            }

        val newMiddlewares = listOf(modifyingMiddleware)
        val newMessageBus = DefaultMessageBus(newMiddlewares)
        val messageHandler = { command: String -> command }
        newMessageBus.subscribe("test", messageHandler)

        `when`("A message goes through middleware") {
            val message = "hello"

            then("it should be modified by the middleware before handling") {
                runTest {
                    val result = newMessageBus.handle(message)
                    result.await() shouldBe "HELLO"
                }
            }
        }
    }
})
