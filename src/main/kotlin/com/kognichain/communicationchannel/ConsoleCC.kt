package com.kognichain.communicationchannel

import com.kognichain.core.ObservableCommunicationChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Closeable

class ConsoleCC : ObservableCommunicationChannel, Closeable {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    private val messagesFlow = Channel<String>(Channel.UNLIMITED)
    private val reader = BufferedReader(InputStreamReader(System.`in`))
    private var readJob: Job? = null

    init {
        startReading()
    }

    private fun startReading() {
        readJob = scope.launch {
            try {
                while (isActive) {
                    val line = withContext(Dispatchers.IO) {
                        reader.readLine()
                    } ?: break

                    messageChannel.send(line)
                    messagesFlow.send(line)
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                println("Error reading from console: ${e.message}")
            }
        }
    }

    override suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            println("BOT: $message")
            print("YOU: ")
        }
    }

    override suspend fun receiveMessage(): String {
        return messageChannel.receive()
    }

    override suspend fun observeMessages(): Flow<String> = channelFlow {
        try {
            while (true) {
                val message = messagesFlow.receive()
                send(message)
            }
        } catch (e: CancellationException) {
            println("Error observeMessages: ${e.message}")
        }
    }

    override fun close() {
        scope.cancel()
        readJob?.cancel()
        messageChannel.close()
        messagesFlow.close()
        reader.close()
    }

    fun stop() {
        close()
    }
}