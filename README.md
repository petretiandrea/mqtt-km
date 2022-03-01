
# MQTT-KM (MQTT Kotlin Multiplatform)

A simple MQTT client kotlin multiplatform implementation.

[![GitHub Build](https://github.com/petretiandrea/mqtt-km/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/petretiandrea/mqtt-km/actions/workflows/build.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-1.6.10-orange.svg)](http://kotlinlang.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petretiandrea/mqtt-km.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.petretiandrea.mqtt-km%22)

**WIKI will be available as soon as possible**

## Supported platforms and features

| Platform    |     MQTT 3.1.1     | TCP                |        TLS         |     Websocket      |
| :---:       |:------------------:| :------------:     |:------------------:|:------------------:|
| JVM         | :white_check_mark: | :white_check_mark: | --- | --- |
| Windows X64 | :white_check_mark: | :white_check_mark: | --- | --- |
| Linux X64   | :white_check_mark: | :white_check_mark: | --- | --- |
| Linux ARM64 | :white_check_mark: | :white_check_mark: | --- | --- |
| Linux ARM32 | --- | --- | --- | --- |
| Node.js     | --- | --- | --- | --- |

## Install
The library is available on Maven Central, so you can install with gradle by:
```gradle
implementation("io.github.petretiandrea:mqtt-km:x.x.x") // kotlin dsl
implementation 'io.github.petretiandrea:mqtt-km:x.x.x' // groovy
```

## Example
The mqtt library works by creating a safe event loop using coroutines. 
To build a valid client instance you need to provide a coroutine scope where launch a "background" coroutine event loop.

You can create a simple mqtt client instance by a special dsl provided by library.
```kotlin
fun main() = runBlocking { scope ->
    val client = mqtt(scope) {
        tcp { // also provide ssl, or websocket builder (actually not implemented)
            hostname = "broker.hivemq.com"
            port = 1883
            clientId = "client-1234"
        }

        // you can subscribe a lot of callbacks during creation...
        // onMessageReceive, onDisconnect, onDeliveryCompleted, etc...
        onSubscribeCompleted { subscribe, qoS ->
            println("Subscribe to: ${subscribe.topic} with qos: $qoS")
        }
    }
}

// ...or after
client.onMessageReceived { message -> println("$message")}

// connect returns a Result, which is a success when connection process ends successfully
client.connect()

// publish. This return a boolean, but the effective delivery of message is handled by event loop, so the message
// complete the delivery when onDeliveryCompleted is called.
val message = Message(MessageId.generate(), "topic/subtopic/", "data", QoS.Q1, retain = false, duplicate = false)
client.publish(message)


// disconnect
client.disconnect()
```