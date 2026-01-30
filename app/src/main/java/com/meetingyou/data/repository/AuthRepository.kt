package com.meetingyou.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.meetingyou.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    val currentUser = auth.currentUser
    
    fun login(email: String, password: String): Flow<com.meetingyou.utils.Resource<User>> = flow {
        emit(com.meetingyou.utils.Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userDoc = db.collection("users").document(result.user!!.uid).get().await()
            val user = userDoc.toObject(User::class.java)!!
            emit(com.meetingyou.utils.Resource.Success(user))
        } catch (e: Exception) {
            emit(com.meetingyou.utils.Resource.Error(e.message ?: "Unknown error"))
        }
    }
    
    fun register(name: String, email: String, password: String): Flow<com.meetingyou.utils.Resource<User>> = flow {
        emit(com.meetingyou.utils.Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                uid = result.user!!.uid,
                name = name,
                email = email
            )
            db.collection("users").document(user.uid).set(user).await()
            emit(com.meetingyou.utils.Resource.Success(user))
        } catch (e: Exception) {
            emit(com.meetingyou.utils.Resource.Error(e.message ?: "Unknown error"))
        }
    }
    
    fun logout() {
        auth.signOut()
    }
}
