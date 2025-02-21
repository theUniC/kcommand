package io.github.theunic.kcommand.transport.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import java.util.Properties
import kotlin.reflect.KClass

data class KafkaStreamsTransportConfig<M : Any, TOPIC : Enum<TOPIC>>(
    val inputTopics: List<TOPIC>,
    val topicResolver: (KClass<out M>) -> TOPIC,
    val streamsProperties: Properties,
) {
    companion object {
        /**
         * Helper to create a basic config object.
         * @param applicationId The application.id for Kafka Streams.
         * @param bootstrapServers Kafka bootstrap addresses.
         */
        fun <M : Any, TOPIC : Enum<TOPIC>> basic(
            applicationId: String,
            bootstrapServers: String,
            inputTopics: List<TOPIC>,
            topicResolver: (KClass<out M>) -> TOPIC,
        ): KafkaStreamsTransportConfig<M, TOPIC> {
            val props =
                Properties().apply {
                    put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
                    put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde::class.java.name)
                    put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde::class.java.name)
                }

            return KafkaStreamsTransportConfig(
                streamsProperties = props,
                topicResolver = topicResolver,
                inputTopics = inputTopics,
            )
        }
    }
}
