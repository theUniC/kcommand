package io.github.theunic.kcommand.core

import arrow.core.getOrElse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.reflect.KClass

class DefaultMessageBus<M : Any, R : Any>(
    private val middlewares: List<Middleware<M, R>> = listOf(),
    private val transport: Transport<M, R> = LocalTransport(),
) : MessageBus<M, R> {
    private val subscriptions: MutableMap<KClass<out M>, suspend (M) -> R> = mutableMapOf()

    init {
        transport.receive()
            .onEach { processCommand(it.first, it.second.getOrElse { CompletableDeferred() }) }
            .launchIn(
                CoroutineScope(Dispatchers.Default),
            )
    }

    override suspend fun handle(message: M): CompletableDeferred<R> {
        return transport.send(message).getOrElse { CompletableDeferred() }
    }

    override fun subscribe(
        message: M,
        messageHandler: suspend (M) -> R,
    ) {
        synchronized(subscriptions) {
            subscriptions[message::class] = messageHandler
        }
    }

    private suspend fun processCommand(
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

    private suspend fun handleCommand(command: M): R {
        return getCommandHandler(command::class)(command)
    }

    private fun getCommandHandler(commandClass: KClass<out M>): suspend (M) -> R {
        synchronized(subscriptions) {
            return subscriptions[commandClass]
                ?: throw IllegalArgumentException("No handler found for command: $commandClass")
        }
    }
}
