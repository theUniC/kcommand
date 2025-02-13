package io.github.theunic.kcommand.core

import kotlinx.serialization.Serializable

@Serializable
data class Envelope(
    val type: String,
    val payload: String,
)
