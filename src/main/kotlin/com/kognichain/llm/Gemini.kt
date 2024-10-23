package com.kognichain.llm

import com.kognichain.core.LLMClient
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties

class Gemini(
    private var input: Map<String, Any>? = mutableMapOf<String, Any>()
) : LLMClient {
    private val httpClient = HttpClient.newBuilder().build()
    private var apiKey:String?
    private val url:String?

    private fun loadProperties(): Properties {
        val properties = Properties()
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("application.properties")
        inputStream?.use {
            properties.load(it)
        }
        return properties
    }

    init {
        val p = loadProperties()
        apiKey = p.getProperty("gemini.apikey") ?: System.getenv("GEMINI_API_KEY") ?: ""
        url = p.getProperty("gemini.url") ?: System.getenv("GEMINI_URL") ?: ""
    }

    private val objectMapper = ObjectMapper()

    override suspend fun generateResponse(prompt: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$url?alt=sse&key=$apiKey"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(prompt))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return processResponse(response) ?: "Error processing response"
    }

    @Throws(IOException::class)
    private fun processResponse(response: HttpResponse<String>): String? {
        if (response.statusCode() != 200) {
            handleErrorResponse(response)
            return null
        }
        val combinedResponse = StringBuilder()
        try {
            val lines = response.body().lines()
            for (line in lines) {
                if (line.isBlank()) continue

                if (line.startsWith("data: ")) {
                    val jsonLine = line.removePrefix("data: ")
                    val rootNode = objectMapper.readTree(jsonLine)
                    val candidatesNode: JsonNode? = rootNode.get("candidates")
                    candidatesNode?.forEach { candidateNode ->
                        val contentNode = candidateNode.get("content")
                        contentNode?.get("parts")?.forEach { partNode ->
                            val textNode = partNode.get("text")
                            if (textNode != null) {
                                combinedResponse.append(textNode.asText()).append(" ")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error parsing the response: ${e.message}")
            return null
        }

        return combinedResponse.toString().trim()
    }


    private fun handleErrorResponse(response: HttpResponse<String>) {
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
    }
}
