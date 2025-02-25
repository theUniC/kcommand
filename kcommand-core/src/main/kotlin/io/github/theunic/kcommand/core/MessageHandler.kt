package io.github.theunic.kcommand.core

interface MessageHandler<M, R> {
    suspend fun handle(message: M): R
}
