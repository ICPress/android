package net.crowdventures.storypop.util

import net.crowdventures.storypop.models.GroqModel

object GroqModelProvider {
    fun getModels(): List<GroqModel> = listOf(
        // Meta Llama Models (Production)
        GroqModel(
            id = "llama-3.3-70b-versatile",
            displayName = "Llama 3.3 70B Versatile",
            description = "High-performance flagship model for complex reasoning, creative writing, and nuanced understanding. Best for article generation, detailed analysis, and professional content.",
            rpm = 30,
            tpm = 12000, // Standardized Free Tier TPM for 70B
            contextWindow = 128000,
            isFree = true
        ),
        GroqModel(
            id = "llama-3.1-8b-instant",
            displayName = "Llama 3.1 8B Instant",
            description = "Fast, lightweight model optimized for quick responses. Ideal for real-time editing, headline generation, and rapid content iterations.",
            rpm = 30,
            tpm = 60000, // Higher TPM allowance for smaller "instant" models
            contextWindow = 128000,
            isFree = true
        ),
        // OpenAI OSS Models (High Context)
        GroqModel(
            id = "openai/gpt-oss-120b",
            displayName = "GPT-OSS 120B",
            description = "Large-scale open-source model with extensive context window. Perfect for long-form articles, in-depth research, and comprehensive content generation.",
            rpm = 30,
            tpm = 8000,
            contextWindow = 131072,
            isFree = true
        ),
        GroqModel(
            id = "openai/gpt-oss-20b",
            displayName = "GPT-OSS 20B",
            description = "Efficient model balancing speed and quality. Well-suited for everyday writing tasks, summaries, and content improvements.",
            rpm = 30,
            tpm = 8000,
            contextWindow = 131072,
            isFree = true
        ),
        // Specialized Systems
        GroqModel(
            id = "groq/compound",
            displayName = "Groq Compound (Search/Logic)",
            description = "Specialized model for structured reasoning, fact-checking, and logical analysis. Excellent for outlines, argument development, and research assistance.",
            rpm = 30,
            tpm = 70000,
            contextWindow = 32768,
            isFree = true
        )
    )
}