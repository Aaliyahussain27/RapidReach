package com.example.rapidreach.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://fsuttyjfqfiwpsmdenff.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZzdXR0eWpmcWZpd3BzbWRlbmZmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ1NDYwMDIsImV4cCI6MjA5MDEyMjAwMn0.7RD1B7fSRb8g4cyZQK2vKFpm0iqmMOOJN40rdQkFCdA"
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
        install(Realtime)
    }
}
