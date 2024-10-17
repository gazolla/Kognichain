package com.kognichain.sensors

import com.kognichain.core.Sensor

class SimpleSensor(private val key: String, private var value: Any) : Sensor {
    override fun sense(): Map<String, Any> = mapOf(key to value)

    fun setValue(newValue: Any) {
        value = newValue
    }
}
