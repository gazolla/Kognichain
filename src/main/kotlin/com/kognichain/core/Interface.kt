package com.kognichain.core

import kotlinx.coroutines.Deferred

interface Task {
    fun execute(input: Map<String, Any>): Result<String>  // Exemplo com Result<String>
}

interface Listener {
    suspend fun listen(): Map<String, Any>?
}

interface DecisionMaker {
    suspend fun decide(input: Map<String, Any>): AgentDecision
}

interface Memory {
    suspend fun store(key: String, value: Any)
    suspend fun retrieve(key: String): Any?
    suspend fun clear()
}

interface Action {
    suspend fun execute(): Result<Any>
    suspend fun cancel(): Boolean
}

interface LLMClient {
    suspend fun generateResponse(prompt: String): String
}

interface CommunicationChannel {
    suspend fun send(message: Any)
    suspend fun receive(): Any
}

interface CollaborativeTask {
    fun execute(agents: List<Agent>, sharedMemory: Memory, communicationChannel: CommunicationChannel)
}