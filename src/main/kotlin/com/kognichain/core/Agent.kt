package com.kognichain.core

import com.kognichain.decisionmakers.LLMDecisionMaker
import com.kognichain.examples.com.kognichain.core.AbstractAgent
import kotlinx.coroutines.delay
import java.util.UUID

class Agent(
    id: String  = UUID.randomUUID().toString(),
    decisionMaker: LLMDecisionMaker,
    tasks: List<Task>,
    listeners: List<Listener> = emptyList(),
    memory: Memory,
    llmClient: LLMClient? = null,
    communicationChannel: CommunicationChannel? = null,
    initialPrompt: String? = null
) : AbstractAgent<LLMDecisionMaker>(
    id = id,
    decisionMaker = decisionMaker,
    tasks = tasks,
    listeners = listeners,
    memory = memory,
    llmClient = llmClient,
    communicationChannel = communicationChannel,
    initialPrompt = initialPrompt
) {


    override fun setup(){
        decisionMaker.memory = memory
        decisionMaker.tasks = tasks
    }

    override suspend fun loop () {

        initialPrompt?.let {
            val llmResponse = llmClient?.generateResponse(it)
            memory.store("initial_prompt", it)
            memory.store("llm_initial_response", llmResponse ?: "No response from LLM")
        }


        while (isRunning) {
            val input: MutableMap<String, Any> = mutableMapOf()


            var hasNewData = false
            for (listener in listeners) {
                val listenerData = listener.listen()
                if (listenerData != null && listenerData.isNotEmpty()) {
                    hasNewData = true
                    input.putAll(listenerData)
                }
            }


            delayTime = if (!hasNewData) {
                (delayTime * 2).coerceAtMost(maxDelayTime)
            } else {
                minDelayTime
            }


            val decision = decisionMaker.decide(input)


            when (decision) {
                is ExecuteTaskDecision -> {
                    tasks.forEach { task ->
                        val taskResult = task.execute(decision.parameters ?: emptyMap())
                        if (taskResult.isSuccess) {
                            taskResult.getOrNull()?.let { result ->
                                memory.store("task_result", result)
                            }
                        }
                    }
                }
                is StopAgentDecision -> stopAgent()
                is CustomDecision -> {

                    communicationChannel?.send("Decision: ${decision.details} from Agent $id")
                }
                else -> {
                    println("No valid decision was made.")
                }
            }


            delay(delayTime)
        }
    }

    // Método para receber mensagens de outros agentes
    suspend fun receiveMessage(): Any? {
        return communicationChannel?.receive()
    }
}
