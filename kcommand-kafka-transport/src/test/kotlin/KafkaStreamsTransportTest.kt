package io.github.theunic.kcommand.transport.kafka

import io.github.theunic.kcommand.core.MessageRegistry
import io.kotest.common.ExperimentalKotest
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Properties

enum class Topics(val topicName: String) {
    DEFAULT("testTopic")
}

@OptIn(ExperimentalKotest::class)
class KafkaStreamsTransportTest : BehaviorSpec({
    context("The KafkaStreamsTransport should send and receive messages using Kafka streams") {
        given("A Kafka cluster") {
            val kafka = install(ContainerExtension(KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.1"))))

            beforeSpec {
                kafka.start()
            }

            val registry = MessageRegistry<TestMessage>()
            registry.register(
                TestMessage::class,
                TestMessage.serializer()
            )

            val transportConfig = KafkaStreamsTransportConfig.basic<TestMessage, Topics>(
                applicationId = "kcommand-streams-test",
                bootstrapServers = kafka.bootstrapServers,
                inputTopics = listOf(Topics.DEFAULT),
                topicResolver = { _ -> Topics.DEFAULT }
            )

            val transport = KafkaStreamsRemoteTransport<TestMessage, Any, Topics>(
                config = transportConfig,
                registry = registry,
            )

            val properties = Properties()
            properties["bootstrap.servers"] = kafka.bootstrapServers
            properties["connections.max.idle.ms"] = 10000
            properties["request.timeout.ms"] = 5000
            val adminClient = KafkaAdminClient.create(properties)
            adminClient.createTopics(listOf(NewTopic("testTopic", 1, 1)))

            afterSpec {
                kafka.stop()
                transport.stop()
            }

            `when`("the cluster is inspected") {
                val topics = adminClient.listTopics()
                topics.names().whenComplete { topicNames, _ ->
                    topicNames shouldContain "testTopic"
                }
            }

            given("A KafkaStreamsRemoteTransport with a real Kafka container") {
                `when`("calling send(...)") {
                    then("the message should be received in the flow") {
                        runTest {
                            val job = launch {
                                val (msg, either) = transport.receive().first()
                                msg.content shouldBe "Hello from Testcontainers"
                                either.isLeft() shouldBe true
                                this.cancel()
                            }

                            // Wait for the topology to start
                            delay(2000)

                            val result = transport.send(TestMessage("Hello from Testcontainers"))
                            result.isLeft() shouldBe true

                            job.join()
                        }
                    }
                }
            }
        }
    }
})
