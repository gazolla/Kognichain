package com.kognichain.core

import com.kognichain.decisionmakers.LLMDecisionMaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class Agent(
    id: String  = UUID.randomUUID().toString(),
    decisionMaker: LLMDecisionMaker,
    tasks: List<Task>,
    listeners: List<Listener> = emptyList(),
    memory: Memory,
    llmClient: LLMClient? = null,
    communicationChannel: ObservableCommunicationChannel? = null,
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
    private var observeMessagesJob: Job? = null

    override suspend fun setup(){
        decisionMaker.memory = memory
        decisionMaker.tasks = tasks

        initialPrompt?.let {
            val decision = decisionMaker.decide( mapOf("message" to it))
            processDecision(decision)
        }

        (communicationChannel as? ObservableCommunicationChannel)?.let { channel ->
            coroutineScope {
                observeMessagesJob = launch {
                    channel.observeMessages().collect { message ->
                        val input: MutableMap<String, Any> = mutableMapOf("message" to message)
                        val decision = decisionMaker.decide(input)
                        processDecision(decision)
                    }
                }
            }
        }
    }

    override suspend fun loop () {
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

            if (hasNewData) {
                val decision = decisionMaker.decide(input)
                processDecision(decision)
            }
        }
    }

    private suspend fun processDecision(decision: AgentDecision) {
        when (decision) {
            is ExecuteTaskDecision -> {
                tasks.forEach { task ->
                    val taskResult = task.execute(decision.parameters ?: emptyMap())
                    decision.parameters?.let{ map ->
                        if (map.contains("userResponse")){
                            communicationChannel ?: return
                            communicationChannel.sendMessage(map["userResponse"].toString())
                        }
                    }
                    if (taskResult.isSuccess) {
                        taskResult.getOrNull()?.let { result ->
                            memory.store("task_result", result)
                        }
                    }
                }
            }
            is RespondToUserDecision -> {
                communicationChannel ?: return
                communicationChannel.sendMessage(decision.userResponse)
            }
            is StopAgentDecision -> stopAgent()
            is CustomDecision -> {
                communicationChannel ?: return
                communicationChannel.sendMessage("Decision: ${decision.details} from Agent $id")

            }

            else -> {
                println("No valid decision was made.")
            }
        }
    }

    override suspend fun stopAgent() {
        observeMessagesJob?.cancel()
        isRunning = false
    }
}