package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private val client = OkHttpClient()

    // Helper to build headers
    private fun getBaseRequest(url: String, key: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
    }

    // --- Dynamic REST operations ---
    suspend fun insertRow(
        tableName: String,
        jsonBody: String,
        supabaseUrl: String,
        supabaseKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return@withContext false
        try {
            val url = "${supabaseUrl.trimEnd('/')}/rest/v1/$tableName"
            val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = getBaseRequest(url, supabaseKey)
                .post(body)
                .addHeader("Prefer", "return=minimal")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Insert error to table $tableName: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert exception on table $tableName", e)
            return@withContext false
        }
    }

    suspend fun updateRow(
        tableName: String,
        rowId: Int,
        jsonBody: String,
        supabaseUrl: String,
        supabaseKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return@withContext false
        try {
            val url = "${supabaseUrl.trimEnd('/')}/rest/v1/$tableName?id=eq.$rowId"
            val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = getBaseRequest(url, supabaseKey)
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Update error to table $tableName: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update exception on table $tableName", e)
            return@withContext false
        }
    }

    suspend fun deleteRow(
        tableName: String,
        rowId: Int,
        supabaseUrl: String,
        supabaseKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return@withContext false
        try {
            val url = "${supabaseUrl.trimEnd('/')}/rest/v1/$tableName?id=eq.$rowId"
            val request = getBaseRequest(url, supabaseKey)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Delete error from table $tableName: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete exception on table $tableName", e)
            return@withContext false
        }
    }

    // --- Supabase Storage Upload ---
    suspend fun uploadFile(
        bucketName: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        supabaseUrl: String,
        supabaseKey: String
    ): String? = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return@withContext null
        try {
            val cleanFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val url = "${supabaseUrl.trimEnd('/')}/storage/v1/object/$bucketName/$cleanFileName"
            val body = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            
            // First let's check or directly upsert (overwrite if exists)
            val request = getBaseRequest(url, supabaseKey)
                .post(body)
                .addHeader("x-upsert", "true")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Storage upload failed: ${response.code} - ${response.body?.string()}")
                    return@withContext null
                }
                // Return public URL
                return@withContext "${supabaseUrl.trimEnd('/')}/storage/v1/object/public/$bucketName/$cleanFileName"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Storage upload exception", e)
            return@withContext null
        }
    }

    // --- Telegram notifications ---
    suspend fun sendTelegramMessage(
        token: String,
        chatId: String,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || chatId.isBlank()) return@withContext false
        try {
            val url = "https://api.telegram.org/bot${token.trim()}/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("text", message)
                put("parse_mode", "HTML")
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Telegram send error: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram send exception", e)
            return@withContext false
        }
    }
}
