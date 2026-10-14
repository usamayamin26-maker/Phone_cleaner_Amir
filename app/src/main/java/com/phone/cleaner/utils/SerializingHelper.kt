package com.phone.cleaner.utils

import kotlinx.serialization.json.Json

object SerializingHelper {

    private val json: Json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun getJson(): Json = json

    inline fun <reified T> fromJson(data: String): T? {
        return try {
            getJson().decodeFromString<T>(data)
        } catch (_: Exception) {
            null
        }
    }

    inline fun <reified T> toJson(data: T): String? {
        return try {
            getJson().encodeToString(data)
        } catch (_: Exception) {
            null
        }
    }
}
