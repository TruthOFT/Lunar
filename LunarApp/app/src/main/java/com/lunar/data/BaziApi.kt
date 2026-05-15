package com.lunar.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val baziCalculateUrls = listOf(
    "http://10.0.2.2:8081/api/bazi/calculate",
    "http://101.34.238.130:8080/api/bazi/calculate"
)

suspend fun fetchBaziCalculate(
    name: String,
    sex: Int,
    solar: SolarRequest
): BaziResponse {
    return withContext(Dispatchers.IO) {
        val requestBody = buildBaziCalculateRequest(name, sex, solar)
        var lastError: Throwable? = null

        for (url in baziCalculateUrls) {
            try {
                return@withContext parseBaziResponse(postJson(url, requestBody))
            } catch (error: Throwable) {
                lastError = error
            }
        }

        throw IllegalStateException("接口请求失败：${lastError?.message ?: "未知错误"}")
    }
}

private fun buildBaziCalculateRequest(name: String, sex: Int, solar: SolarRequest): String {
    return JSONObject()
        .put("name", name)
        .put("sex", sex)
        .put(
            "solar",
            JSONObject()
                .put("year", solar.year)
                .put("month", solar.month)
                .put("day", solar.day)
                .put("hour", solar.hour)
                .put("minute", solar.minute)
        )
        .toString()
}

data class SolarRequest(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

private fun postJson(url: String, body: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    connection.setRequestProperty("Accept", "application/json")

    return try {
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ?: throw IllegalStateException("接口无响应内容：$responseCode")
        val responseText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        if (responseCode !in 200..299) {
            throw IllegalStateException("接口返回异常：$responseCode $responseText")
        }

        responseText
    } finally {
        connection.disconnect()
    }
}

private fun parseBaziResponse(json: String): BaziResponse {
    val root = JSONObject(json)
    val basicInfo = root.getJSONObject("basicInfo")
    val pillars = root.getJSONObject("bazi").getJSONObject("pillars")
    val boneWeight = root.getJSONObject("boneWeight")
    val dayun = root.getJSONObject("dayun")
    val dayunDetail = root.getJSONObject("dayunDetail")
    val xiaoyun = root.getJSONObject("xiaoyun")
    val shensha = root.getJSONObject("shensha")

    return BaziResponse(
        basicInfo = BasicInfo(
            name = basicInfo.getString("name"),
            gender = basicInfo.getString("gender"),
            zodiac = basicInfo.getString("zodiac"),
            gregorianDatetime = basicInfo.getString("gregorianDatetime"),
            lunarDatetime = basicInfo.getString("lunarDatetime"),
            solarTerms = basicInfo.getJSONObject("solarTerms").toSolarTerms(),
            taiyuan = basicInfo.getJSONObject("taiyuan").toStemBranchInfo(),
            minggong = basicInfo.getJSONObject("minggong").toStemBranchInfo(),
            startYun = basicInfo.getJSONObject("startYun").toStartYun(),
            wuxingName = basicInfo.getJSONArray("wuxingName").toWuxingNameList()
        ),
        bazi = Bazi(
            pillars = Pillars(
                year = pillars.getJSONObject("year").toPillar(),
                month = pillars.getJSONObject("month").toPillar(),
                day = pillars.getJSONObject("day").toPillar(),
                hour = pillars.getJSONObject("hour").toPillar()
            )
        ),
        boneWeight = BoneWeight(
            value = boneWeight.getString("value"),
            comment = boneWeight.getString("comment")
        ),
        dayun = Dayun(dayun.getJSONArray("items").toDayunItems()),
        dayunDetail = DayunDetail(
            tianganTenGod = dayunDetail.getJSONArray("tianganTenGod").toStringList(),
            dizhiTenGod = dayunDetail.getJSONArray("dizhiTenGod").toNestedStringList(),
            changsheng = dayunDetail.getJSONArray("changsheng").toStringList(),
            endYear = dayunDetail.getJSONArray("endYear").toIntList()
        ),
        xiaoyun = Xiaoyun(
            tenGod = xiaoyun.getJSONArray("tenGod").toStringList(),
            ganzhi = xiaoyun.getJSONArray("ganzhi").toStringList(),
            age = xiaoyun.getJSONArray("age").toIntList()
        ),
        shensha = Shensha(
            dayunShensha = shensha.getJSONArray("dayunShensha").toStringList(),
            liunianShensha = shensha.getJSONArray("liunianShensha").toStringList(),
            liuyueShensha = shensha.getJSONArray("liuyueShensha").toStringList(),
            liuriShensha = shensha.getJSONArray("liuriShensha").toStringList()
        )
    )
}

private fun JSONObject.toSolarTerms(): SolarTerms {
    return SolarTerms(
        jie = getString("jie"),
        qi = getString("qi")
    )
}

private fun JSONObject.toStemBranchInfo(): StemBranchInfo {
    return StemBranchInfo(
        value = getString("value"),
        nayin = getString("nayin")
    )
}

private fun JSONObject.toStartYun(): StartYun {
    return StartYun(
        after = getString("after"),
        startTime = getString("startTime"),
        rule = getString("rule")
    )
}

private fun JSONObject.toPillar(): Pillar {
    return Pillar(
        tiangan = getString("tiangan"),
        dizhi = getString("dizhi"),
        tenGod = getString("tenGod"),
        hiddenStems = getJSONArray("hiddenStems").toStringList(),
        hiddenTenGod = getJSONArray("hiddenTenGod").toStringList(),
        nayin = getString("nayin"),
        kongwang = getJSONArray("kongwang").toStringList(),
        shensha = getJSONArray("shensha").toStringList(),
        tianganNote = getString("tianganNote"),
        dizhiNote = getString("dizhiNote")
    )
}

private fun JSONArray.toWuxingNameList(): List<WuxingName> {
    return (0 until length()).map {
        val item = getJSONObject(it)
        WuxingName(
            element = item.getString("element"),
            char = item.getString("char")
        )
    }
}

private fun JSONArray.toDayunItems(): List<DayunItem> {
    return (0 until length()).map {
        val item = getJSONObject(it)
        DayunItem(
            age = item.getInt("age"),
            year = item.getInt("year"),
            ganzhi = item.getString("ganzhi")
        )
    }
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).map { getString(it) }
}

private fun JSONArray.toNestedStringList(): List<List<String>> {
    return (0 until length()).map { getJSONArray(it).toStringList() }
}

private fun JSONArray.toIntList(): List<Int> {
    return (0 until length()).map { getInt(it) }
}
