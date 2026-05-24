package com.example.tennistracker.common

data class Measurement(
    val x: Float,
    val y: Float,
    val z: Float,
    // UTC timestamp of when the measurement was captured, in milliseconds.
    val timestamp: Long,
)
