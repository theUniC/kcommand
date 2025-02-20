package io.github.theunic.kcommand.transport.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.theunic.kcommand.core.MessageRegistry
import io.github.theunic.kcommand.core.RemoteTransport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import java.time.Duration
import java.util.Properties
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

suspend fun KafkaProducer<String, String>.sendAwait(record: ProducerRecord<String, String>): RecordMetadata =
    suspendCancellableCoroutine { cont ->
        send(record) { metadata, exception ->
            if (exception == null) {
                cont.resume(metadata)
            } else {
                cont.resumeWithException(exception)
            }
        }
    }

class KafkaStreamsRemoteTransport<M : Any, R : Any>(
    private val config: KafkaStreamsTransportConfig<M>,
    registry: MessageRegistry<M>,
) : RemoteTransport<M, R>(registry) {

    private val producer: KafkaProducer<String, String> by lazy {
        val props = Properties().apply {
            putAll(config.streamsProperties)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        }
        KafkaProducer<String, String>(props)
    }

    private val logger = KotlinLogging.logger {}

    private lateinit var streams: KafkaStreams

    override suspend fun doSend(kclass: KClass<out M>, serializedMessage: String) {
        val topic = config.topicResolver.invoke(kclass)
        producer.sendAwait(ProducerRecord(topic, serializedMessage))
    }

    override fun doReceiveFlow(): Flow<String> = callbackFlow {
        val builder = StreamsBuilder()

        val stream: KStream<String, String> = builder.stream(config.defaultTopic)
        stream.foreach { _, value ->
            val result = trySend(value)
            if (result.isFailure) {
                logger.error(result.exceptionOrNull()) {}
            }
        }

        val topology = builder.build()
        streams = KafkaStreams(topology, config.streamsProperties)
        streams.start()

        awaitClose {
            streams.close()
            producer.close()
        }
    }

    fun stop() {
        streams.close(Duration.ofSeconds(1))
        producer.close()
    }
}
