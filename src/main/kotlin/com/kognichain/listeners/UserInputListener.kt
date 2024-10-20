package com.kognichain.listeners

import com.kognichain.core.Listener
import java.util.Scanner

class UserInputListener : Listener {
    private val scanner = Scanner(System.`in`)

    override suspend fun listen(): Map<String, Any>?{
        print("You: ")
        val input = scanner.nextLine()

        return if (input.equals("exit", ignoreCase = true)) {
            null
        } else {
            mapOf("userInput" to input)
        }
    }
}
