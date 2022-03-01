package io.github.petretiandrea

import io.github.petretiandrea.AsyncTest.collectCallback
import io.github.petretiandrea.AsyncTest.waitCallback
import io.github.petretiandrea.TestCollection.assertContentEqualsIgnoreOrder
import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe
import io.github.petretiandrea.mqtt.dsl.mqtt
import io.github.petretiandrea.mqtt.dsl.tcp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
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

    companion object {
        fun createDefaultClient(scope: CoroutineScope, keepAliveSeconds: Int = 10): MqttClient {
            return mqtt(scope) {
                tcp {
                    hostname = "broker.hivemq.com"
                    port = 1883
                    clientId = "test-km"
                    keepAlive = keepAliveSeconds.seconds
                }
            }
        }
    }

    @Test
    fun canConnectToServer() = runBlocking {
        client = createDefaultClient(this)
        val connected = client.connect()
        assertTrue(connected.isSuccess, "${connected.exceptionOrNull()}" )
        assertTrue(client.isConnected)

        teardown()
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
        val messages = QoS.values()
            .map { Message(MessageId.generate(), "test/kotlin", "hello", it, retain = false, duplicate = false) }

        val waitResponses = collectCallback<Message>(2, 5.seconds) {
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
        val topics = QoS.values().map { it to "test/kotlin${it.ordinal}" }.toList()
        val waitResponses = collectCallback<Pair<Subscribe, QoS>>(3, 5.seconds) {
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
    fun mustUnsubscribeFromTopic() = runBlocking {
        client = createDefaultClient(this).apply { connect() }
        val topic = "test/kotlin"
        val waitUnsubscribe = waitCallback<Unsubscribe>(3.seconds) {
            client.onUnsubscribeComplete {
                trySend(it)
            }
        }

        assertTrue(client.unsubscribe(topic))
        assertEquals(topic, waitUnsubscribe().topic)

        teardown()
    }
}