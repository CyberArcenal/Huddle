package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class OTPRequestsRepository {
    private val api = ApiService.otpRequestsApi

    suspend fun getOtpRequests(
        email: String? = null,
        isUsed: Boolean? = null,
        search: String? = null
    ): Result<OtpRequestListResponse> =
        safeApiCall { api.otpRequestsRetrieve(email, isUsed, search) }

    suspend fun getOtpRequest(id: Int): Result<OtpRequestDisplay> =
        safeApiCall { api.otpRequestsRetrieve2(id) }
}