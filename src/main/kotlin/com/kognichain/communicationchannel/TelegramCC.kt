package com.kognichain.communicationchannel

import com.kognichain.core.CommunicationChannel
import com.kognichain.core.ObservableCommunicationChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.Properties

class TelegramCC(
    private var botToken: String = "",
    private var botName: String = ""
) : TelegramLongPollingBot(), ObservableCommunicationChannel {

    private val messageChannel = Channel<Pair<Long, String>>(Channel.UNLIMITED)
    private val messagesFlow = Channel<String>(Channel.UNLIMITED)

    override fun getBotToken(): String = botToken
    override fun getBotUsername(): String = botName

    init {
        val properties = loadProperties()
        botToken = properties.getProperty("telegram.bottoken") ?: System.getenv("TELEGRAM_BOT_TOKEN") ?: ""
        botName = properties.getProperty("telegram.botname") ?: System.getenv("TELEGRAM_BOTNAME") ?: ""
        registerBot()
    }

    private fun loadProperties(): Properties {
        val properties = Properties()
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("application.properties")
        inputStream?.use {
            properties.load(it)
        }
        return properties
    }

    private fun registerBot() {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            telegramBotsApi.registerBot(this)
            println("Bot registered successfully.")
        } catch (e: Exception) {
            println("Error registering bot: ${e.message}")
        }
    }

    override fun onUpdateReceived(update: Update) {
        println("recebi uma msg")
        val message: Message? = update.message
        message?.let {
            val receivedMessage = it.text
            val chatId = update.message.chatId
            messageChannel.trySend(chatId to receivedMessage).isSuccess
            messagesFlow.trySend(receivedMessage)
        }
    }

    override suspend fun sendMessage(message: String){
        withContext(Dispatchers.IO) {
            if (messageChannel.isEmpty) {
                println("Message channel is empty. Cannot send message.")
                return@withContext
            }
            try {
                val (chatId, _) = messageChannel.receive()

                val sendMessageRequest = SendMessage().apply {
                    this.chatId = chatId.toString()
                    text = message
                }
                execute(sendMessageRequest)
                println("Message sent successfully to chatId: $chatId")
            } catch (e: TelegramApiException) {
                e.printStackTrace()
                throw e
            } catch (e: IllegalStateException) {
                println("No messages in channel, exiting sendMessage method.")
            }
        }
    }

    override suspend fun receiveMessage(): String {
        val (_, message) = messageChannel.receive()
        return message
    }

    override suspend fun observeMessages(): Flow<String> = channelFlow {
        while (true) {
            val message = messagesFlow.receive()
            send(message)
        }
    }
}