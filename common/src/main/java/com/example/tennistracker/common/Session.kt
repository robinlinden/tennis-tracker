package com.example.tennistracker.common

data class Session(
    val timestamp: Long = System.currentTimeMillis(),
    val accelerometerMeasurements: List<Measurement> = emptyList(),
    val gyroscopeMeasurements: List<Measurement> = emptyList(),
)
