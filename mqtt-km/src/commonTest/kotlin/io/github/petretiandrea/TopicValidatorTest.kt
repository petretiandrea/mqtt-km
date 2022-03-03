package io.github.petretiandrea

import io.github.petretiandrea.mqtt.core.DefaultTopicValidator
import io.github.petretiandrea.mqtt.core.TopicValidator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopicValidatorTest {

    private lateinit var validator: TopicValidator

    @BeforeTest
    fun setup() {
        validator = DefaultTopicValidator()
    }

    @Test
    fun canRecognizeValidSubscribeTopics() {
        val validTopics = listOf(
            "topic/+/",
            "topic/+",
            "topic/#",
            "topic/+/nestedTopic",
            "topic/+/nestedTopic/#"
        )

        validTopics.forEach {
            assertTrue(validator.isValidSubscribeTopic(it))
        }
    }

    @Test
    fun canDetectInvalidSubscribeTopics() {
        val invalidTopics = listOf(
            "topic+",
            "topic/+/nestedTopic+",
            "topic/#/",
            "topic#",
            "topic/+/#/nestedTopic",
            "topic/+/nestedTopic#",
            "#",
            "+"
        )
        invalidTopics.forEach {
            assertFalse(validator.isValidSubscribeTopic(it))
        }
    }

    @Test
    fun canRecognizeValidPublishTopics() {
        val validTopics = listOf("topic/nestedTopic/nestedTopic2", "topic/", "topic")
        validTopics.forEach {
            assertTrue(validator.isValidPublishTopic(it))
        }
    }

    @Test
    fun canDetectInvalidPublishTopics() {
        val invalidTopics = listOf(
            "topic/+",
            "topic/#",
            "topic/nestedTopic/#",
            "topic/+/nestedTopic",
            "topic+",
            "topic#"
        )
        invalidTopics.forEach { assertFalse(validator.isValidPublishTopic(it)) }
    }

}