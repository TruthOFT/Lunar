package com.lunar.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

private const val BACKEND_BASE_URL = "http://192.168.0.222:8080/"

object AuthTokenHolder {
    @Volatile
    var token: String = ""
}

val appJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@OptIn(ExperimentalSerializationApi::class)
private val jsonConverterFactory = appJson.asConverterFactory("application/json; charset=utf-8".toMediaType())

private val backendRetrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BACKEND_BASE_URL)
    .client(
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = AuthTokenHolder.token
                val requestBuilder = chain.request().newBuilder()
                if (token.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    )
    .addConverterFactory(jsonConverterFactory)
    .build()

private val baziRetrofit: Retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8081/")
    .client(OkHttpClient.Builder().build())
    .addConverterFactory(jsonConverterFactory)
    .build()

private val backendApi: BackendApi = backendRetrofit.create(BackendApi::class.java)
private val baziApi: BaziRetrofitApi = baziRetrofit.create(BaziRetrofitApi::class.java)

private val baziCalculateUrls = listOf(
    "http://10.0.2.2:8081/api/bazi/calculate",
    "http://101.34.238.130:8080/api/bazi/calculate"
)

interface BackendApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): ApiResponse<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): ApiResponse<AuthResponse>

    @GET("api/user/me")
    suspend fun me(): ApiResponse<UserInfo>

    @POST("api/records")
    suspend fun saveRecord(@Body request: RecordSaveRequest): ApiResponse<ChartRecordItem>

    @GET("api/records")
    suspend fun listRecords(): ApiResponse<List<ChartRecordItem>>

    @POST("api/ai/analyze")
    suspend fun analyze(@Body request: AiAnalyzeRequest): ApiResponse<String>
}

interface BaziRetrofitApi {

    @POST
    suspend fun calculate(@Url url: String, @Body request: BaziCalculateRequest): BaziResponse
}

@Serializable
data class BaziCalculateRequest(
    val name: String,
    val sex: Int,
    val solar: SolarRequest
)

suspend fun login(account: String, password: String): AuthSession {
    val response = backendApi.login(AuthRequest(account = account, password = password))
    return response.requireData().toSession()
}

suspend fun register(account: String, password: String, nickname: String): AuthSession {
    val response = backendApi.register(AuthRequest(account = account, password = password, nickname = nickname))
    return response.requireData().toSession()
}

suspend fun fetchCurrentUser(token: String): UserInfo {
    AuthTokenHolder.token = token
    return backendApi.me().requireData()
}

suspend fun saveChartRecord(token: String, request: RecordSaveRequest): ChartRecordItem {
    AuthTokenHolder.token = token
    return backendApi.saveRecord(request).requireData()
}

suspend fun fetchChartRecords(token: String): List<ChartRecordItem> {
    AuthTokenHolder.token = token
    return backendApi.listRecords().requireData()
}

suspend fun analyzeChartRecord(token: String, request: AiAnalyzeRequest): String {
    AuthTokenHolder.token = token
    return backendApi.analyze(request).requireData()
}

suspend fun fetchBaziCalculate(name: String, sex: Int, solar: SolarRequest): BaziResponse {
    val request = BaziCalculateRequest(name = name, sex = sex, solar = solar)
    var lastError: Throwable? = null

    for (url in baziCalculateUrls) {
        try {
            return baziApi.calculate(url, request)
        } catch (error: Throwable) {
            lastError = error
        }
    }

    throw IllegalStateException("接口请求失败：${lastError?.message ?: "未知错误"}")
}

private fun AuthResponse.toSession(): AuthSession {
    return AuthSession(
        token = token,
        userId = user.id,
        account = user.account,
        nickname = user.nickname
    )
}

private fun <T> ApiResponse<T>.requireData(): T {
    if (code != 0) {
        throw IllegalStateException(message)
    }
    return data ?: throw IllegalStateException("接口无响应数据")
}

fun Throwable.userMessage(): String {
    return when (this) {
        is HttpException -> apiErrorMessage() ?: "接口异常：${code()}"
        else -> message ?: "网络请求失败"
    }
}

private fun HttpException.apiErrorMessage(): String? {
    val body = response()?.errorBody()?.string().orEmpty()
    if (body.isBlank()) {
        return null
    }
    return runCatching {
        appJson.parseToJsonElement(body)
            .jsonObject["message"]
            ?.jsonPrimitive
            ?.content
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
