package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred

class SimpleMessageBus<M : Any, R : Any>(
    middlewares: List<Middleware<M, R>> = listOf(),
) : AbstractMessageBus<M, R>(middlewares) {
    override suspend fun handle(message: M): CompletableDeferred<R> {
        val deferred = CompletableDeferred<R>()
        processCommand(message, deferred)
        return deferred
    }
}
