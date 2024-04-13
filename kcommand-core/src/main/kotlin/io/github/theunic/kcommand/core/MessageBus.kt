package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred

interface MessageBus<M, R> {
    suspend fun handle(message: M): CompletableDeferred<R>

    fun subscribe(
        message: M,
        messageHandler: suspend (M) -> R,
    )
}
