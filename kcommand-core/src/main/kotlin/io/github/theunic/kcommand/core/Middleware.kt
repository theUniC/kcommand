package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred

interface Middleware<M, R> {
    suspend fun handle(
        message: M,
        next: suspend (M) -> CompletableDeferred<R>,
    ): CompletableDeferred<R>
}
