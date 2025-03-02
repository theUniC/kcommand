package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

abstract class AbstractMessageBus<M : Any, R : Any>(
    private val middlewares: List<Middleware<M, R>> = listOf(),
) : MessageBus<M, R> {
    private val subscriptions: MutableMap<KClass<out M>, MessageHandler<M, R>> = mutableMapOf()

    override fun subscribe(
        messageType: KClass<out M>,
        messageHandler: MessageHandler<M, R>,
    ) {
        synchronized(subscriptions) {
            subscriptions[messageType] = messageHandler
        }
    }

    protected suspend fun processCommand(
        message: M,
        deferred: CompletableDeferred<R>,
    ) {
        val next: suspend (M) -> CompletableDeferred<R> = { cmd ->
            try {
                deferred.complete(handleMessage(cmd))
                deferred
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                deferred
            }
        }

        val chain =
            middlewares.foldRight(next) { middleware, proceed ->
                { cmd -> middleware.handle(cmd, proceed) }
            }

        chain(message)
    }

    private suspend fun handleMessage(message: M): R = getMessageHandler(message::class).handle(message)

    private fun getMessageHandler(messageClass: KClass<out M>): MessageHandler<M, R> {
        synchronized(subscriptions) {
            return subscriptions[messageClass]
                ?: throw IllegalArgumentException("No handler found for message: $messageClass")
        }
    }
}
