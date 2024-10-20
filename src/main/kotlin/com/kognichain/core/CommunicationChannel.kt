package com.kognichain.core

import java.util.concurrent.ConcurrentLinkedQueue

class CommunicationChannelImp {
    private val messages = ConcurrentLinkedQueue<Message>()

    fun send(message: Message) {
        messages.add(message)
    }

    fun receive(): Message? = messages.poll()

    fun isEmpty(): Boolean = messages.isEmpty()
}

data class Message(val from: String, val to: String, val content: Any)
