package com.cyberarcenal.huddle.network

import android.content.Context
import com.cyberarcenal.huddle.api.models.TokenRefreshRequestRequest
import com.cyberarcenal.huddle.data.repositories.TokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val context: Context,
    private val refreshRepository: TokenRepository
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Kunin ang refresh token mula sa DataStore (TokenManager)
        // Gumagamit tayo ng runBlocking dahil ang authenticate ay synchronous function
        val refreshToken = runBlocking {
            TokenManager.getRefreshToken(context)
        } ?: return null // Kung walang refresh token, logout na (return null)

        // 2. Tawagin ang Refresh API
        val result = runBlocking {
            refreshRepository.refreshToken(TokenRefreshRequestRequest(refreshToken))
        }

        return if (result.isSuccess) {
            val tokenData = result.getOrThrow()

            // 3. I-save ang bagong tokens sa DataStore
            runBlocking {
                TokenManager.saveTokens(
                    context,
                    tokenData.access,
                    tokenData.refresh ?: refreshToken // Gamitin ang luma kung walang bagong refresh token
                )
            }

            // 4. I-retry ang original request gamit ang bagong Access Token
            response.request.newBuilder()
                .header("Authorization", "Bearer ${tokenData.access}")
                .build()
        } else {
            // 5. Kung failed ang refresh (ex: expired na rin ang refresh token), i-clear ang data
            runBlocking {
                TokenManager.clearAll(context)
            }
            null
        }
    }
}