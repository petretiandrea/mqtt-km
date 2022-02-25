package io.github.petretiandrea.mqtt.core.model

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@ExperimentalUnsignedTypes
interface MqttDeserializer {
    fun fromByteArray(data: UByteArray): Result<MqttPacket>
}