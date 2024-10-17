package com.kognichain.core

data class Task(
    val name: String,
    val sensors: List<Sensor>,
    val actuators: List<Actuator>
)