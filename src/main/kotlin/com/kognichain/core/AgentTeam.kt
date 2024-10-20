package com.kognichain.core

class AgentTeam(
    val name: String,
    private val agents: List<Agent>,
    private val sharedMemory: Memory,
    private val communicationChannel: CommunicationChannel
) {
    suspend fun executeTeamTask(task: CollaborativeTask) {
        task.execute(agents, sharedMemory, communicationChannel)
    }
}