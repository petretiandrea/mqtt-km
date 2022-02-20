package io.github.petretiandrea

import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.packets.Connect
import io.github.petretiandrea.mqtt.core.transport.Transport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@ExperimentalUnsignedTypes
class MqttTest {

    @Test
    fun connectToServer() = runBlocking {
        val transport = Transport.tcp()
        val connected = transport.connect("3.126.191.185", 1883)
        assert(connected.isSuccess)

        val connect = Connect(
            version = MqttVersion.MQTT_31,
            clientId = "ciao",
            "",
            "",
            true,
            10,
            null
        )
        val wrote = transport.writePacket(connect)
        assert(wrote.isSuccess)
        val ack = transport.readPacket()
        println("Ack: $ack")
    }
}