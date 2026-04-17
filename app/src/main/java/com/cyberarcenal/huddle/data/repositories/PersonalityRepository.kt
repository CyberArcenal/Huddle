package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.PersonalityQuizApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class PersonalityRepository {
    private val api: PersonalityQuizApi = ApiService.personalityQuizApi

    suspend fun getQuestions(): Result<PersonalityQuizQuestionResponse> = safeApiCall {
        api.apiV1UsersPersonalityQuestionsRetrieve()
    }

    suspend fun getStatus(): Result<PersonalityTypeResponse> = safeApiCall {
        api.apiV1UsersPersonalityStatusRetrieve()
    }

    suspend fun submitAnswers(answers: Map<String, Int>): Result<PersonalityTypeResponse> = safeApiCall {
        api.apiV1UsersPersonalitySubmitCreate(SubmitAnswerRequest(answers))
    }

    suspend fun getPersonalityDetails(mbtiType: String): Result<PersonalityTypeDetailsResponse> = safeApiCall {
        api.apiV1UsersPersonalityDetailsRetrieve(mbtiType, mbtiType)
    }
}
