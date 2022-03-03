package io.github.petretiandrea.mqtt.core

interface TopicValidator {
    fun isValidPublishTopic(topic: String): Boolean
    fun isValidSubscribeTopic(topic: String): Boolean
}


internal class DefaultTopicValidator : TopicValidator {

    override fun isValidPublishTopic(topic: String): Boolean =
        topic.encodeToByteArray().size <= MAX_TOPIC_LENGTH && !topic.contains("+") && !topic.contains("#")

    /**
     * A non valid subscribe topic examples:
     * - sport/tennis/# is valid
     * - sport/tennis# is not valid CASE 1
     * - sport/tennis/#/ranking is not valid CASE 2
     * - sport+ is not valid CASE 3
     */
    override fun isValidSubscribeTopic(topic: String): Boolean {
        if (topic.encodeToByteArray().size <= MAX_TOPIC_LENGTH) {
            val wildcards = topic.mapIndexedNotNull { index, c ->
                if (c == '+' || c == '#') Triple(topic.getOrNull(index - 1), c, topic.getOrNull(index + 1)) else null
            }
            // enclosed to / / or / and nothing after
            val validPlus = wildcards.filter { it.second == '+' }.all {
                (it.first == '/' && it.third == '/') || (it.first == '/' && it.third == null)
            }

            val validSharp = wildcards.filter { it.second == '#' }.all {
                it.first == '/' && it.third == null
            }

            return validPlus and validSharp
        }
        return false
    }

    companion object {
        private const val MAX_TOPIC_LENGTH = 65_535
    }

}
