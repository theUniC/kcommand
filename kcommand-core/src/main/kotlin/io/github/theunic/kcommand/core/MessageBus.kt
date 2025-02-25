package io.github.theunic.kcommand.core

import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

interface MessageBus<M : Any, R> {
    suspend fun handle(message: M): CompletableDeferred<R>

    fun subscribe(
        messageType: KClass<out M>,
        messageHandler: MessageHandler<M, R>,
    )
}
