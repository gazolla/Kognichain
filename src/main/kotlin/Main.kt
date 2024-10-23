package com.kognichain.examples

import com.kognichain.communicationchannel.ConsoleCC
import com.kognichain.communicationchannel.TelegramCC
import com.kognichain.core.Agent
import com.kognichain.decisionmakers.LLMDecisionMaker
import com.kognichain.llm.Gemini
import com.kognichain.memories.SimpleMemory
import com.kognichain.tasks.SendEmailTask
import kotlinx.coroutines.runBlocking

fun main() = runBlocking{

    val agent = Agent(
        decisionMaker = LLMDecisionMaker(llmClient = Gemini()),
        tasks = listOf(SendEmailTask()),
        memory = SimpleMemory(),
        communicationChannel = ConsoleCC(),
        initialPrompt = "You are a helpful assistant. Respond to the user's questions via Telegram."
    )

    agent.runAgent()


}
