package com.kognichain.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface Task {
    fun execute(input: Map<String, Any>): Result<String>
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

interface LLMClient {
    suspend fun generateResponse(prompt: String): String
}

interface CommunicationChannel {
    suspend fun sendMessage(message: String)
    suspend fun receiveMessage(): String
}

interface ObservableCommunicationChannel : CommunicationChannel {
    suspend fun observeMessages(): Flow<String>
}

interface CollaborativeTask {
    fun execute(agents: List<Agent>, sharedMemory: Memory, communicationChannel: CommunicationChannel)
}
