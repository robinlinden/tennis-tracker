package com.example.tennistracker.common

data class Session(
    val timestamp: Long = System.currentTimeMillis(),
    val measurements: List<Measurement> = emptyList(),
)
