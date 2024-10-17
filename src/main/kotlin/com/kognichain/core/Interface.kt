package com.kognichain.core

interface Actuator {
    fun execute(parameters: Map<String, Any>): Result<Any>
}

interface Sensor {
    fun sense(): Map<String, Any>
}

interface DecisionMaker {
    fun decide(input: Map<String, Any>): Action
}

interface Memory {
    fun store(key: String, value: Any)
    fun retrieve(key: String): Any?
    fun clear()
}

interface Action {
    fun execute(): Result<Any>
    fun cancel(): Boolean
}

interface LLMClient {
    suspend fun generateResponse(prompt: String): String
}