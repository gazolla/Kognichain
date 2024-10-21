package com.kognichain.tasks

import com.kognichain.core.Task
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

class SendEmailTask(
    private val input: Map<String, Any> // Recebe o input no construtor
) : Task {

    // Carrega as propriedades de e-mail de um arquivo properties
    private fun loadEmailProperties(): Properties {
        val properties = Properties()
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("application.properties")
        inputStream?.use {
            properties.load(it)
        }
        return properties
    }

    override fun execute(taskInput: Map<String, Any>): Result<String> {

        val mergedInput = input.toMutableMap().apply { putAll(taskInput) }


        val host = mergedInput["host"] as? String ?: loadEmailProperties().getProperty("mail.smtp.host")
        ?: System.getenv("MAIL_SMTP_HOST") ?: "default-smtp.gmail.com"
        val port = mergedInput["port"] as? Int ?: loadEmailProperties().getProperty("mail.smtp.port")?.toIntOrNull()
        ?: System.getenv("MAIL_SMTP_PORT")?.toIntOrNull() ?: 587
        val username = mergedInput["username"] as? String ?: loadEmailProperties().getProperty("mail.smtp.username")
        ?: System.getenv("MAIL_SMTP_USERNAME") ?: "default@example.com"
        val password = mergedInput["password"] as? String ?: loadEmailProperties().getProperty("mail.smtp.password")
        ?: System.getenv("MAIL_SMTP_PASSWORD") ?: "defaultpassword"

        val recipientEmail = mergedInput["to"] as? String
            ?: return Result.failure(Exception("Recipient email not provided"))
        val sbjct = mergedInput["subject"] as? String
            ?: return Result.failure(Exception("Subject not provided"))
        val messageBody = mergedInput["body"] as? String
            ?: return Result.failure(Exception("Message body not provided"))

        val properties = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(properties, object : jakarta.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                subject = sbjct
                setText(messageBody)
            }
            Transport.send(message)
            Result.success("Email sent successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
