package com.kognichain.decisionmakers

import com.kognichain.core.AgentDecision
import com.kognichain.core.CustomDecision
import com.kognichain.core.DecisionMaker
import com.kognichain.core.ExecuteTaskDecision
import com.kognichain.core.LLMClient
import com.kognichain.core.Memory
import com.kognichain.core.StopAgentDecision
import com.kognichain.core.Task

class LLMDecisionMaker(
    private val llmClient: LLMClient,  // LLM pode ser injetada
    private val memory: Memory,
    private val tasks: List<Task>  // Lista de tasks disponíveis
) : DecisionMaker {

    override suspend fun decide(input: Map<String, Any>): AgentDecision {
        val history = memory.retrieve("llm_history") as? String ?: ""
        memory.store("llm_history", "$history\nUser prompt: $input\n")
        val userPrompt = buildUserPrompt(input, history)
        val userResponse = llmClient.generateResponse(userPrompt)


        val contextDescription = input.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
        val taskPrompt = buildTaskPrompt(input, history, contextDescription)
        val taskResponse = llmClient.generateResponse(taskPrompt)
        return parseDecision(taskResponse, userResponse)
    }

    // Prompt para responder ao usuário
    private fun buildUserPrompt(input: Map<String, Any>, history: String): String {
        val contextDescription = input.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }

        val prompt = """
        Historical context: $history
        
        Based on the following input, generate a response to the user:
        
        Context:
        $contextDescription
        
        Provide a clear and concise answer based on the information given.
        """.trimIndent()

        return formatPromptForLLM(prompt)
    }

    // Prompt para decidir qual task executar
    private fun buildTaskPrompt(input: Map<String, Any>, history: String, userQuestion: String): String {
        val availableTasksDescription = tasks.joinToString(", ") { it::class.simpleName ?: "Unnamed Task" }

        val prompt = """
        Historical context: $history
        
        Based on the following input and the user's question, decide which task to execute:
        
        User question:
        $userQuestion
        
        Available tasks: $availableTasksDescription
        
        Your task is to analyze the user response and choose the most appropriate task to execute, 
        or decide to stop the agent.
        """.trimIndent()

        return formatPromptForLLM(prompt)
    }

    private fun formatPromptForLLM(prompt: String): String {
        return """{"contents":[{"parts":[{"text":"$prompt"}],"role":"user"}]}"""
    }

    private fun parseDecision(taskResponse: String, userResponse: String): AgentDecision {
        if (taskResponse.contains("stop", ignoreCase = true)) {
            return StopAgentDecision()
        }

        val taskName = tasks.find { taskResponse.contains(it::class.simpleName ?: "", ignoreCase = true) }

        return taskName?.let {
            ExecuteTaskDecision(
                taskName = it::class.simpleName ?: "Unnamed Task",
                parameters = mapOf("response" to userResponse)
            )
        } ?: run {
            CustomDecision("Task not found in response: $taskResponse")
        }
    }

}
