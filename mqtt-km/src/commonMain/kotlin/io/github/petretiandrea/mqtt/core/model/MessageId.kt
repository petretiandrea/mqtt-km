package io.github.petretiandrea.mqtt.core.model

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object MessageId {
    private var lastGenerated = 0
    fun generate(): Int {
        lastGenerated = if (lastGenerated >= 65_534) 1 else lastGenerated + 1
        return lastGenerated
    }
}
