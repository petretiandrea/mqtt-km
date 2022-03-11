package io.github.petretiandrea

import io.github.petretiandrea.AsyncTest.collectCallback
import io.github.petretiandrea.AsyncTest.waitCallback
import io.github.petretiandrea.TestCollection.assertContentEqualsIgnoreOrder
import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.Publish
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe
import io.github.petretiandrea.mqtt.dsl.mqtt
import io.github.petretiandrea.mqtt.dsl.tcp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@Suppress("ClassOrdering")
@ExperimentalUnsignedTypes
class MqttTest {

    private lateinit var client: MqttClient

    private fun teardown(): Unit = runBlocking {
        client.disconnect()
    }

    private fun generateRandomTopic(): String {
        return "test/kotlin/topic${Random.nextInt(20_000)}"
    }


    companion object {
        val DEFAULT_TEST_TIMEOUT = 10.seconds
        fun createDefaultClient(scope: CoroutineScope, keepAliveSeconds: Int = 10): MqttClient =
            createWillMessageClient(scope, keepAliveSeconds, null)

        fun createWillMessageClient(
            scope: CoroutineScope,
            keepAliveSeconds: Int = 10,
            willMessage: Message?
        ): MqttClient {
            return mqtt(scope) {
                tcp {
                    hostname = "broker.hivemq.com"
                    port = 1883
                    clientId = "test-km-${generateRandomString(5)}"
                    keepAlive = keepAliveSeconds.seconds
                    this.willMessage = willMessage
                }
            }
        }
    }

    @Test
    fun nonExistingBrokerMustFailConnect() = runBlocking {
        client = mqtt {
            tcp {
                hostname = ""
            }
        }

        assertTrue { client.connect().isFailure }
    }

    @Test
    fun canConnectToServer() = runBlocking {
        client = createDefaultClient(this)
        val connected = client.connect()
        assertTrue(connected.isSuccess, "${connected.exceptionOrNull()}")
        assertTrue(client.isConnected)

        teardown()
    }

    @Test
    fun mustThrowConnectionErrorWithInvalidServer() = runBlocking {
        client = mqtt {
            tcp { hostname = "invalid.server.com"; clientId = "1234" }
        }
        assertTrue(client.connect().isFailure)
    }

    @Test
    fun canDisconnectFromServer() = runBlocking {
        client = createDefaultClient(this)
        client.connect()

        assertTrue(client.disconnect().isSuccess)
        assertTrue(!client.isConnected)

        teardown()
    }

    @Test
    fun canReconnectToServer() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        delay(1000)

        val disconnected = client.disconnect()
        assertTrue(disconnected.isSuccess, "${disconnected.exceptionOrNull()}")

        val reconnected = client.connect()
        assertTrue(reconnected.isSuccess, "${reconnected.exceptionOrNull()}")

        teardown()
    }

    @Test
    fun mustKeepConnectionActive() = runBlocking {
        client = createDefaultClient(this, 2).apply { connect() }
        delay(10_000)
        assertTrue(client.isConnected)
        assertTrue(client.disconnect().isSuccess)

        teardown()
    }

    @Test
    fun mustPublishWithDifferentQos() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = generateRandomTopic()
        val messages = QoS.values()
            .map { Message(topic, generateRandomString(6), it, retain = false, duplicate = false) }

        val waitResponses = collectCallback<Message>(2, DEFAULT_TEST_TIMEOUT) {
            client.onDeliveryCompleted { trySend(it) }
        }
        // publish
        val publishStates = messages.map { client.publish(it) }
        val collectedAck = waitResponses()

        assertTrue(publishStates.all { it })
        assertContentEqualsIgnoreOrder(messages.drop(1), collectedAck)

        teardown()
    }

    @Test
    fun mustSubscribeToTopicWithDifferentQos() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topics = QoS.values().map { it to "${generateRandomTopic()}${it.ordinal}" }.toList()
        val waitResponses = collectCallback<Pair<Subscribe, QoS>>(3, DEFAULT_TEST_TIMEOUT) {
            client.onSubscribeCompleted { subscribe, grantedQos ->
                trySend(subscribe to grantedQos)
            }
        }

        val published = topics.map { client.subscribe(it.second, it.first) }
        assertTrue(published.all { it })

        val collectedAck = waitResponses().map { it.second to it.first.topic } // qos -> topic

        assertContentEqualsIgnoreOrder(topics, collectedAck)
        teardown()
    }

    @Test
    fun mustReceiveMessageWithDifferentQos() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val messages = QoS.values().map {
            Message(
                generateRandomTopic(),
                generateRandomString(10),
                it,
            )
        }.toList()

        val waitMessages = collectCallback<Message>(3, DEFAULT_TEST_TIMEOUT) {
            client.onMessageReceived { trySend(it) }
        }

        val subscribed = messages.map { client.subscribe(it.topic, it.qos) }
        assertTrue(subscribed.all { it })

        messages.forEach { client.publish(it.topic, it.message, it.qos, it.retain, it.duplicate) }

        assertContentEqualsIgnoreOrder(
            messages.map { it.copy(messageId = 0) },
            waitMessages().map { it.copy(messageId = 0) })
        teardown()
    }

    @Test
    fun mustUnsubscribeFromTopic() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = generateRandomTopic()
        val waitUnsubscribe = waitCallback<Unsubscribe>(DEFAULT_TEST_TIMEOUT) {
            client.onUnsubscribeComplete {
                trySend(it)
            }
        }

        assertTrue(client.unsubscribe(topic))
        assertEquals(topic, waitUnsubscribe().topic)

        teardown()
    }

    @Test
    fun canPublishLargeMessage() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = generateRandomTopic()
        val messagePayload = generateRandomString(127)
        val message = Message(topic, messagePayload, qos = QoS.Q1, retain = false, duplicate = false)

        val waitPublish = waitCallback<Message>(DEFAULT_TEST_TIMEOUT) {
            client.onDeliveryCompleted { trySend(it) }
        }

        assertTrue(client.publish(message))
        assertEquals(message, waitPublish())

        teardown()
    }

    @Test
    fun canConnectWithWillMessage() = runBlocking {
        val topic = generateRandomTopic()
        val willMessage = Message(topic, generateRandomString(10), QoS.Q1, messageId = 1)

        val clientToDie = createWillMessageClient(this, willMessage = willMessage).apply { connect() }
        client = createDefaultClient(this).apply { connect() }

        val waitDeadMessage = waitCallback<Message>(DEFAULT_TEST_TIMEOUT) {
            client.onMessageReceived { trySend(it) }
        }

        assertTrue(client.subscribe(topic, QoS.Q1))

        delay(1000)
        assertTrue(clientToDie.disconnect(gracefully = false).isSuccess)
        assertEquals(willMessage, waitDeadMessage().copy(messageId = 1))

        teardown()
    }
}