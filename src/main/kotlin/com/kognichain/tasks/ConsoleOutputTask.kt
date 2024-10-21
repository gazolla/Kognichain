package com.kognichain.tasks

import com.kognichain.core.Task

class ConsoleOutputTask : Task {
    override fun execute(input: Map<String, Any>): Result<String> {
        return try {
            val response = input["userResponse"] ?: "No response available."
            println("LLM: $response")
            Result.success("Output successfully printed to console.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
