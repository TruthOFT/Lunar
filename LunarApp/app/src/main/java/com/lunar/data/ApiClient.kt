package com.lunar.data

import kotlinx.serialization.ExperimentalSerializationApi
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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val BACKEND_BASE_URL = "http://101.34.238.130:7000/"
private const val BAZI_BASE_URL = "http://www.lanxingai.space/"

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
    .baseUrl(BAZI_BASE_URL)
    .client(
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
    )
    .addConverterFactory(jsonConverterFactory)
    .build()

private val backendApi: BackendApi = backendRetrofit.create(BackendApi::class.java)
private val baziApi: BaziRetrofitApi = baziRetrofit.create(BaziRetrofitApi::class.java)

class ApiBusinessException(
    val code: Int,
    override val message: String
) : RuntimeException(message)

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

    @POST("api/licences/activate")
    suspend fun activateLicence(@Body request: LicenceActivateRequest): ApiResponse<LicenceActivateResponse>
}

interface BaziRetrofitApi {

    @POST("api.php")
    suspend fun calculate(@Body request: LanxingBaziRequest): LanxingApiResponse<List<BaziTreeItem>>

    @POST("api.php")
    suspend fun calculateText(@Body request: LanxingBaziTextRequest): LanxingApiResponse<String>

    @POST("api.php")
    suspend fun calculateYear(@Body request: LanxingBaziYearRequest): LanxingApiResponse<List<BaziTreeItem>>

    @POST("api.php")
    suspend fun calculateMonth(@Body request: LanxingBaziMonthRequest): LanxingApiResponse<List<BaziTreeItem>>
}

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

suspend fun activateLicence(token: String, licenceCode: String): LicenceActivateResponse {
    AuthTokenHolder.token = token
    return backendApi.activateLicence(LicenceActivateRequest(licenceCode = licenceCode)).requireData()
}

suspend fun fetchBaziCalculate(
    name: String,
    sex: Int,
    dateType: String,
    solar: SolarRequest
): ChartResult {
    val gender = if (sex == 0) "女" else "男"
    val birthTime = "%04d-%02d-%02d %02d:%02d".format(
        solar.year,
        solar.month,
        solar.day,
        solar.hour,
        solar.minute
    )
    val treeResponse = baziApi.calculate(
        LanxingBaziRequest(
            gender = gender,
            date_type = dateType,
            birth_date = birthTime,
            use_true_solar = false
        )
    )
    if (treeResponse.code != 200) {
        throw ApiBusinessException(treeResponse.code, treeResponse.msg)
    }
    val textResponse = baziApi.calculateText(
        LanxingBaziTextRequest(
            gender = gender,
            date_type = dateType,
            birth_date = birthTime,
            use_true_solar = false
        )
    )
    if (textResponse.code != 200) {
        throw ApiBusinessException(textResponse.code, textResponse.msg)
    }
    val items = treeResponse.data ?: throw IllegalStateException("接口无响应数据")
    val rawText = textResponse.data.orEmpty()
    val luckItems = parseLuckItems(rawText)
    return ChartResult(
        name = name.ifBlank { "未命名" },
        gender = gender,
        birthTime = birthTime,
        dateType = dateType,
        rawText = rawText,
        treeItems = items,
        pillars = parsePillars(rawText),
        luckItems = luckItems,
        conflictNotes = parseConflictNotes(rawText),
        summary = parseChartSummary(rawText, gender, birthTime, luckItems)
    )
}

private fun parsePillars(rawText: String): ChartPillars {
    val gan = parsePipeRow(rawText, "天干")
    val zhi = parsePipeRow(rawText, "地支")
    val ganGod = parsePipeRow(rawText, "天干十神")
    val hiddenRows = rawText.lines()
        .filter { it.contains("|") && (it.contains("偏") || it.contains("正") || it.contains("七杀") || it.contains("比肩") || it.contains("劫财") || it.contains("食神") || it.contains("伤官")) }
        .filter { it.trimStart().startsWith("藏干十神") || it.trimStart().startsWith("|").not() && it.startsWith("　　　　 |") }
    val hidden = List(4) { mutableListOf<String>() }
    hiddenRows.forEach { line ->
        parsePipeRowCells(line).forEachIndexed { index, cell ->
            if (index in 0..3 && cell.isNotBlank()) hidden[index].add(cell)
        }
    }
    val nayin = parsePipeRow(rawText, "纳音")
    val state = parsePipeRow(rawText, "自坐").ifEmpty { parsePipeRow(rawText, "星运") }
    return ChartPillars(
        year = ChartPillar(gan.getOrElse(0) { "" }, zhi.getOrElse(0) { "" }, ganGod.getOrElse(0) { "" }, hidden.getOrNull(0)?.firstOrNull().orEmpty(), hidden.getOrNull(0).orEmpty(), nayin.getOrElse(0) { "" }, state.getOrElse(0) { "" }),
        month = ChartPillar(gan.getOrElse(1) { "" }, zhi.getOrElse(1) { "" }, ganGod.getOrElse(1) { "" }, hidden.getOrNull(1)?.firstOrNull().orEmpty(), hidden.getOrNull(1).orEmpty(), nayin.getOrElse(1) { "" }, state.getOrElse(1) { "" }),
        day = ChartPillar(gan.getOrElse(2) { "" }, zhi.getOrElse(2) { "" }, ganGod.getOrElse(2) { "" }, hidden.getOrNull(2)?.firstOrNull().orEmpty(), hidden.getOrNull(2).orEmpty(), nayin.getOrElse(2) { "" }, state.getOrElse(2) { "" }),
        hour = ChartPillar(gan.getOrElse(3) { "" }, zhi.getOrElse(3) { "" }, ganGod.getOrElse(3) { "" }, hidden.getOrNull(3)?.firstOrNull().orEmpty(), hidden.getOrNull(3).orEmpty(), nayin.getOrElse(3) { "" }, state.getOrElse(3) { "" })
    )
}

private fun parsePipeRow(rawText: String, label: String): List<String> {
    val line = rawText.lines().firstOrNull { it.trimStart().startsWith(label) && it.contains("|") } ?: return emptyList()
    return parsePipeRowCells(line)
}

private fun parsePipeRowCells(line: String): List<String> {
    return line.substringAfter("|", "")
        .split("|")
        .map { it.replace("　", "").trim() }
        .filter { it.isNotBlank() }
        .take(4)
}

private fun parseLuckItems(rawText: String): List<ChartLuckItem> {
    val start = rawText.lines().indexOfFirst { it.trim() == "大运信息" }
    if (start < 0) return emptyList()
    return rawText.lines()
        .drop(start + 3)
        .takeWhile { it.isNotBlank() && !it.trim().endsWith("信息") }
        .mapNotNull { line ->
            val cells = line.split("|").map { it.trim() }
            if (cells.size < 4) return@mapNotNull null
            val startYear = cells.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val age = cells.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            ChartLuckItem(
                gz = cells.getOrElse(0) { "" },
                startYear = startYear,
                age = age,
                state = cells.getOrElse(3) { "" },
                god = cells.getOrElse(4) { "" },
                stars = cells.getOrElse(5) { "" }
            )
        }
}

private fun parseConflictNotes(rawText: String): ChartConflictNotes {
    val lines = rawText.lines()
    return ChartConflictNotes(
        tiangan = parseConflictBlock(lines, "原局天干"),
        dizhi = parseConflictBlock(lines, "原局地支")
    )
}

private fun parseConflictBlock(lines: List<String>, title: String): String {
    val start = lines.indexOfFirst { it.trim() == title }
    if (start < 0) return ""
    val nextTitles = setOf("原局天干", "原局地支", "原局整柱", "日主分析", "大运信息", "流年信息")
    return lines
        .drop(start + 1)
        .takeWhile { line ->
            val trimmed = line.trim()
            trimmed !in nextTitles && !trimmed.endsWith("信息")
        }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("  ")
}

private fun parseChartSummary(
    rawText: String,
    fallbackGender: String,
    birthTime: String,
    luckItems: List<ChartLuckItem>
): ChartSummary {
    val firstLuck = luckItems.firstOrNull()
    val startYear = firstLuck?.startYear
    return ChartSummary(
        gender = parseBracketValue(rawText, "性别").ifBlank { fallbackGender },
        zodiac = parseBracketValue(rawText, "生肖"),
        gregorianDatetime = parseBracketValue(rawText, "公历日期").ifBlank { birthTime },
        lunarDatetime = parseBracketValue(rawText, "农历日期"),
        solarTerms = parseSolarTerms(rawText),
        taiyuan = parseBracketValue(rawText, "胎元"),
        minggong = parseBracketValue(rawText, "命宫"),
        wuxing = "暂无",
        startYun = firstLuck?.let { luck ->
            if (luck.age <= 0) "出生当年起运" else "出生后 ${luck.age} 岁起运"
        }.orEmpty(),
        handoverYun = startYear?.let { "命主于公历${it}年交运" }.orEmpty(),
        changeYun = startYear?.let { "以后每逢尾数带${it % 10}的年份换运" }.orEmpty()
    )
}

private fun parseBracketValue(rawText: String, label: String): String {
    return Regex("【${Regex.escape(label)}】：([^\\r\\n]+)")
        .find(rawText)
        ?.groupValues
        ?.getOrNull(1)
        ?.cleanSummaryText()
        .orEmpty()
}

private fun parseSolarTerms(rawText: String): String {
    val lines = rawText.lines()
    val start = lines.indexOfFirst { it.trim().startsWith("【节气信息】") }
    if (start < 0) return ""
    return lines
        .drop(start + 1)
        .map { it.cleanSummaryText() }
        .filter { it.isNotBlank() }
        .takeWhile { it != "八字排盘" }
        .take(2)
        .joinToString("  ")
}

private fun String.cleanSummaryText(): String {
    return replace("\u3000", "")
        .trim()
}

suspend fun fetchBaziTreeItems(
    gender: String,
    dateType: String,
    birthTime: String,
    queryYear: Int,
    queryMonth: Int? = null
): List<BaziTreeItem> {
    val response = if (queryMonth == null) {
        baziApi.calculateYear(
            LanxingBaziYearRequest(
                gender = gender,
                date_type = dateType,
                birth_date = birthTime,
                use_true_solar = false,
                query_year = queryYear
            )
        )
    } else {
        baziApi.calculateMonth(
            LanxingBaziMonthRequest(
                gender = gender,
                date_type = dateType,
                birth_date = birthTime,
                use_true_solar = false,
                query_year = queryYear,
                query_month = queryMonth
            )
        )
    }
    if (response.code != 200) {
        throw ApiBusinessException(response.code, response.msg)
    }
    return response.data ?: throw IllegalStateException("接口无响应数据")
}

private fun formatTreeItems(items: List<BaziTreeItem>): String {
    if (items.isEmpty()) {
        return "暂无树形数据"
    }
    return buildString {
        appendLine("年份 | 年龄 | 干支 | 纳音 | 旺衰 | 十神 | 神煞")
        appendLine("---- | ---- | ---- | ---- | ---- | ---- | ----")
        items.forEach { item ->
            append(item.year ?: item.month ?: "")
            append(" | ")
            append(item.age?.let { "${it}岁" } ?: item.name.orEmpty())
            append(" | ")
            append(item.gz)
            append(" | ")
            append(item.nayin.orEmpty())
            append(" | ")
            append(item.ds.orEmpty())
            append(" | ")
            append(item.ss)
            append(" | ")
            append(item.sx.joinToString("、"))
            val hidden = item.cangGanSS.joinToString("、").takeIf { it.isNotBlank() }
            if (hidden != null) {
                append(" | 藏干: ")
                append(hidden)
            }
            appendLine()
        }
    }
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
        throw ApiBusinessException(code, message)
    }
    return data ?: throw IllegalStateException("接口无响应数据")
}

fun Throwable.userMessage(): String {
    return when (this) {
        is ApiBusinessException -> message
        is HttpException -> apiErrorMessage() ?: "接口异常：${code()}"
        is SocketTimeoutException -> "排盘接口连接超时，请稍后重试或切换网络"
        is ConnectException -> "排盘接口暂时连不上，请稍后重试或切换网络"
        is UnknownHostException -> "无法解析排盘接口域名，请检查网络"
        else -> message ?: "网络请求失败"
    }
}

fun Throwable.isLoginInvalid(): Boolean {
    return this is ApiBusinessException && code in setOf(4007, 4008, 4009)
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
