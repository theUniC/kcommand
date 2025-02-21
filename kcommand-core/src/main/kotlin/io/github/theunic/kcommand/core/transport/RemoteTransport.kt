package io.github.theunic.kcommand.core.transport

import arrow.core.Either
import io.github.theunic.kcommand.core.Envelope
import io.github.theunic.kcommand.core.MessageRegistry
import io.github.theunic.kcommand.core.Transport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

abstract class RemoteTransport<M : Any, R : Any>(
    private val registry: MessageRegistry<M>,
    private val json: Json =
        Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
            prettyPrint = false
        },
) : Transport<M, R> {
    protected abstract suspend fun doSend(
        kclass: KClass<out M>,
        serializedMessage: String,
    )

    protected abstract fun doReceiveFlow(): Flow<String>

    override suspend fun send(message: M): Either<Unit, CompletableDeferred<R>> {
        val kclass = message::class
        val (typeName, serializer) =
            registry.typeNameAndSerializer(kclass)
                ?: return Either.Left(Unit)

        @Suppress("UNCHECKED_CAST")
        val payloadJson = json.encodeToString(serializer as KSerializer<M>, message)
        val envelope = Envelope(typeName, payloadJson)
        val envelopeJson = json.encodeToString(Envelope.serializer(), envelope)

        doSend(kclass, envelopeJson)

        // Fire-and-forget
        return Either.Left(Unit)
    }

    override fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>> =
        doReceiveFlow()
            .map { raw ->
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(), raw)
                    val serializer = registry.serializerFor(envelope.type)
                    if (serializer != null) {
                        val obj = json.decodeFromString(serializer, envelope.payload)
                        val casted = obj as? M
                        if (casted != null) {
                            casted to Either.Left(Unit)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()
}
