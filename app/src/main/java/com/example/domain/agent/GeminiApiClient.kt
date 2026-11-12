package com.example.domain.agent

import com.example.data.model.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface GeminiResponse {
    data class Success(val text: String, val toolCall: ToolCall?) : GeminiResponse
    data class RateLimitOrBusy(val message: String, val statusCode: Int) : GeminiResponse
    data class FatalError(val message: String, val statusCode: Int) : GeminiResponse
}

data class ChatHistoryItem(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiApiClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        apiKey: String,
        model: String,
        customBaseUrl: String,
        systemInstruction: String,
        history: List<ChatHistoryItem>
    ): GeminiResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResponse.FatalError(
                "API Key is missing. Please enter your Gemini API key in Settings.",
                400
            )
        }

        val baseUrl = if (customBaseUrl.isNotBlank()) {
            customBaseUrl.trimEnd('/')
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        }

        val url = if (baseUrl.contains("?")) {
            "$baseUrl&key=$apiKey"
        } else {
            "$baseUrl?key=$apiKey"
        }

        val requestJson = JSONObject()

        // System instructions
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
        sysInstructionObj.put("parts", sysParts)
        requestJson.put("systemInstruction", sysInstructionObj)

        // Contents
        val contentsArray = JSONArray()
        for (item in history) {
            val contentObj = JSONObject()
            val apiRole = if (item.role == "user" || item.role == "tool") "user" else "model"
            contentObj.put("role", apiRole)
            val partsArray = JSONArray().put(JSONObject().put("text", item.text))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        requestJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.3)
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val statusCode = response.code
            val responseBody = response.body?.string() ?: ""

            // Check for rate limit or server overload
            // 429: Too Many Requests / Resource Exhausted
            // 503: Service Unavailable / Overloaded
            // 500: Internal Server Error
            if (statusCode == 429 || statusCode == 503 || statusCode == 502) {
                return@withContext GeminiResponse.RateLimitOrBusy(
                    "Rate limit or server busy (HTTP $statusCode). Server is temporarily overloaded.",
                    statusCode
                )
            }

            if (!response.isSuccessful) {
                // Check if JSON error mentions quota/resource exhausted
                if (responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                    responseBody.contains("quota", ignoreCase = true) ||
                    responseBody.contains("rate limit", ignoreCase = true) ||
                    responseBody.contains("overloaded", ignoreCase = true)
                ) {
                    return@withContext GeminiResponse.RateLimitOrBusy(
                        "Rate limit reached / Server busy: $responseBody",
                        statusCode
                    )
                }

                return@withContext GeminiResponse.FatalError(
                    "API Error (HTTP $statusCode): $responseBody",
                    statusCode
                )
            }

            // Parse success response
            val rootObj = JSONObject(responseBody)
            val candidates = rootObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResponse.FatalError("Empty response from model", statusCode)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                }
            }

            val fullText = textBuilder.toString().trim()
            val toolCall = parseToolCall(fullText)

            GeminiResponse.Success(fullText, toolCall)

        } catch (e: IOException) {
            // Network issue / timeout / connection failure can also trigger retry
            GeminiResponse.RateLimitOrBusy("Network error / Server timeout: ${e.localizedMessage}", 0)
        } catch (e: Exception) {
            GeminiResponse.FatalError("Error: ${e.localizedMessage}", -1)
        }
    }

    private fun parseToolCall(text: String): ToolCall? {
        // Look for JSON block in markdown or direct JSON
        val jsonPattern = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", RegexOption.MULTILINE)
        val match = jsonPattern.find(text)
        val jsonString = match?.groupValues?.get(1) ?: run {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                text.substring(start, end + 1)
            } else null
        } ?: return null

        return try {
            val obj = JSONObject(jsonString)
            if (obj.has("tool")) {
                val toolName = obj.getString("tool")
                val thought = obj.optString("thought", "")
                val argsObj = obj.optJSONObject("arguments")
                val argsMap = mutableMapOf<String, String>()
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.optString(key, "")
                    }
                }
                ToolCall(toolName, thought, argsMap)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
