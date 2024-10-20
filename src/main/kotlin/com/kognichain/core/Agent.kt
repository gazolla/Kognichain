package com.kognichain.core

import kotlinx.coroutines.delay
import java.util.UUID

class Agent(
    val id: String = UUID.randomUUID().toString(),  // ID único para o agente
    private val decisionMaker: DecisionMaker,
    private val tasks: List<Task>,
    private val listeners: List<Listener> = emptyList(),
    private val memory: Memory,
    private val llmClient: LLMClient? = null,
    private val communicationChannel: CommunicationChannel? = null,  // Canal de comunicação para interagir com outros agentes
    private val initialPrompt: String? = null
) {
    @Volatile
    private var isRunning: Boolean = true
    private var delayTime: Long = 100 // Valor inicial do delay em milissegundos
    private val maxDelayTime: Long = 5000 // Delay máximo de 5 segundos
    private val minDelayTime: Long = 100 // Delay mínimo de 100 milissegundos

    suspend fun runAgent() {
        // Se houver um prompt inicial, envia para o LLMClient
        initialPrompt?.let {
            val llmResponse = llmClient?.generateResponse(it)
            memory.store("initial_prompt", it)
            memory.store("llm_initial_response", llmResponse ?: "No response from LLM")
        }

        // Loop principal do agente
        while (isRunning) {
            val input: MutableMap<String, Any> = mutableMapOf()

            // 1. Captura dados dos Listeners (se existirem)
            var hasNewData = false
            for (listener in listeners) {
                val listenerData = listener.listen()
                if (listenerData != null && listenerData.isNotEmpty()) {
                    hasNewData = true
                    input.putAll(listenerData)  // listenerData será processado apenas se não for nulo
                }
            }

            // 2. Aumenta o delay exponencialmente se não houver dados novos
            delayTime = if (!hasNewData) {
                (delayTime * 2).coerceAtMost(maxDelayTime) // Delay aumenta até o limite
            } else {
                minDelayTime // Reseta o delay ao valor mínimo se houver novos dados
            }

            // 3. Decisão a ser tomada
            val decision = decisionMaker.decide(input)

            // 4. Executa as tarefas com base na decisão
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
                is StopAgentDecision -> stopAgent()  // Interrompe o agente
                is CustomDecision -> {
                    // Decisão customizada, exemplo de comunicação entre agentes
                    communicationChannel?.send("Decision: ${decision.details} from Agent $id")
                }
                else -> {
                    println("No valid decision was made.")
                }
            }

            // 5. Delay dinâmico baseado na entrada (com backoff controlado)
            delay(delayTime)
        }
    }

    fun stopAgent() {
        isRunning = false
    }

    // Método para receber mensagens de outros agentes
    suspend fun receiveMessage(): Any? {
        return communicationChannel?.receive()
    }
}
