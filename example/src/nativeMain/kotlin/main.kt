import io.github.petretiandrea.mqtt.core.model.Message
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
            topic = "sensor/sensor-1234/temperature", message = "45.4", qos = QoS.Q1
        )
    )

    client.publish("sensor/sensor-1234/temperature", "47.6") // default qos0

    client.onDisconnect { println("Client Disconnected") }

    delay(10_000)

    client.disconnect()

    Unit
}