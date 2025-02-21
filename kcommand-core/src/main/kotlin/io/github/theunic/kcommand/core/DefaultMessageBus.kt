package io.github.theunic.kcommand.core

import arrow.core.getOrElse
import io.github.theunic.kcommand.core.transport.AggregatorTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DefaultMessageBus<M : Any, R : Any>(
    middlewares: List<Middleware<M, R>> = listOf(),
    private val transport: AggregatorTransport<M, R> = AggregatorTransport(),
) : AbstractMessageBus<M, R>(middlewares),
    Stopable {
    init {
        transport
            .receive()
            .onEach { processCommand(it.first, it.second.getOrElse { CompletableDeferred() }) }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    override suspend fun handle(message: M): CompletableDeferred<R> =
        transport
            .send(message)
            .getOrElse { CompletableDeferred() }

    override fun stop() {
        transport.stop()
    }
}
