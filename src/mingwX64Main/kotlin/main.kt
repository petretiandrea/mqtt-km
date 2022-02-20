import io.github.petretiandrea.mqtt.core.transport.BufferedPacketReader
import io.github.petretiandrea.mqtt.core.transport.DefaultBufferedPacketReader
import io.github.petretiandrea.socket.buffer.allocMultiplatformBuffer
import io.github.petretiandrea.socket.createSocket
import io.github.petretiandrea.socket.stream.BufferedInputStream.Companion.buffered
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun main() = runBlocking {
    println("Experimenteal mm: ${isExperimentalMM()}")


    val socket = createSocket(
        "192.168.1.48",
        2000
    )

    val bufferedReader = socket.inputStream().buffered()
    val mqttReader = DefaultBufferedPacketReader(bufferedReader)

    try {
        val readJob = launch {
            while(isActive) {
                val packet = mqttReader.next()
                println("Received packet: $packet")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        socket.close()
    }

    Unit
}