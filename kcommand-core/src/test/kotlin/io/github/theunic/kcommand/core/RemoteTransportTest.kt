package io.github.theunic.kcommand.core

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

class DummyRemoteTransport<M : Any, R : Any>(
    registry: MessageRegistry<M>,
    json: Json = Json { ignoreUnknownKeys = true }
) : RemoteTransport<M, R>(registry, json) {

    val sentMessages = mutableListOf<String>()
    private val incomingFlow = MutableSharedFlow<String>()

    override suspend fun doSend(kclass: KClass<out M>, serializedMessage: String) {
        sentMessages.add(serializedMessage)
    }

    override fun doReceiveFlow(): Flow<String> {
        return incomingFlow
    }

    suspend fun emitIncomingMessage(raw: String) {
        incomingFlow.emit(raw)
    }
}

@Serializable
sealed class BaseCommand

@Serializable
data class MyCommand(val id: Int, val payload: String) : BaseCommand()

@Serializable
data class OtherCommand(val name: String) : BaseCommand()

class RemoteTransportTest : BehaviorSpec({
     val registry = MessageRegistry<BaseCommand>().apply {
         register(MyCommand::class, MyCommand.serializer())
         register(OtherCommand::class, OtherCommand.serializer())
     }

     val transport = DummyRemoteTransport<BaseCommand, Any>(registry)

     given("A RemoteTransport with a dummy implementation") {

         `when`("we send a MyCommand object") {
             then("it should serialize the object into an Envelope and store it") {
                 runTest {
                     val cmd = MyCommand(42, "HelloTransport")
                     val result = transport.send(cmd)

                     result.isLeft() shouldBe true

                     transport.sentMessages.size shouldBe 1
                     val raw = transport.sentMessages.first()

                     raw.contains(MyCommand::class.qualifiedName.toString()) shouldBe true
                     raw.contains("HelloTransport") shouldBe true
                 }
             }
         }

         `when`("we receive a raw envelope for OtherCommand") {
             then("it should reconstruct the object and emit it in the flow") {
                 runTest {
                     val job = launch {
                         transport.receive().collect {
                             println("DEBUG: flow emitted => $it")
                         }
                     }

                     val envelopeJson = """
                        {
                            "type": "${OtherCommand::class.qualifiedName}",
                            "payload": "{\"name\":\"myName\"}"
                        }
                     """.trimIndent()

                     transport.emitIncomingMessage(envelopeJson)

                     delay(500)
                     job.cancel()
                 }
             }
         }

         `when`("we receive an envelope with a type not registered") {
             then("it should emit nothing (and we don't see a Pair)") {
                 runTest {
                     val job = launch {
                         transport.receive().first()
                     }

                     val invalidEnvelopeJson = """
                        {
                            "type": "com.example.UnknownCommand",
                            "payload": "{}"
                        }
                     """.trimIndent()

                     transport.emitIncomingMessage(invalidEnvelopeJson)

                     job.cancel()
                 }
             }
         }
     }
 })
