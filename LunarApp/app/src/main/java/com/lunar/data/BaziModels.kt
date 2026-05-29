package com.lunar.data

import kotlinx.serialization.Serializable

@Serializable
data class ChartResult(
    val name: String,
    val gender: String,
    val birthTime: String,
    val dateType: String,
    val rawText: String,
    val treeItems: List<BaziTreeItem> = emptyList(),
    val pillars: ChartPillars = ChartPillars(),
    val luckItems: List<ChartLuckItem> = emptyList(),
    val conflictNotes: ChartConflictNotes = ChartConflictNotes()
)

@Serializable
data class LanxingApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null,
    val timestamp: Long? = null
)

@Serializable
data class LanxingBaziRequest(
    val action: String = "八字_树形",
    val gender: String,
    val date_type: String,
    val birth_date: String,
    val use_true_solar: Boolean = false
)

@Serializable
data class LanxingBaziTextRequest(
    val action: String = "八字",
    val gender: String,
    val date_type: String,
    val birth_date: String,
    val use_true_solar: Boolean = false
)

@Serializable
data class LanxingBaziYearRequest(
    val action: String = "八字_树形",
    val gender: String,
    val date_type: String,
    val birth_date: String,
    val use_true_solar: Boolean = false,
    val query_year: Int
)

@Serializable
data class LanxingBaziMonthRequest(
    val action: String = "八字_树形",
    val gender: String,
    val date_type: String,
    val birth_date: String,
    val use_true_solar: Boolean = false,
    val query_year: Int,
    val query_month: Int
)

@Serializable
data class BaziTreeItem(
    val year: Int? = null,
    val age: Int? = null,
    val idx: Int? = null,
    val month: Int? = null,
    val name: String? = null,
    val gz: String = "",
    val nayin: String? = null,
    val ds: String? = null,
    val ss: String = "",
    val cangGanSS: List<String> = emptyList(),
    val sx: List<String> = emptyList()
)

@Serializable
data class ChartPillars(
    val year: ChartPillar = ChartPillar(),
    val month: ChartPillar = ChartPillar(),
    val day: ChartPillar = ChartPillar(),
    val hour: ChartPillar = ChartPillar()
)

@Serializable
data class ChartPillar(
    val gan: String = "",
    val zhi: String = "",
    val ganGod: String = "",
    val zhiGod: String = "",
    val hidden: List<String> = emptyList(),
    val nayin: String = "",
    val state: String = ""
)

@Serializable
data class ChartConflictNotes(
    val tiangan: String = "",
    val dizhi: String = ""
)

@Serializable
data class ChartLuckItem(
    val gz: String,
    val startYear: Int,
    val age: Int,
    val state: String = "",
    val god: String = "",
    val stars: String = ""
)

@Serializable
data class SolarRequest(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

@Serializable
data class BaziResponse(
    val basicInfo: BasicInfo,
    val bazi: Bazi,
    val boneWeight: BoneWeight,
    val dayun: Dayun,
    val dayunDetail: DayunDetail,
    val xiaoyun: Xiaoyun,
    val shensha: Shensha
)

@Serializable
data class BasicInfo(
    val name: String,
    val gender: String,
    val zodiac: String,
    val gregorianDatetime: String,
    val lunarDatetime: String,
    val solarTerms: SolarTerms,
    val taiyuan: StemBranchInfo,
    val minggong: StemBranchInfo,
    val startYun: StartYun,
    val wuxingName: List<WuxingName>
)

@Serializable
data class SolarTerms(
    val jie: String,
    val qi: String
)

@Serializable
data class StemBranchInfo(
    val value: String,
    val nayin: String
)

@Serializable
data class StartYun(
    val after: String,
    val startTime: String,
    val rule: String
)

@Serializable
data class WuxingName(
    val element: String,
    val char: String
)

@Serializable
data class Bazi(
    val pillars: Pillars
)

@Serializable
data class Pillars(
    val year: Pillar,
    val month: Pillar,
    val day: Pillar,
    val hour: Pillar
)

@Serializable
data class Pillar(
    val tiangan: String,
    val dizhi: String,
    val tenGod: String,
    val hiddenStems: List<String>,
    val hiddenTenGod: List<String>,
    val nayin: String,
    val kongwang: List<String>,
    val shensha: List<String>,
    val tianganNote: String,
    val dizhiNote: String
)

@Serializable
data class BoneWeight(
    val value: String,
    val comment: String
)

@Serializable
data class Dayun(
    val items: List<DayunItem>
)

@Serializable
data class DayunItem(
    val age: Int,
    val year: Int,
    val ganzhi: String
)

@Serializable
data class DayunDetail(
    val tianganTenGod: List<String>,
    val dizhiTenGod: List<List<String>>,
    val changsheng: List<String>,
    val endYear: List<Int>
)

@Serializable
data class Xiaoyun(
    val tenGod: List<String>,
    val ganzhi: List<String>,
    val age: List<Int>
)

@Serializable
data class Shensha(
    val dayunShensha: List<String>,
    val liunianShensha: List<String>,
    val liuyueShensha: List<String>,
    val liuriShensha: List<String>
)
