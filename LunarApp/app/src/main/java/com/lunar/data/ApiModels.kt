package com.lunar.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
)

@Serializable
data class AuthRequest(
    val account: String,
    val password: String,
    val nickname: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: Long,
    val account: String,
    val nickname: String
)

@Serializable
data class RecordSaveRequest(
    val title: String,
    val chartName: String,
    val gender: String,
    val birthTime: String,
    val resultJson: String
)

@Serializable
data class ChartRecordItem(
    val id: Long,
    val title: String,
    val chartName: String,
    val gender: String,
    val birthTime: String,
    val resultJson: String,
    val createTime: String
)

@Serializable
data class AiAnalyzeRequest(
    val recordId: Long,
    val resultJson: String
)

data class AuthSession(
    val token: String,
    val userId: Long,
    val account: String,
    val nickname: String
)
