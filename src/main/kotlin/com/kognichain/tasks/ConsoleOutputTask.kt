package com.kognichain.tasks

import com.kognichain.core.Task

class ConsoleOutputTask : Task {
    override fun execute(input: Map<String, Any>): Result<String> {
        return try {
            val response = input["response"] ?: "No response available."
            println("LLM: $response")
            Result.success("Output successfully printed to console.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
