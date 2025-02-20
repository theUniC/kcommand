package io.github.theunic.kcommand.transport.kafka

import kotlinx.serialization.Serializable

@Serializable
data class TestMessage(val content: String)
