package net.crowdventures.storypop.dto

data class ArticleCommentLikeReplyNotification(val comment:ArticleCommentPublished, val replyToComment:ArticleCommentPublished?,val notificationReply:ArticleCommentPublished?  )