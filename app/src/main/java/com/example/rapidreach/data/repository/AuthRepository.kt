package com.example.rapidreach.data.repository

import com.example.rapidreach.data.model.User
import com.example.rapidreach.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {
    private val auth = SupabaseClient.client.auth
    private val postgrest = SupabaseClient.client.postgrest

    suspend fun signup(email: String, password: String, user: User): Result<User> {
        return try {
            val response = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // In Supabase, if email confirmation is on, the user object might be inside 'response' 
            // but the session won't be active until confirmed.
            val userId = response?.id ?: auth.currentUserOrNull()?.id 
                ?: throw Exception("Signup failed: Could not retrieve User ID. Please check your email for confirmation.")

            val userWithId = user.copy(id = userId)
            
            // Insert user profile into public.users table
            postgrest["users"]
                .insert(userWithId)

            Result.success(userWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            val userId = auth.currentUserOrNull()?.id 
                ?: throw Exception("Login failed: Session not established. Ensure your email is confirmed.")

            // Fetch user data from Postgres
            val user = postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
                ?: throw Exception("User profile not found in database.")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val user = postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
                ?: throw Exception("User profile not found.")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            postgrest["users"]
                .update(user) {
                    filter {
                        eq("id", user.id)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }
}