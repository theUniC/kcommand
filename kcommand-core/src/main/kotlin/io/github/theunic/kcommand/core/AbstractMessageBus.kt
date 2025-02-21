package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

abstract class AbstractMessageBus<M : Any, R : Any>(
    private val middlewares: List<Middleware<M, R>> = listOf(),
) : MessageBus<M, R> {
    private val subscriptions: MutableMap<KClass<out M>, suspend (M) -> R> = mutableMapOf()

    override fun subscribe(
        messageType: KClass<out M>,
        messageHandler: suspend (M) -> R,
    ) {
        synchronized(subscriptions) {
            subscriptions[messageType] = messageHandler
        }
    }

    protected suspend fun processCommand(
        command: M,
        deferred: CompletableDeferred<R>,
    ) {
        val next: suspend (M) -> CompletableDeferred<R> = { cmd ->
            try {
                deferred.complete(handleCommand(cmd))
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

        chain(command)
    }

    private suspend fun handleCommand(command: M): R = getCommandHandler(command::class)(command)

    private fun getCommandHandler(commandClass: KClass<out M>): suspend (M) -> R {
        synchronized(subscriptions) {
            return subscriptions[commandClass]
                ?: throw IllegalArgumentException("No handler found for command: $commandClass")
        }
    }
}
