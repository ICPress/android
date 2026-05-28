package net.crowdventures.storypop.dto

import net.crowdventures.storypop.room.StoryComment

class ArticleCommentPublished(
    slugTitle: String,
    authorName: String,
    comment: String,
    commentUUID: String,
    replyToCommentUIID: String?,
    var hearts: UInt,
    val timestamp: String,
    val langCode: String,
    var numReplies: UInt,
    var deleted: Boolean,
    var hidden: Boolean,
    var authorBadge: String?,
    var liked: Boolean,
    var replies: MutableList<ArticleCommentPublished>,
    var reply_to_username: String?
) : ArticleComment(slugTitle, authorName, comment, commentUUID, replyToCommentUIID) {
    fun toStoryComment(): StoryComment {
        return StoryComment(
            slugTitle,
            authorName,
            comment,
            commentUUID,
            replyToCommentUUID,
            timestamp
        )
    }
}

