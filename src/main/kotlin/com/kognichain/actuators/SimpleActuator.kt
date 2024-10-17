package com.kognichain.actuators

import com.kognichain.core.Actuator

class SimpleActuator(private val action: (Map<String, Any>) -> Any) : Actuator {
    override fun execute(parameters: Map<String, Any>): Result<Any> {
        return try {
            Result.success(action(parameters))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}