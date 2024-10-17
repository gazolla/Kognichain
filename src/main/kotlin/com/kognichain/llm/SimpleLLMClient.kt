package com.kognichain.llm

import com.kognichain.core.LLMClient
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.regex.Pattern

class SimpleLLMClient() : LLMClient {
    private val httpClient = HttpClient.newBuilder().build()
    private val API_KEY = "<<Adicione sua chave aqui>>"
    private val URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent"

    override suspend fun generateResponse(prompt: String): String {

      //  val fullPrompt = "return the result as plain text without any formatting, special characters, or backticks. Question:$prompt"

        val jsonRequest = """{"contents":[{"parts":[{"text":"$prompt"}],"role":"user"}]}"""

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$URL?alt=sse&key=$API_KEY"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return processResponse(response).toString()
    }

    @Throws(IOException::class)
    private fun processResponse(response: HttpResponse<String>): String? {
        if (response.statusCode() != 200) {
            println("Error: ${response.statusCode()}")
            return null
        }
        val pattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"")
        val answer = StringBuilder()
        BufferedReader(StringReader(response.body())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) continue
                val matcher = pattern.matcher(line.substring(5))
                if (matcher.find()) {
                    answer.append(matcher.group(1)).append(" ")
                }
            }
        }
        return answer.toString().trim()
    }

}