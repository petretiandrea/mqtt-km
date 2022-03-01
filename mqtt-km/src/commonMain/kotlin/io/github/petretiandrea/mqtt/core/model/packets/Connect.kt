package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.Extension.writeString
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import kotlin.experimental.or

@ExperimentalUnsignedTypes
data class Connect(
    val version: MqttVersion,
    val clientId: String,
    val username: String,
    val password: String,
    val cleanSession: Boolean,
    val keepAliveSeconds: Int,
    val willMessage: Message?
) : MqttPacket {

    private val protocolName: String = if (version == MqttVersion.MQTT_31) "MQIsdp" else "MQTT"
    private val protocolLevel: Int = if (version == MqttVersion.MQTT_31) 3 else 4

    override val qos: QoS = QoS.Q0

    @Suppress("MagicNumber")
    override fun toByteArray(): UByteArray {
        val output = mutableListOf<Byte>()

        // length MSB and LSB
        output += 0.toByte()
        output += if (version == MqttVersion.MQTT_31) 6.toByte() else 4.toByte()

        // protocol name and version
        output += protocolName.encodeToByteArray().toList()
        output += protocolLevel.toByte()

        // connection flags
        val indexConnectionFlags = output.size
        output += 0.toByte()

        // clean session connection flags
        if (cleanSession) {
            output[indexConnectionFlags] = output[indexConnectionFlags] or FLAG_CLEAN_SESSION
        }

        // keep alive msb and lsb
        output += (keepAliveSeconds shr 8).toByte()
        output += (keepAliveSeconds and 0xFF).toByte()

        // mqtt client id
        output.writeString(clientId)

        // topic will and message
        if (willMessage != null) {
            output.writeString(willMessage.topic)
            output.writeString(willMessage.message)
            // will, qos, retain flags
            output[indexConnectionFlags] = output[indexConnectionFlags] or FLAG_WILL
            output[indexConnectionFlags] =
                output[indexConnectionFlags] or ((willMessage.qos.ordinal and 0x3) shl 3).toByte()
            if (willMessage.retain) output[indexConnectionFlags] = output[indexConnectionFlags] or FLAG_WILL_RETAIN
        }

        // username and password
        if (username.isNotEmpty()) {
            output[indexConnectionFlags] = output[indexConnectionFlags] or FLAG_USERNAME
            output.writeString(username)
        }

        if (password.isNotEmpty()) {
            output[indexConnectionFlags] = output[indexConnectionFlags] or FLAG_PASSWORD
            output.writeString(password)
        }

        return FixedHeader(Type.CONNECT, false, qos, false).toByteArray(output.size) + output.toByteArray()
            .toUByteArray()
    }

    companion object : MqttDeserializer {
        private const val FLAG_CLEAN_SESSION: Byte = 2
        private const val FLAG_WILL = (1 shl 2).toByte()
        private const val FLAG_WILL_RETAIN = (1 shl 5).toByte()
        private const val FLAG_PASSWORD = (1 shl 6).toByte()
        private const val FLAG_USERNAME = (1 shl 7).toByte()

        override fun fromByteArray(data: UByteArray): Result<MqttPacket> {
            return Result.failure(NotImplementedError("Not yet implemented"))
        }
    }
}
