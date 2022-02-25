package io.github.petretiandrea

import io.github.petretiandrea.mqtt.dsl.mqtt
import io.github.petretiandrea.mqtt.dsl.tcp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MqttDslTest {

    @Test
    fun hostnamePortClientIdMustBeMandatory() {
        assertFailsWith<IllegalArgumentException> {
            mqtt(CoroutineScope(Dispatchers.Main)) {
                tcp {
                    hostname = null
                    port = null
                    clientId = null
                }
            }
        }
    }

    @Test
    fun dslMustCreateValidClient() {
        runBlocking {
            val client = mqtt(this) {
                tcp {
                    hostname = "broker.hivemq.com"
                    port = 1883
                    clientId = "test-dsl"
                }
            }
            assertTrue { client.connect().isSuccess }
            assertTrue { client.disconnect().isSuccess }
        }
    }
}