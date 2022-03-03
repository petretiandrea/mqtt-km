package io.github.petretiandrea.mqtt.dsl

import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.core.Protocol
import kotlinx.coroutines.CoroutineScope

@DslMarker
annotation class MqttClientDsl

@MqttClientDsl
fun CoroutineScope.mqtt(bloc: MqttClientBuilder.() -> Unit): MqttClient {
    return mqtt(this, bloc)
}

@MqttClientDsl
fun mqtt(scope: CoroutineScope, bloc: MqttClientBuilder.() -> Unit): MqttClient {
    return MqttClientBuilder(scope).apply(bloc).buildClient()
}

@MqttClientDsl
fun MqttClientBuilder.tcp(bloc: MqttClientSettingsBuilder.() -> Unit) {
    settings =
        MqttClientSettingsBuilder(hostname = null, clientId = null, protocol = Protocol.TCP).also(bloc)
}

@MqttClientDsl
fun MqttClientBuilder.ssl(bloc: MqttClientSettingsBuilder.() -> Unit) {
    settings =
        MqttClientSettingsBuilder(hostname = null, clientId = null, protocol = Protocol.SSL).also(bloc)
}

@MqttClientDsl
fun MqttClientBuilder.websocket(bloc: MqttClientSettingsBuilder.() -> Unit) {
    settings =
        MqttClientSettingsBuilder(hostname = null, clientId = null, protocol = Protocol.SSL).also(bloc)
}