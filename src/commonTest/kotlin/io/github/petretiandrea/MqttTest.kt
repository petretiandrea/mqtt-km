package io.github.petretiandrea

import io.github.petretiandrea.AsyncTest.waitCallback
import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.MqttClientImpl
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.*
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.transport.Transport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.callbackFlow
import platform.posix.time
import kotlin.native.concurrent.Future
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


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
            return MqttClient(
                scope,
                SETTINGS.copy(keepAliveSeconds = keepAliveSeconds),
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
    fun mustPublishWithQos0() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        assert(
            client.publish(Message(0, "test/kotlin", "hello", QoS.Q0, retain = false, duplicate = false))
        )
        teardown()
    }

    @Test
    fun mustPublishWithQos1() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val message = Message(MessageId.generate(), "test/kotlin", "hello", QoS.Q1, retain = false, duplicate = false)
        val waitPublish = waitCallback<Message>(2.seconds) {
            client.onDeliveryCompleted { trySend(it) }
        }

        assert(client.publish(message))
        waitPublish().let {
            assertEquals(message.topic, it.topic)
            assertEquals(message.message, it.message)
            assertEquals(message.qos, it.qos)
        }

        teardown()
    }

    @Test
    fun mustPublishWithQos2() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val message = Message(MessageId.generate(), "test/kotlin", "hello", QoS.Q2, retain = false, duplicate = false)
        val waitPublish = waitCallback<Message>(2.seconds) {
            client.onDeliveryCompleted { trySend(it) }
        }

        assert(client.publish(message))
        waitPublish().let {
            assertEquals(message.topic, it.topic)
            assertEquals(message.message, it.message)
            assertEquals(message.qos, it.qos)
        }

        teardown()
    }

    @Test
    fun mustSubscribeToTopicQos0() = runBlocking {
        client = createDefaultClient(this)
        assert(client.subscribe("test/kotlin", QoS.Q0))

        teardown()
    }

    @Test
    fun mustSubscribeToTopicQos1() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = "test/kotlin"
        val waitSubscribe = waitCallback<Subscribe>(timeout = 2.seconds) {
            client.onSubscribeCompleted {
                trySend(it)
            }
        }
        assert(client.subscribe(topic, QoS.Q1))
        val subscription = waitSubscribe()
        assertEquals(topic, subscription.topic)
        assertEquals(QoS.Q1, subscription.qos)
        teardown()
    }

    @Test
    fun mustSubscribeToTopicQos2() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = "test/kotlin"
        val waitSubscribe = waitCallback<Subscribe>(timeout = 2.seconds) {
            client.onSubscribeCompleted {
                trySend(it)
            }
        }
        assert(client.subscribe(topic, QoS.Q2))
        val subscription = waitSubscribe()
        assertEquals(topic, subscription.topic)
        assertEquals(QoS.Q2, subscription.qos)
        teardown()
    }

}