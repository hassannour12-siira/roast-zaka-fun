package com.example.model

enum class AiProvider(val displayName: String, val badge: String, val description: String) {
    OPENAI("OpenAI (ChatGPT)", "GPT-4o", "OpenAI's latest GPT-4o model"),
    CLAUDE("Anthropic (Claude)", "Claude 3.5 Sonnet", "Anthropic's flagship Claude 3.5 Sonnet"),
    GEMINI("Google Gemini", "Gemini 2.5 Flash", "Google's high-speed multimodal Gemini")
}
