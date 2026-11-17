package com.pla.gambarcocok

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchFact(apiKey: String, emoji: String, callback: (String?) -> Unit) {
        val prompt = "Berikan satu fakta unik singkat (maks 20 kata) dalam Bahasa Indonesia tentang gambar emoji ${emoji}."
        val bodyJson = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val s = response.body?.string() ?: return callback(null)
                    val json = JSONObject(s)
                    val text = json
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0)
                        .getString("text")
                    callback(text.trim())
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
}