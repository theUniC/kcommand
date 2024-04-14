package io.github.theunic.kcommand.core

import arrow.core.getOrElse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DefaultMessageBus<M : Any, R : Any>(
    middlewares: List<Middleware<M, R>> = listOf(),
    private val transport: Transport<M, R> = LocalTransport(),
) : AbstractMessageBus<M, R>(middlewares) {
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
}
