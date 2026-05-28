package net.crowdventures.storypop.models

import net.crowdventures.storypop.R

sealed class AIOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int
) {
    object TitleOnly : AIOption(
        id = "title_only",
        title = "Title Only",
        description = "Generate compelling headlines for your article",
        icon = R.drawable.ic_title,
       )

    object ContentOnly : AIOption(
        id = "content_only",
        title = "Content Only",
        description = "Write full article content based on your title",
        icon = R.drawable.ic_article   )

    object BothTitleAndContent : AIOption(
        id = "both",
        title = "Title & Content",
        description = "Generate both title and full article content",
        icon = R.drawable.ic_ai_pattern)

    companion object {
        fun getAllOptions(): List<AIOption> = listOf(
            TitleOnly,
            ContentOnly,
            BothTitleAndContent
        )

        fun getById(id: String): AIOption? = getAllOptions().find { it.id == id }
    }
}