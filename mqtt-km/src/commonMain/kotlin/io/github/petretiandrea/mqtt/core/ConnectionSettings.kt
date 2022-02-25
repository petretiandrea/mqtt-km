package io.github.petretiandrea.mqtt.core

import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.Connect

enum class Protocol {
    TCP,
    SSL,
    WS
}

data class ConnectionSettings(
    val hostname: String,
    val port: Int,
    val clientId: String,
    val username: String?,
    val password: String?,
    val cleanSession: Boolean,
    val willMessage: Message?,
    val keepAliveSeconds: Int,
    val protocol: Protocol = Protocol.TCP,
)

@ExperimentalUnsignedTypes
internal fun ConnectionSettings.asConnectPacket(): Connect = Connect(
    version = MqttVersion.MQTT_311,
    clientId = clientId,
    username = username.orEmpty(),
    password = password.orEmpty(),
    cleanSession = cleanSession,
    keepAliveSeconds = keepAliveSeconds,
    willMessage = willMessage
)

