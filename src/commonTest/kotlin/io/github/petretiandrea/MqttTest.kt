package io.github.petretiandrea

import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.MqttClientImpl
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.Connect
import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalUnsignedTypes
class MqttTest {

    private lateinit var client: MqttClient


    private fun teardown(): Unit = runBlocking {
        client.disconnect()
    }

    companion object {
        val SETTINGS = ConnectionSettings(
            hostname = "broker.hivemq.com",
            port = 1883,
            clientId = "test-km",
            username = null,
            password = null,
            cleanSession = true,
            willMessage = null,
            keepAliveSeconds = 5
        )

        fun createDefaultClient(scope: CoroutineScope, keepAliveSeconds: Int = 10): MqttClient {
            return MqttClientImpl(
                transport = Transport.tcp(),
                connectionSettings = SETTINGS.copy(keepAliveSeconds = keepAliveSeconds),
                scope = scope,
                dispatcher = Dispatchers.Default,
                session = ClientSession(SETTINGS.clientId, SETTINGS.cleanSession)
            )
        }
    }

    @Test
    fun canConnectToServer() = runBlocking {
        client = createDefaultClient(this)
        val connected = client.connect()
        assert(connected.isSuccess) { "${connected.exceptionOrNull()}" }
        assert(client.isConnected)

        teardown()
    }

    @Test
    fun canDisconnectFromServer() = runBlocking {
        client = createDefaultClient(this)
        client.connect()

        assert(client.disconnect().isSuccess)
        assert(!client.isConnected)

        teardown()
    }

    @Test
    fun canReconnectToServer() = runBlocking {
        client = createDefaultClient(this)
        val connected = client.connect()
        assert(connected.isSuccess) { "${connected.exceptionOrNull()}" }

        delay(500)

        val disconnected = client.disconnect()
        assert(disconnected.isSuccess) { "${disconnected.exceptionOrNull()}" }

        val reconnected = client.connect()
        assert(reconnected.isSuccess) { "${reconnected.exceptionOrNull()}" }

        assert(client.disconnect().isSuccess)

        teardown()
    }

    @Test
    fun mustKeepConnectionActive() = runBlocking {
        client = createDefaultClient(this, 2).apply { connect() }
        delay(10000)
        assert(client.isConnected)
        assert(client.disconnect().isSuccess)

        teardown()
    }

    @Test
    fun canPublishQos0() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val published = client.publish(Message(0, "test/kotlin", "hello", QoS.Q0, retain = false, duplicate = false))
        assert(published)

        teardown()
    }

    @Test
    fun mustSubscribeToTopic() = runBlocking {
        client = createDefaultClient(this)


        teardown()
    }


}