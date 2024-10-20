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

class Gemini() : LLMClient {
    private val httpClient = HttpClient.newBuilder().build()
    private val API_KEY = ""
    private val URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent"

    override suspend fun generateResponse(prompt: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$URL?alt=sse&key=$API_KEY"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(prompt))
            .build()
        val response =  httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return processResponse(response).toString()
    }

    @Throws(IOException::class)
    private fun processResponse(response: HttpResponse<String>): String? {
        if (response.statusCode() != 200) {
            val statusCode = response.statusCode()
            val reasonPhrase = when (statusCode) {
                400 -> "Bad Request: The server could not understand the request."
                401 -> "Unauthorized: Access is denied due to invalid credentials."
                403 -> "Forbidden: The server understood the request, but it refuses to authorize it."
                404 -> "Not Found: The requested resource could not be found."
                500 -> "Internal Server Error: The server encountered an error."
                else -> "Unexpected Error"
            }
            val responseBody = response.body() ?: "No response body available."
            val headers = response.headers().map().toString()

            println("""
            Error occurred:
            Status Code: $statusCode
            Reason: $reasonPhrase
            Response Body: $responseBody
            Headers: $headers
        """.trimIndent())

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