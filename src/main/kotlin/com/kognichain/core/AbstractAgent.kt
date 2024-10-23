package com.kognichain.core

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

    abstract suspend fun setup()
    abstract suspend fun loop()
    suspend fun runAgent() {
        setup()
        loop()
    }
    abstract suspend fun stopAgent()
}