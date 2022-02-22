package io.github.petretiandrea.mqtt.core

import io.github.petretiandrea.mqtt.core.model.Message

data class ConnectionSettings(
    val hostname: String,
    val port: Int,
    val clientId: String,
    val username: String?,
    val password: String?,
    val cleanSession: Boolean,
    val willMessage: Message?,
    val keepAliveSeconds: Int
)

