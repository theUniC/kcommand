package io.github.theunic.kcommand.core

import arrow.core.Either
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LocalTransport<M : Any, R : Any> : Transport<M, R> {
    private val commandEmitter: MutableSharedFlow<Pair<M, Either<Unit, CompletableDeferred<R>>>> = MutableSharedFlow()

    override suspend fun send(message: M): Either<Unit, CompletableDeferred<R>> {
        val result = Either.Right(CompletableDeferred<R>())
        commandEmitter.emit(message to result)
        return result
    }

    override fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>> = commandEmitter.asSharedFlow()
}
