package com.kognichain.decisionmakers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kognichain.core.AgentDecision
import com.kognichain.core.CustomDecision
import com.kognichain.core.DecisionMaker
import com.kognichain.core.ExecuteTaskDecision
import com.kognichain.core.LLMClient
import com.kognichain.core.Memory
import com.kognichain.core.RespondToUserDecision
import com.kognichain.core.StopAgentDecision
import com.kognichain.core.Task

class LLMDecisionMaker(
    private val llmClient: LLMClient,
    var memory: Memory? = null,
    var tasks: List<Task> = emptyList()
) : DecisionMaker {

    private val objectMapper = ObjectMapper()

    override suspend fun decide(input: Map<String, Any>): AgentDecision {
        val history = memory?.retrieve("llm_history") as? String ?: ""
        memory?.store("llm_history", "$history\nUser prompt: $input\n")

        val contextDescription = input.entries.joinToString("\n") { (key, value) -> "$key: $value" }
        val unifiedPrompt = buildUnifiedPrompt(input, history, contextDescription)
        val response = llmClient.generateResponse(unifiedPrompt)

        return parseDecision(response, input)
    }

    private fun buildUnifiedPrompt(input: Map<String, Any>, history: String, userContext: String): String {
        val availableTasksDescription = tasks.joinToString(", ") { it::class.simpleName ?: "Unnamed Task" }

        val prompt = """
    Historical context: $history
    
    Based on the following input and the user's question, decide which task to execute and generate a response to the user:
    
    User question:
    $userContext
    
    Available tasks: $availableTasksDescription
    
    Your task is to:
    1. Choose the most appropriate task to execute based on the user's input, or decide to respond to the user without executing a task.
    2. If a task is selected, provide the parameters required for this task (if applicable) in a structured JSON format.
    3. Generate a clear and concise response to the user based on the context.
    
    The response should be formatted as follows:
    {
        \"task\": \"TaskName\",
        \"parameters\": {\"param1\": \"value1\", \"param2\": \"value2\"},
        \"userResponse\": \"Your response to the user\"
    }
    
    If no task is selected and you want to respond to the user, use \"respond\" as the task name.
    """.trimIndent()

        return formatPromptForLLM(prompt)
    }

    private fun formatPromptForLLM(prompt: String): String {
        return """{"contents":[{"parts":[{"text":"$prompt"}],"role":"user"}]}"""
    }


    private fun parseDecision(response: String, input: Map<String, Any>): AgentDecision {
        val cleanResponse = response.replace("```json\n", "").replace("```", "")
        val responseNode: JsonNode = try {
            objectMapper.readTree(cleanResponse)
        } catch (e: Exception) {
            return CustomDecision("Error parsing response: ${e.message}")
        }
        val taskName = extractTaskName(responseNode) ?: ""
        val taskParameters = extractTaskParameters(responseNode).toMutableMap() ?: mutableMapOf()
        val userResponse = extractUserResponse(responseNode) ?: "No response from LLM."
        taskParameters["userResponse"] = userResponse
        return when {
            taskName.equals("stop", ignoreCase = true) -> StopAgentDecision()
            taskName.equals("respond", ignoreCase = true) -> RespondToUserDecision(userResponse)
            else -> {
                val task = tasks.find { it::class.simpleName == taskName }
                if (task != null) {
                    ExecuteTaskDecision(
                        taskName = taskName,
                        parameters = taskParameters
                    )
                } else {
                    CustomDecision("Task not found in response: $response")
                }
            }
        }
    }

    private fun extractTaskName(responseNode: JsonNode): String? {
        return responseNode.path("task").asText(null)
    }
    private fun extractTaskParameters(candidate: JsonNode): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        val parametersNode = candidate.get("parameters")
        if (parametersNode != null && parametersNode.isObject) {
            if (parametersNode.size() > 0) {
                 parametersNode.fieldNames().forEachRemaining { fieldName ->
                    val fieldValue = parametersNode.get(fieldName).asText()
                    parameters[fieldName.trim()] = fieldValue
                }
            }
        } else {
            println("'parameters' não encontrado ou não é um objeto.")
        }

        return parameters
    }

    private fun extractUserResponse(responseNode: JsonNode): String? {
        val userResponseNode = responseNode.fieldNames().asSequence()
            .firstOrNull { it.trim() == "userResponse" }?.let { responseNode[it] }

        return userResponseNode?.asText(null) ?: "No user response found."
    }
}
