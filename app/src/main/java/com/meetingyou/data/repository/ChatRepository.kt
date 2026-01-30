package com.meetingyou.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.meetingyou.data.model.Chat
import com.meetingyou.data.model.Message
import com.meetingyou.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    
    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    val participants = doc.get("participants") as List<String>
                    val otherUserId = participants.first { it != currentUserId }
                    
                    Chat(
                        chatId = doc.id,
                        userId = otherUserId,
                        userName = doc.getString("userName_$otherUserId") ?: "",
                        userImage = doc.getString("userImage_$otherUserId") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageTime = doc.getLong("lastMessageTime") ?: 0,
                        unreadCount = doc.getLong("unread_$currentUserId")?.toInt() ?: 0
                    )
                } ?: emptyList()
                
                trySend(chats)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.map { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        receiverId = doc.getString("receiverId") ?: "",
                        message = doc.getString("message") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        seen = doc.getBoolean("seen") ?: false,
                        type = doc.getString("type") ?: "text"
                    )
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun sendMessage(chatId: String, message: Message) {
        val chatRef = db.collection("chats").document(chatId)
        
        db.runBatch { batch ->
            batch.set(chatRef.collection("messages").document(), message)
            batch.update(chatRef, mapOf(
                "lastMessage" to message.message,
                "lastMessageTime" to message.timestamp,
                "lastSender" to message.senderId
            ))
        }.await()
    }
    
    suspend fun createChat(otherUser: User): String {
        val chatId = if (currentUserId > otherUser.uid) 
            "${currentUserId}_${otherUser.uid}" 
        else 
            "${otherUser.uid}_${currentUserId}"
            
        val chatDoc = db.collection("chats").document(chatId).get().await()
        
        if (!chatDoc.exists()) {
            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)!!
            
            val chatData = hashMapOf(
                "participants" to listOf(currentUserId, otherUser.uid),
                "userName_${currentUserId}" to currentUser.name,
                "userName_${otherUser.uid}" to otherUser.name,
                "userImage_${currentUserId}" to currentUser.profileImage,
                "userImage_${otherUser.uid}" to otherUser.profileImage,
                "lastMessage" to "",
                "lastMessageTime" to 0,
                "unread_$currentUserId" to 0,
                "unread_${otherUser.uid}" to 0
            )
            db.collection("chats").document(chatId).set(chatData).await()
        }
        
        return chatId
    }
    
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { 
                    it.toObject(User::class.java) 
                }?.filter { it.uid != currentUserId } ?: emptyList()
                
                trySend(users)
            }
        
        awaitClose { listener.remove() }
    }
}
