import io.github.petretiandrea.mqtt.MqttClientImpl
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.transport.Transport
import kotlinx.coroutines.runBlocking

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

    mqtt.onMessageReceived {
        println("Received: $it")

    }
    mqtt.onSubscribeCompleted { subscription, _ ->
        println("Completed subscribe to: ${subscription.topic}")
    }

    val published = mqtt.publish(Message(1, "test/kotlin", "hello2", QoS.Q2, retain = false, duplicate = false))
    println(published)


    println(mqtt.subscribe("test/kotlin", QoS.Q2))


    Unit
}