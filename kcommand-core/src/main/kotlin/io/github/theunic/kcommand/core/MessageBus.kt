package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

interface MessageBus<M : Any, R> {
    suspend fun handle(message: M): CompletableDeferred<R>

    fun subscribe(
        message: KClass<out M>,
        messageHandler: suspend (M) -> R,
    )
}
