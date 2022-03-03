import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.dsl.mqtt
import io.github.petretiandrea.mqtt.dsl.tcp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {

    // create from current coroutine scope or specify it: mqtt(scope) { }
    val client = mqtt {
        tcp {
            hostname = "broker.hivemq.com"
            port = 1883
            clientId = "client-1234"
        }

        onSubscribeCompleted { subscribe, qoS ->
            println("Subscribe to: ${subscribe.topic} with qos: $qoS")
        }
    }

    client.onMessageReceived {
        println("Message received: $it")
    }

    client.onDeliveryCompleted {
        println("Delivery completed of: $it")
    }

    client.connect()

    client.subscribe("sensor/sensor-1234/temperature", QoS.Q1)

    client.publish(
        Message(
            MessageId.generate(), "sensor/sensor-1234/temperature", "45.4", QoS.Q1, retain = false,
            duplicate = false
        )
    )

    client.onDisconnect { println("Client Disconnected") }

    delay(10_000)

    client.disconnect()

    Unit
}