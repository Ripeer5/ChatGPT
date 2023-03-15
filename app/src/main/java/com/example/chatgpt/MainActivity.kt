package com.example.chatgpt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerIconDefaults.Text
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.springframework.stereotype.Service


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setContent {
            DisplayTextField("vide")
        }


    }
}

fun example(): String {
    val properties = ChatGPTProperties(
        "https://api.openai.com/v1/chat/completions",
        "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"
    )
    val client = ChatGPTAutoConfiguration().gptWebClient(properties)
    val gptService = GptService(client)
    val response = gptService.askGpt(
        ChatCompletionRequest(
            listOf(ChatMessage(content = "Hello!"))
        )
    ).block()

    return response.id
}

@Composable
fun DisplayTextField(text: String) {
    TextField(value = text,
        onValueChange = { /* logique à exécuter lorsqu'il y a un changement de valeur */ },
        label = { Text(example()) })
}

@AutoConfigureAfter(JacksonAutoConfiguration::class)
@EnableConfigurationProperties(ChatGPTProperties::class)
class ChatGPTAutoConfiguration {

    @Bean
    fun gptWebClient(chatGPTProperties: ChatGPTProperties) =
        WebClient.builder()
            .baseUrl(chatGPTProperties.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${chatGPTProperties.token}")
            .build()

    @Bean
    fun gptService(gptWebClient: WebClient) = GptService(gptWebClient)
}

@ConstructorBinding
@ConfigurationProperties("chatgpt")
data class ChatGPTProperties(
    val url: String,
    val token: String
)


class GptService(
    private val gptWebClient: WebClient
) {
    fun askGpt(request: ChatCompletionRequest) = gptWebClient.post()
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ChatCompletionResponse::class.java)
}

data class ChatCompletionResponse(
    val id: String,
    val created: Long,
    val choices: List<ChatChoice>,
    val usage: ChatUsage
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @JsonProperty("finish_reason")
    val finishReason: String
)

data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: String = GPT3_5_TURBO,
    val temperature: Float = 1f,
) {
    companion object {
        const val GPT3_5_TURBO = "gpt-3.5-turbo"
        const val GPT3_5_TURBO_0301 = "gpt-3.5-turbo-0301"
    }
}

data class ChatMessage(
    val role: ChatMessageRole = ChatMessageRole.USER,
    val content: String
)

enum class ChatMessageRole(private val value: String) {
    USER("user"), SYSTEM("system"), ASSISTANT("assistant");

    @JsonValue
    fun getValue() = value
}

data class ChatUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int
)