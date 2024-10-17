package com.kognichain.team

import com.kognichain.core.Agent
import com.kognichain.core.Memory

interface CollaborativeTask {
    fun execute(agents: List<Agent>, sharedMemory: Memory)
}