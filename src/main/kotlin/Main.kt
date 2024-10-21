package com.kognichain.examples

import com.kognichain.core.Agent
import com.kognichain.decisionmakers.LLMDecisionMaker
import com.kognichain.llm.Gemini
import com.kognichain.listeners.UserInputListener
import com.kognichain.tasks.ConsoleOutputTask
import com.kognichain.memories.SimpleMemory
import com.kognichain.tasks.SendEmailTask
import kotlinx.coroutines.runBlocking

fun main() = runBlocking{

    val emailTask = SendEmailTask(
        input = mapOf(
            "host" to "smtp.gmail.com",
            "port" to 587,
            "username" to "",
            "password" to ""
        )
    )

   val agent = Agent(
        listeners = listOf(UserInputListener()),
        decisionMaker = LLMDecisionMaker(llmClient = Gemini()),
        tasks = listOf(ConsoleOutputTask(),emailTask),
        memory = SimpleMemory(),
        initialPrompt = "You are a helpful assistant. Respond to the user's questions clearly."
    )
    agent.runAgent()
}
