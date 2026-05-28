package net.crowdventures.storypop.models

import net.crowdventures.storypop.R

enum class ArticleType(
    val displayName: String,
    val description: String,
    val icon: Int,
    val systemPrompt: String
) {
    RESEARCH(
        displayName = "Research Article",
        description = "Academic-style, evidence-based writing with citations and methodology",
        icon = R.drawable.ic_research,
        systemPrompt = "Write a well-researched academic article with proper citations, methodology, and evidence-based conclusions. Include an abstract, introduction, methodology, results, discussion, and conclusion sections. Reply with markdown text."
    ),
    OPINION(
        displayName = "Opinion Piece",
        description = "Personal perspective, persuasive arguments, and thought leadership",
        icon = R.drawable.ic_opinion,
        systemPrompt = "Write a compelling opinion piece that presents a clear viewpoint, supports arguments with reasoning, and engages readers with persuasive language and personal insights. Reply with markdown text."
    ),
    INVESTIGATION(
        displayName = "Journalistic Investigation",
        description = "In-depth reporting, facts, sources, and investigative narrative",
        icon = R.drawable.ic_investigation,
        systemPrompt = "Write a journalistic investigation that presents facts, includes multiple sources, follows a narrative structure, and reveals important findings or insights. Reply with markdown text."
    ),
    NEWS(
        displayName = "News Coverage",
        description = "Timely reporting, who/what/when/where/why, objective facts",
        icon = R.drawable.ic_news,
        systemPrompt = "Write a news article covering recent events with objective facts, key information (who, what, when, where, why), and quotes from relevant sources. Reply with markdown text."
    ),
    TUTORIAL(
        displayName = "How-to Guide",
        description = "Step-by-step instructions, practical advice, actionable content",
        icon = R.drawable.ic_tutorial,
        systemPrompt = "Write a step-by-step tutorial with clear instructions, practical examples, and actionable advice that readers can follow easily. Reply with markdown text."
    )
}