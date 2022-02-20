package io.github.petretiandrea.mqtt.core.model

import kotlin.experimental.and


object Extension {
    fun MutableList<Byte>.writeString(string: String) {
        add((string.length shr 8).toByte())
        add((string.length and 0xFF).toByte())
        // UTF-8 encoding
        string.encodeToByteArray().forEach { add(it) }
    }
}

object Util {
    /**
     * Get int number for 2 byte Most Significant e Less Significant Bytes.
     */
    fun getIntFromMSBLSB(msb: Byte, lsb: Byte): Int {
        //  return ((msb & 0xFF) << 8) | (lsb & 0xFF);
        return (((msb.toUInt() and 255u) shl 8) or (lsb.toUInt() and 255u)).toInt()
    }
}