package io.github.petretiandrea

import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.Util
import io.github.petretiandrea.mqtt.core.model.packets.*
import platform.windows.byte
import kotlin.experimental.and
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class PacketParserOfflineTest {

    @Test
    fun canSerializeDeserializeFixedHeader(){
        val header = FixedHeader(Type.CONNECT, false, QoS.Q0, false)
        val bytes = header.toByteArray(0)
        assert(bytes.isNotEmpty())
        val parsedHeader = FixedHeader.fromByte(bytes[0].toByte()).getOrNull()
        assertEquals(header, parsedHeader)
    }

    @Test
    fun canSerializeDeserializeConnAck() {
        val connAck = ConnAck(
            sessionPresent = true,
            connectionStatus = ConnectionStatus.ACCEPT
        )
        val bytes = connAck.toByteArray().let { it.copyOfRange(2, it.size) }
        assert(bytes.isNotEmpty())
        val parsedAck = ConnAck.fromByteArray(bytes).getOrNull()
        assertEquals(connAck, parsedAck)
    }

    @Test
    fun canSerializeDeserializeConnect() {
        val connect = Connect(
            version = MqttVersion.MQTT_31,
            clientId = "ciaoo",
            username = "pedro",
            password = "pedro",
            cleanSession = false,
            keepAliveSeconds = 2,
            willMessage = null
        )
        assert(connect.toByteArray().isNotEmpty())
    }

    @Test
    fun canSerializeDeserializePubComp() {
        val ack = PubComp(1234)
        val dataBytes = ack.toByteArray().let { it.copyOfRange(2, it.size) }
        assertEquals(ack, PubComp.fromByteArray(dataBytes).getOrNull())
    }

    @Test
    fun canSerializeDeserializePubRec() {
        val ack = PubRec(1234)
        val dataBytes = ack.toByteArray().let { it.copyOfRange(2, it.size) }
        assertEquals(ack, PubRec.fromByteArray(dataBytes).getOrNull())
    }

    @Test
    fun canSerializeDeserializePubRel() {
        val ack = PubRel(1234)
        val dataBytes = ack.toByteArray().let { it.copyOfRange(2, it.size) }
        assertEquals(ack, PubRel.fromByteArray(dataBytes).getOrNull())
    }

    @Test
    fun canSerializeDeserializeUnsubAck() {
        val ack = UnsubAck(1234)
        val dataBytes = ack.toByteArray().let { it.copyOfRange(2, it.size) }
        assertEquals(ack, UnsubAck.fromByteArray(dataBytes).getOrNull())
    }
}