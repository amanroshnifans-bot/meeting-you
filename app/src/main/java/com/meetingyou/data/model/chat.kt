package com.meetingyou.data.model

data class Chat(
    val chatId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val online: Boolean = false,
    val typing: Boolean = false
)
