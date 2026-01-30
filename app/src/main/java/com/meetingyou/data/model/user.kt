package com.meetingyou.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val status: String = "Hey there! I'm using Meeting You",
    val online: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val typingTo: String = ""
) : Parcelable
