package net.crowdventures.storypop.util

import net.crowdventures.storypop.dto.AccountInfoFull
import net.crowdventures.storypop.dto.ArticleCommentPublished
import net.crowdventures.storypop.room.StoryComment

// Add to StoryComment.kt or a CommentExtensions.kt file
fun StoryComment.toArticleCommentPublished(
    slugTitle: String,
    loggedInUser: AccountInfoFull
): ArticleCommentPublished {
    return ArticleCommentPublished(
        slugTitle = slugTitle,
        authorName = loggedInUser.username,
        comment = comment,
        commentUUID = commentUUID,
        replyToCommentUIID = replyToCommentUUID,
        hearts = 0u,
        timestamp = entryTimeStamp,
        langCode = "",
        numReplies = 0u,
        deleted = false,
        hidden = false,
        authorBadge = loggedInUser.profileIcon,
        liked = false,
        replies = mutableListOf(),
        reply_to_username = null
    )
}