package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Supabase client configuration and database communication manager.
 * Connects directly to Supabase project 'bansalbhavya114@gmail.com's project' (id: srpuwmqmxvwjdyyjgueh).
 * Uses SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY from .env / BuildConfig.
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    const val PROJECT_NAME = "bansalbhavya114@gmail.com's project"
    const val PROJECT_ID = "srpuwmqmxvwjdyyjgueh"

    /**
     * Supabase project base URL. Injected from .env via Secrets Gradle plugin.
     */
    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.ifBlank {
            "https://srpuwmqmxvwjdyyjgueh.supabase.co"
        }.trimEnd('/')

    /**
     * Supabase Publishable / Anon API Key. Injected from .env via Secrets Gradle plugin.
     */
    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank {
            "sb_publishable_L_rJeJgFezV1cXJevYtBgw_IsfMNZRk"
        }

    /**
     * PostgREST REST API base endpoint for querying and modifying PostgreSQL tables.
     */
    val restUrl: String
        get() = "$supabaseUrl/rest/v1"

    /**
     * Supabase GoTrue Auth base endpoint.
     */
    val authUrl: String
        get() = "$supabaseUrl/auth/v1"

    /**
     * OkHttpClient pre-configured with Supabase authentication headers:
     * - apikey: <SUPABASE_PUBLISHABLE_KEY>
     * - Authorization: Bearer <SUPABASE_PUBLISHABLE_KEY> (or user JWT)
     * - Content-Type & Accept: application/json
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("apikey", publishableKey)
                    .header("Authorization", "Bearer $publishableKey")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Prefer", "return=representation")
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    /**
     * Test communication and health check with the Supabase database.
     * Returns Result.success(true) if reachable and credentials are valid.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$restUrl/")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 200) {
                    Log.d(TAG, "Successfully connected to Supabase database ($supabaseUrl)")
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string().orEmpty()
                    Log.w(TAG, "Supabase connection returned code ${response.code}: $errorBody")
                    Result.failure(IOException("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Query records from a Supabase PostgreSQL table.
     * Example: queryTable("invoices", select = "*", filter = "status=eq.PAID")
     */
    suspend fun queryTable(
        tableName: String,
        select: String = "*",
        filter: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var url = "$restUrl/$tableName?select=$select"
            if (!filter.isNullOrBlank()) {
                url += "&$filter"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(IOException("Query failed (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upsert (Insert or Update on conflict) a record in a Supabase PostgreSQL table.
     * Uses PostgREST's resolution=merge-duplicates preference.
     * Example: upsertRecord("parties", onConflict = "name", jsonPayload = "{...}")
     */
    suspend fun upsertRecord(
        tableName: String,
        onConflict: String,
        jsonPayload: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$restUrl/$tableName?on_conflict=$onConflict")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(responseText)
                } else {
                    Result.failure(IOException("Upsert failed (${response.code}): $responseText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Insert a record or list of records into a Supabase PostgreSQL table.
     * jsonPayload: JSON string representing object or array.
     */
    suspend fun insertRecord(
        tableName: String,
        jsonPayload: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$restUrl/$tableName")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(responseText)
                } else {
                    Result.failure(IOException("Insert failed (${response.code}): $responseText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update matching records in a Supabase table.
     * Example: updateRecord("invoices", filter = "invoice_number=eq.INV-101", jsonPayload = "{\"status\":\"PAID\"}")
     */
    suspend fun updateRecord(
        tableName: String,
        filter: String,
        jsonPayload: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$restUrl/$tableName?$filter")
                .patch(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(responseText)
                } else {
                    Result.failure(IOException("Update failed (${response.code}): $responseText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete matching records from a Supabase table.
     * Example: deleteRecord("invoices", filter = "id=eq.5")
     */
    suspend fun deleteRecord(
        tableName: String,
        filter: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$restUrl/$tableName?$filter")
                .delete()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(responseText)
                } else {
                    Result.failure(IOException("Delete failed (${response.code}): $responseText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
