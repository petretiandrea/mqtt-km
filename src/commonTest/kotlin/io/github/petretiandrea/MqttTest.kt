package io.github.petretiandrea

import io.github.petretiandrea.AsyncTest.collectCallback
import io.github.petretiandrea.AsyncTest.waitCallback
import io.github.petretiandrea.TestCollection.assertContentEqualsIgnoreOrder
import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.*
import kotlinx.coroutines.*
import kotlin.test.*
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
        client = createDefaultClient(this).apply { connect() }
        delay(1000)

        val disconnected = client.disconnect()
        assert(disconnected.isSuccess) { "${disconnected.exceptionOrNull()}" }

        val reconnected = client.connect()
        assert(reconnected.isSuccess) { "${reconnected.exceptionOrNull()}" }

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
    fun mustPublishWithDifferentQos() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val messages = QoS.values()
            .map { Message(MessageId.generate(), "test/kotlin", "hello", it, retain = false, duplicate = false) }

        val waitResponses = collectCallback<Message>(2, 5.seconds) {
            client.onDeliveryCompleted { trySend(it) }
        }
        // publish
        val publishStates = messages.map { client.publish(it) }
        val collectedAck = waitResponses()

        assert(publishStates.all { it })
        assertContentEqualsIgnoreOrder(messages.drop(1), collectedAck)

        teardown()
    }

    @Test
    fun mustSubscribeToTopicWithDifferentQos() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topics = QoS.values().map { it to "test/kotlin${it.ordinal}" }.toList()
        val waitResponses = collectCallback<Pair<Subscribe, QoS>>(3, 5.seconds) {
            client.onSubscribeCompleted { subscribe, grantedQos ->
                trySend(subscribe to grantedQos)
            }
        }

        val published = topics.map { client.subscribe(it.second, it.first) }
        val collectedAck = waitResponses().map { it.second to it.first.topic } // qos -> topic

        assert(published.all { it })
        assertContentEqualsIgnoreOrder(topics, collectedAck)
        teardown()
    }

    @Test
    fun mustUnsubscribeFromTopic() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = "test/kotlin"
        val waitUnsubscribe = waitCallback<Unsubscribe>(3.seconds) {
            client.onUnsubscribeComplete {
                trySend(it)
            }
        }

        assert(client.unsubscribe(topic))
        assertEquals(topic, waitUnsubscribe().topic)

        teardown()
    }
}