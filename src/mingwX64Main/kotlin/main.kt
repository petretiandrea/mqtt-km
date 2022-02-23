import io.github.petretiandrea.mqtt.MqttClientImpl
import io.github.petretiandrea.mqtt.SubscribeCallback
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.transport.BufferedPacketReader
import io.github.petretiandrea.mqtt.core.transport.DefaultBufferedPacketReader
import io.github.petretiandrea.mqtt.core.transport.Transport
import io.github.petretiandrea.socket.buffer.allocMultiplatformBuffer
import io.github.petretiandrea.socket.createSocket
import io.github.petretiandrea.socket.stream.BufferedInputStream.Companion.buffered
import kotlinx.coroutines.*

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun main() = runBlocking {

    val settings = ConnectionSettings(
        hostname = "broker.hivemq.com",
        port = 1883,
        clientId = "test-km",
        username = null,
        password = null,
        cleanSession = true,
        willMessage = null,
        keepAliveSeconds = 5
    )

    val mqtt = MqttClientImpl(
        transport = Transport.tcp(),
        connectionSettings = settings,
        scope = this,
        session = ClientSession(settings.clientId, settings.cleanSession)
    )

    mqtt.connect()

    val published = mqtt.publish(Message(1, "test/kotlin", "hello2", QoS.Q2, retain = false, duplicate = false))
    println(published)

    println(mqtt.subscribe("test/kotlin", QoS.Q2))

    mqtt.registerSubscribeCallback(object : SubscribeCallback {
        override suspend fun onSubscribeCompleted(subscribe: Subscribe) {
            println("Completed subscribe to: ${subscribe.topic}")
        }

        override suspend fun onMessageReceived(message: Message) {
            println("Received: $message")
        }

        override suspend fun onUnsubscribeCompleted(unsubscribe: Unsubscribe) {
            TODO("Not yet implemented")
        }

    })

    Unit
}