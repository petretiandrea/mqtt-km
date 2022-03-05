package io.github.petretiandrea

import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes
class PacketParserTest {

    @Test
    fun canSerializeDeserializeFixedHeader() {
        val header = FixedHeader(Type.CONNECT, false, QoS.Q0, false)
        val bytes = header.toByteArray(0)
        assertTrue(bytes.isNotEmpty())
        val parsedHeader = FixedHeader.fromByte(bytes[0].toUByte()).getOrNull()
        assertEquals(header, parsedHeader)
    }

    @Test
    fun canSerializeDeserializeConnAck() {
        val connAck = ConnAck(
            sessionPresent = true,
            connectionStatus = ConnectionStatus.ACCEPT
        )
        val bytes = connAck.toByteArray().let { it.copyOfRange(2, it.size) }
        assertTrue(bytes.isNotEmpty())
        val parsedAck = ConnAck.fromByteArray(bytes).getOrNull()
        assertEquals(connAck, parsedAck)
    }

    @Test
    fun canSerializeDeserializeConnect() {
        val connect = Connect(
            version = MqttVersion.MQTT_31,
            clientId = generateRandomString(5),
            username = generateRandomString(6),
            password = generateRandomString(6),
            cleanSession = false,
            keepAliveSeconds = 2,
            willMessage = null
        )
        assertTrue(connect.toByteArray().isNotEmpty())
    }

    @Test
    fun canSerializeDeserializeMqttAcks() {
        val acks = listOf(
            PubComp(MessageId.generate()),
            PubRel(MessageId.generate()),
            PubRec(MessageId.generate()),
            UnsubAck(MessageId.generate()),
            PubAck(MessageId.generate()),
        ) + QoS.values().zip(listOf(true, false)).map {
            SubAck(MessageId.generate(), it.first, it.second)
        } + ConnectionStatus.values().zip(listOf(true, false)).map {
            ConnAck(it.second, it.first)
        }

        acks.forEach {
            assertEquals(it, MqttTestUtil.serializeAndParse(it))
        }
    }

    @Test
    fun canSerializeDeserializePublish() {
        val publish = Publish(Message("topic", "message", qos = QoS.Q1, retain = false, duplicate = false))
        val publishByte = publish.toByteArray().filterIndexed { index, _ ->  index != 1 }.toUByteArray()
        assertEquals(publish, Publish.fromByteArray(publishByte).getOrNull())
    }

    @Test
    fun canSerializeDeserializeBigPublishPacket() {
        val message = generateRandomString(127)
        val publish =
            Publish(Message(generateRandomString(5), message, qos = QoS.Q1, retain = false, duplicate = false))

        // filter remove remaining length bytes
        val publishByte = publish.toByteArray().filterIndexed { index, _ ->  index != 1 && index != 2 }.toUByteArray()
        assertEquals(publish, Publish.fromByteArray(publishByte).getOrNull())
    }

    @Test
    fun canProduceValidPingPackets() {
        val respByte = PingResp.toByteArray().first()
        val reqByte = PingReq.toByteArray().first()

        assertEquals(Type.PINGRESP, FixedHeader.fromByte(respByte).getOrThrow().type)
        assertEquals(Type.PINGREQ, FixedHeader.fromByte(reqByte).getOrThrow().type)
    }
}

object MqttTestUtil {
    fun serializeAndParse(packet: MqttPacket): MqttPacket? {
        val (header, body) = packet.toByteArray().let { it.take(1).first() to it.drop(2).toUByteArray() }
        return MqttPacket.parse(header, body)
    }
}