package io.github.theunic.kcommand.core

import arrow.core.Either
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull

abstract class RemoteTransport<M: Any, R: Any>(
    private val registry: MessageRegistry<M>,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        prettyPrint = false
    }
): Transport<M, R> {
    protected abstract fun doSend(serializedMessage: String)
    protected abstract fun doReceiveFlow(): Flow<String>

    override suspend fun send(message: M): Either<Unit, CompletableDeferred<R>> {
        val kclass = message::class
        val (typeName, serializer) = registry.typeNameAndSerializer(kclass)
            ?: return Either.Left(Unit)

        @Suppress("UNCHECKED_CAST")
        val payloadJson = json.encodeToString(serializer as KSerializer<M>, message)
        val envelope = Envelope(typeName, payloadJson)
        val envelopeJson = json.encodeToString(Envelope.serializer(), envelope)

        doSend(envelopeJson)

        // Fire-and-forget
        return Either.Left(Unit)
    }

    override fun receive(): Flow<Pair<M, Either<Unit, CompletableDeferred<R>>>> {
        return doReceiveFlow()
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
            }
            .filterNotNull()
    }
}
