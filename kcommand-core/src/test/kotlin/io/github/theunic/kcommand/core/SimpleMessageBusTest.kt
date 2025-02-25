package io.github.theunic.kcommand.core

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SimpleMessageBusTest :
    BehaviorSpec({
        given("A Simple Message Bus") {
            val messageBus = SimpleMessageBus<String, Int>()
            `when`("A subscription is done") {
                messageBus.subscribe(
                    String::class,
                    object : MessageHandler<String, Int> {
                        override suspend fun handle(message: String) = message.length
                    },
                )
                and("A message is sent to the bus") {
                    val message = "Hello, world!"
                    val result = messageBus.handle(message).await()
                    then("The result should be the length of the message") {
                        result shouldBe message.length
                    }
                }
            }
        }
    })
