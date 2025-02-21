package io.github.theunic.kcommand.core.transport

import arrow.core.Either
import io.github.theunic.kcommand.core.Stopable
import io.github.theunic.kcommand.core.Transport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class AggregatorTransport<M : Any, R : Any>(
    private val transports: Map<String, Transport<M, R>> = mapOf(),
    private val transportResolver: (M) -> String? = { _ -> null },
) : Transport<M, R>,
    Stopable {
    private val localTransport: Transport<M, R> = LocalTransport()

    override suspend fun send(message: M): Either<Unit, CompletableDeferred<R>> {
        val transportName = transportResolver(message)
        val fallbackTransport =
            if (transportName == null) {
                localTransport
            } else {
                transports[transportName] ?: localTransport
            }

        return fallbackTransport.send(message)
    }

    override fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>> {
        val flows =
            buildList {
                add(localTransport.receive())
                addAll(transports.values.map { it.receive() })
            }
        return flows.merge()
    }

    override fun stop() {
        transports.values.forEach { tr ->
            if (tr is Stopable) {
                tr.stop()
            }
        }
    }
}
