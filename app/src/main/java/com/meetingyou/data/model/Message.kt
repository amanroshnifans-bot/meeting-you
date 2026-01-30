package com.meetingyou.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false,
    val type: String = "text"
)
