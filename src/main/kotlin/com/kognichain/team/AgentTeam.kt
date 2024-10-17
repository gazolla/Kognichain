package com.kognichain.team

import com.kognichain.core.Agent
import com.kognichain.core.Memory

class AgentTeam(
    val name: String,
    private val agents: List<Agent>,
    private val sharedMemory: Memory
) {
    fun executeTeamTask(task: CollaborativeTask) {
        task.execute(agents, sharedMemory)
    }
}