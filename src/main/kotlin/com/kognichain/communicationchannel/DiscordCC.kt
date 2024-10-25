package com.kognichain.communicationchannel

import com.kognichain.core.ObservableCommunicationChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.Properties

class DiscordCC(
    private var token: String = "",
    private var channelId: String = ""
) : ObservableCommunicationChannel {

    private val messageChannel = Channel<Pair<String, String>>(Channel.UNLIMITED)
    private val messagesFlow = Channel<String>(Channel.UNLIMITED)
    private lateinit var jda: JDA

    init {
        val properties = loadProperties()
        token = properties.getProperty("discord.token") ?: System.getenv("DISCORD_TOKEN") ?: ""
        channelId = properties.getProperty("discord.channelId") ?: System.getenv("DISCORD_CHANNEL_ID") ?: ""
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
        val builder = JDABuilder.createDefault(token)
        builder.addEventListeners(object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                val message = event.message
                val channel = event.channel
                if (channel.id == channelId) {
                    messageChannel.trySend(Pair(channel.id, message.contentRaw)).isSuccess
                    messagesFlow.trySend(message.contentRaw)
                }
            }
        })
        jda = builder.build()
        println("Bot registered successfully.")
    }

    override suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            if (!messageChannel.isEmpty) {
                try {
                    val (channelId, _) = messageChannel.receive()
                    val channel = jda.getTextChannelById(channelId)
                    channel?.sendMessage(message)?.queue()
                    println("Message sent successfully to channel: $channelId")
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
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