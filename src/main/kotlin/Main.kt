package com.kognichain.examples

import com.kognichain.core.Agent
import com.kognichain.decisionmakers.LLMDecisionMaker
import com.kognichain.llm.Gemini
import com.kognichain.listeners.UserInputListener
import com.kognichain.tasks.ConsoleOutputTask
import com.kognichain.memories.SimpleMemory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking{
    val memory = SimpleMemory()
    val tasks = listOf(ConsoleOutputTask())
    val decisionMaker = LLMDecisionMaker(
        llmClient = Gemini(),  // Podemos usar Gemini como LLM.
        memory = memory,
        tasks = tasks
    )

    val listeners = listOf(UserInputListener())
    val initialPrompt = "You are a helpful assistant. Respond to the user's questions clearly."
   val agent = Agent(
        listeners = listeners,
        decisionMaker = decisionMaker,
        tasks = tasks,
        memory = memory,
        initialPrompt = initialPrompt
    )
    agent.runAgent()
}
