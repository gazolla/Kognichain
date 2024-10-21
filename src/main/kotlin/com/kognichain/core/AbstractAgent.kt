package com.kognichain.examples.com.kognichain.core

import com.kognichain.core.CommunicationChannel
import com.kognichain.core.DecisionMaker
import com.kognichain.core.LLMClient
import com.kognichain.core.Listener
import com.kognichain.core.Memory
import com.kognichain.core.Task
import java.util.UUID

abstract class AbstractAgent<T : DecisionMaker>(
    val id: String = UUID.randomUUID().toString(),
    protected  val decisionMaker: T,
    protected  val tasks: List<Task>,
    protected  val listeners: List<Listener> = emptyList(),
    protected  val memory: Memory,
    protected  val llmClient: LLMClient? = null,
    protected  val communicationChannel: CommunicationChannel? = null,
    protected  val initialPrompt: String? = null
) {
    @Volatile
    protected var isRunning: Boolean = true
    protected var delayTime: Long = 100
    protected val maxDelayTime: Long = 5000
    protected val minDelayTime: Long = 100

    abstract fun setup()
    abstract suspend fun loop()
    suspend fun runAgent() {
        setup()
        loop()
    }
    fun stopAgent() {
        isRunning = false
    }
}