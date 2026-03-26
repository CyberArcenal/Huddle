package com.cyberarcenal.huddle.data.repositories.utils

import retrofit2.Response

class ApiException(
    val httpCode: Int,
    val errorBody: String? = null
) : Exception(errorBody ?: "HTTP $httpCode")

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Response body is null"))
        } else {
            val errorBody = response.errorBody()?.string()
            throw ApiException(response.code(), errorBody)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}