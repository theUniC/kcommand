package io.github.theunic.kcommand.core

import arrow.core.Either
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow

interface Transport<M : Any, R : Any> {
    suspend fun send(message: M): Either<Unit, CompletableDeferred<R>>
    fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>>
}
