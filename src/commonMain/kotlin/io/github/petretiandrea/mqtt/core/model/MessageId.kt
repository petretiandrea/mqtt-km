package io.github.petretiandrea.mqtt.core.model

@ThreadLocal
object MessageId {
    private var lastGenerated = 0
    fun generate(): Int {
        lastGenerated = if (lastGenerated >= 65534) 1 else lastGenerated + 1
        return lastGenerated
    }
}
