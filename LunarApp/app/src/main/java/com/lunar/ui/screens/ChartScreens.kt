package com.lunar.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.data.BaziResponse
import com.lunar.data.DayunItem
import com.lunar.data.SolarRequest
import com.lunar.data.fetchBaziCalculate
import com.lunar.ui.theme.BrownText
import com.lunar.ui.theme.DarkGray
import com.lunar.ui.theme.DarkText
import com.lunar.ui.theme.DateRed
import com.lunar.ui.theme.FormGreen
import com.lunar.ui.theme.GreenText
import com.lunar.ui.theme.LabelYellow
import com.lunar.ui.theme.LightGray
import com.lunar.ui.theme.LinkBlue
import com.lunar.ui.theme.MidGray
import com.lunar.ui.theme.OrangeText
import com.lunar.ui.theme.RedTitle
import com.lunar.ui.theme.LunarAppTheme
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun ChartRoute(
    result: BaziResponse?,
    onResult: (BaziResponse) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) {
        ChartFormScreen(
            onResult = onResult,
            modifier = modifier
        )
    } else {
        BaziResultScreen(
            result = result,
            onReset = onReset,
            modifier = modifier
        )
    }
}

@Composable
fun ChartFormScreen(
    onResult: (BaziResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { Calendar.getInstance() }
    var name by rememberSaveable { mutableStateOf("") }
    var calendarType by rememberSaveable { mutableStateOf("公历排盘") }
    var gender by rememberSaveable { mutableStateOf("男") }
    var shouldSave by rememberSaveable { mutableStateOf("保存") }
    var year by rememberSaveable { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableStateOf(now.get(Calendar.MONTH) + 1) }
    var day by rememberSaveable { mutableStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var hour by rememberSaveable { mutableStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by rememberSaveable { mutableStateOf(now.get(Calendar.MINUTE)) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PageTitle()

        Column(modifier = Modifier.fillMaxWidth()) {
            FormRow(label = "命主信息:") {
                Text("姓名:", fontSize = 13.sp, color = Color.Black)
                CompactTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .width(160.dp)
                        .height(25.dp)
                )
            }
            FormRow(label = "起盘方式:") {
                CompactRadio("公历排盘", calendarType, onSelect = { calendarType = it })
                CompactRadio("农历排盘", calendarType, onSelect = { calendarType = it })
            }
            FormRow(label = "出生时间:") {
                CompactSelect(year, (1900..2100).toList(), "年") { year = it }
                CompactSelect(month, (1..12).toList(), "月") { month = it }
                CompactSelect(day, (1..31).toList(), "日") { day = it }
            }
            FormRow(label = "") {
                CompactSelect(hour, (0..23).toList(), "时") { hour = it }
                CompactSelect(minute, (0..59).toList(), "分") { minute = it }
            }
            FormRow(label = "命主性别:") {
                CompactRadio("男", gender, onSelect = { gender = it })
                CompactRadio("女", gender, onSelect = { gender = it })
                Text("（排盘结果男女有别，请正确选择）", color = RedTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            FormRow(label = "是否保存:") {
                CompactRadio("保存", shouldSave, onSelect = { shouldSave = it })
                CompactRadio("不保存", shouldSave, onSelect = { shouldSave = it })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    runCatching {
                        fetchBaziCalculate(
                            name = name,
                            sex = if (gender == "男") 0 else 1,
                            solar = SolarRequest(year, month, day, hour, minute)
                        )
                    }.onSuccess(onResult)
                        .onFailure { errorMessage = it.message ?: "排盘失败，请稍后再试" }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.textButtonColors(
                containerColor = RedTitle,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFB96861),
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .height(42.dp)
        ) {
            Text("开始排盘", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(14.dp))
            CircularProgressIndicator(modifier = Modifier.size(26.dp), color = RedTitle)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = RedTitle, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 18.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))
        PromoBanner(modifier = Modifier.width(320.dp).height(135.dp))
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinks()
    }
}

@Composable
fun BaziResultScreen(
    result: BaziResponse,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PageTitle()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("八字排盘结果:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
            BasicInfoBlock(result)
            PillarTable(result)
            NoteBands(result)
        }

        Spacer(modifier = Modifier.height(18.dp))
        PromoBanner(modifier = Modifier.width(320.dp).height(135.dp))
        Spacer(modifier = Modifier.height(14.dp))

        DayunGridFromResponse(result)
        ShenshaRows(result)
        Spacer(modifier = Modifier.height(20.dp))
        PromoBanner(modifier = Modifier.width(320.dp).height(135.dp))
        Spacer(modifier = Modifier.height(14.dp))
        XiaoyunBlock(result)

        TextButton(
            onClick = onReset,
            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFFFF6565), contentColor = Color.White),
            modifier = Modifier
                .width(158.dp)
                .height(40.dp)
        ) {
            Text("重新排盘", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PageTitle() {
    Text(
        "周易大学堂四柱八字排盘",
        color = RedTitle,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 18.dp)
    )
}

@Composable
private fun FormRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .fillMaxSize()
                .background(LabelYellow),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(label, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(FormGreen)
                .padding(start = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            content = content
        )
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
        modifier = modifier
            .background(Color.White)
            .border(1.dp, Color(0xFF777777))
            .padding(horizontal = 4.dp, vertical = 3.dp)
    )
}

@Composable
private fun CompactRadio(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onSelect(label) }
            .padding(end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected == label,
            onClick = { onSelect(label) },
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF0079E6),
                unselectedColor = Color.Black
            ),
            modifier = Modifier.size(20.dp)
        )
        Text(label, fontSize = 13.sp, color = Color.Black)
    }
}

@Composable
private fun CompactSelect(
    value: Int,
    options: List<Int>,
    suffix: String,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .padding(end = 5.dp)
                .height(24.dp)
                .border(1.dp, Color(0xFF555555))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$value$suffix", color = Color.Black, fontSize = 12.sp)
            Text("▼", color = Color.Black, fontSize = 8.sp, modifier = Modifier.padding(start = 3.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text("$item$suffix", fontSize = 13.sp) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PromoBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp))
            .background(Color(0xFFE6E1D0))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color(0xFFEAE5D3))
            val mountain = Path().apply {
                moveTo(size.width * 0.45f, size.height * 0.65f)
                lineTo(size.width * 0.62f, size.height * 0.2f)
                lineTo(size.width * 0.76f, size.height * 0.72f)
                lineTo(size.width * 0.92f, size.height * 0.35f)
                lineTo(size.width, size.height * 0.78f)
            }
            drawPath(mountain, color = Color(0x662F3D38), style = Stroke(width = 3f))
            drawLine(Color(0x444A4A4A), start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.82f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.65f), strokeWidth = 2f)
            drawCircle(Color(0x55000000), radius = 17f, center = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.65f), style = Stroke(width = 3f))
            drawLine(Color(0x99000000), start = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.48f), end = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.76f), strokeWidth = 4f)
            drawLine(Color(0x99000000), start = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.76f), end = androidx.compose.ui.geometry.Offset(size.width * 0.26f, size.height * 0.76f), strokeWidth = 3f)
        }
        Text(
            "推广传统文化",
            color = RedTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 25.dp)
        )
        Text(
            "普及周易知识",
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 34.dp, top = 28.dp)
        )
    }
}

@Composable
private fun HomeLinks() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("三个小时学会看八字", color = LinkBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("关于周易的那点事儿", color = LinkBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("图书目录", color = LinkBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("周易大学堂出门不下雨万年历", color = LinkBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("推广传统文化，普及周易知识", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BasicInfoBlock(result: BaziResponse) {
    val info = result.basicInfo
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        RichLine("姓名: ", info.name, "    五行: ", info.wuxingName.joinToString("，") { "${it.char}(${it.element})" })
        RichLine("性别: ", "${info.gender}    胎元: ${info.taiyuan.value}[${info.taiyuan.nayin}]    命宫: ${info.minggong.value}[${info.minggong.nayin}]")
        RichLine("节气: ", "${info.solarTerms.jie}  ${info.solarTerms.qi}")
        RichLine("起运: ", "命主于出生后 ", info.startYun.after, " 起运")
        RichLine("交运: ", "命主于公历", info.startYun.startTime, "交运,")
        RichLine("换运: ", info.startYun.rule)
        RichLine("公历: ", info.gregorianDatetime)
        RichLine("农历: ", "${info.lunarDatetime}(生肖${info.zodiac})")
    }
}

@Composable
private fun RichLine(vararg parts: String) {
    val annotated = buildAnnotatedString {
        parts.forEachIndexed { index, text ->
            val color = when {
                index == 0 -> Color.Black
                text.any { it.isDigit() } -> DateRed
                text.contains("[") || text.contains("起运") -> GreenText
                else -> DarkText
            }
            withStyle(SpanStyle(color = color, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold)) {
                append(text)
            }
        }
    }
    Text(annotated, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
private fun PillarTable(result: BaziResponse) {
    val pillars = listOf(
        "年柱" to result.bazi.pillars.year,
        "月柱" to result.bazi.pillars.month,
        "日柱" to result.bazi.pillars.day,
        "时柱" to result.bazi.pillars.hour
    )
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
    ) {
        TableRow(cells = listOf("四柱:") + pillars.map { it.first }, background = DarkGray)
        TableRow(cells = listOf("十神:") + pillars.map { it.second.tenGod }, background = MidGray)
        TableRow(
            cells = listOf("天干:") + pillars.map { it.second.tiangan },
            background = LightGray,
            bigIndexes = setOf(1, 2, 3, 4),
            colors = listOf(DarkText, RedTitle, LinkBlue, GreenText, LinkBlue)
        )
        TableRow(
            cells = listOf("地支:") + pillars.map { it.second.dizhi },
            background = MidGray,
            bigIndexes = setOf(1, 2, 3, 4),
            colors = listOf(DarkText, RedTitle, BrownText, BrownText, OrangeText)
        )
        TableRow(
            cells = listOf("藏干:") + pillars.map { pillar ->
                pillar.second.hiddenStems.zip(pillar.second.hiddenTenGod).joinToString("\n") { "${it.first}（${it.second}）" }
            },
            background = LightGray,
            height = 74.dp
        )
        TableRow(cells = listOf("纳音:") + pillars.map { it.second.nayin }, background = MidGray)
        TableRow(cells = listOf("空亡:") + pillars.map { it.second.kongwang.joinToString("") }, background = LightGray)
        TableRow(
            cells = listOf("神煞:") + pillars.map { it.second.shensha.joinToString("\n") },
            background = MidGray,
            height = 118.dp
        )
    }
}

@Composable
private fun NoteBands(result: BaziResponse) {
    val day = result.bazi.pillars.day
    BandText("天干留意: ${day.tianganNote.ifBlank { "暂无" }}")
    BandText("地支留意: ${day.dizhiNote.ifBlank { "暂无" }}")
    BandText("称骨重量: ${result.boneWeight.value}")
    BandText("称骨评语: ${result.boneWeight.comment}", height = 48.dp)
}

@Composable
private fun BandText(text: String, height: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(if (height > 40.dp) LightGray else MidGray)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, fontSize = 13.sp, color = DarkText, lineHeight = 18.sp)
    }
}

@Composable
private fun DayunGridFromResponse(result: BaziResponse) {
    val items = result.dayun.items
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .horizontalScroll(scroll)
            .border(1.dp, Color(0xFFD0A77A))
    ) {
        TableRow(
            cells = listOf("大运") + items.map { "${it.age}岁\n${it.year}" },
            background = MidGray,
            height = 52.dp,
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
        TableRow(
            cells = listOf("干支") + items.map { it.ganzhi },
            background = LightGray,
            bigIndexes = (1..items.size).toSet(),
            colors = listOf(DarkText) + List(items.size) { RedTitle },
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
        TableRow(
            cells = listOf("天干十神") + result.dayunDetail.tianganTenGod,
            background = Color.White,
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
        TableRow(
            cells = listOf("地支十神") + result.dayunDetail.dizhiTenGod.map { it.joinToString("\n") },
            background = LightGray,
            height = 104.dp,
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
        TableRow(
            cells = listOf("长生") + result.dayunDetail.changsheng,
            background = Color.White,
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
        TableRow(
            cells = listOf("止年") + result.dayunDetail.endYear.map { it.toString() },
            background = LightGray,
            cellWidth = 58.dp,
            labelWidth = 58.dp
        )
    }
}

@Composable
private fun ShenshaRows(result: BaziResponse) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        BandText("大运神煞: ${result.shensha.dayunShensha.joinToString(" ")}")
        BandText("流年神煞: ${result.shensha.liunianShensha.joinToString(" ")}")
        BandText("流月神煞: ${result.shensha.liuyueShensha.joinToString(" ")}")
        BandText("流日神煞: ${result.shensha.liuriShensha.joinToString(" ")}")
    }
}

@Composable
private fun XiaoyunBlock(result: BaziResponse) {
    val x = result.xiaoyun
    val count = maxOf(x.age.size, x.ganzhi.size, x.tenGod.size)
    val indexes = (0 until count).toList()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        TableRow(
            cells = listOf("小运年龄") + indexes.map { x.age.getOrNull(it)?.let { age -> "${age}岁" }.orEmpty() },
            background = Color.White,
            cellWidth = 58.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("小运干支") + indexes.map { x.ganzhi.getOrNull(it).orEmpty() },
            background = LightGray,
            cellWidth = 58.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("小运十神") + indexes.map { x.tenGod.getOrNull(it).orEmpty() },
            background = Color.White,
            cellWidth = 58.dp,
            labelWidth = 74.dp
        )
    }
}

@Composable
private fun DayunGrid(result: BaziResponse) {
    val items = result.dayun.items
    val headers = listOf("日期", "流日", "流月", "流年", "大运", "年柱", "月柱", "日柱", "时柱")
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .horizontalScroll(scroll)
            .border(1.dp, Color(0xFFD0A77A))
    ) {
        TableRow(headers, background = LightGray, cellWidth = 64.dp, labelWidth = 64.dp)
        TableRow(
            cells = listOf("岁年", "", "", itemText(items.getOrNull(1)), itemText(items.firstOrNull())) + listOf("*", "*", "*", "*"),
            background = Color(0xFFE7E7E7),
            height = 56.dp,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = listOf("天干", "", "", stem(items.getOrNull(1)), stem(items.firstOrNull())) + listOf(
                result.bazi.pillars.year.tiangan,
                result.bazi.pillars.month.tiangan,
                result.bazi.pillars.day.tiangan,
                result.bazi.pillars.hour.tiangan
            ),
            background = LightGray,
            height = 58.dp,
            bigIndexes = setOf(3, 4, 5, 6, 7, 8),
            colors = listOf(DarkText, DarkText, DarkText, DarkText, DarkText, RedTitle, LinkBlue, GreenText, LinkBlue),
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = listOf("地支", "", "", branch(items.getOrNull(1)), branch(items.firstOrNull())) + listOf(
                result.bazi.pillars.year.dizhi,
                result.bazi.pillars.month.dizhi,
                result.bazi.pillars.day.dizhi,
                result.bazi.pillars.hour.dizhi
            ),
            background = Color(0xFFE7E7E7),
            height = 58.dp,
            bigIndexes = setOf(3, 4, 5, 6, 7, 8),
            colors = listOf(DarkText, DarkText, DarkText, DarkText, DarkText, RedTitle, BrownText, BrownText, OrangeText),
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = listOf("空亡", "", "", "", "") + listOf(
                result.bazi.pillars.year.kongwang.joinToString(""),
                result.bazi.pillars.month.kongwang.joinToString(""),
                result.bazi.pillars.day.kongwang.joinToString(""),
                result.bazi.pillars.hour.kongwang.joinToString("")
            ),
            background = LightGray,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = listOf("02岁\n2028", "12岁\n2038") + items.drop(1).take(7).map { "${it.age}岁\n${it.year}" },
            background = MidGray,
            height = 56.dp,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = listOf("大运") + items.map { it.ganzhi },
            background = LightGray,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = List(10) { "0000" },
            background = MidGray,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
        TableRow(
            cells = List(10) { "流年" },
            background = LightGray,
            cellWidth = 64.dp,
            labelWidth = 64.dp
        )
    }
}

@Composable
private fun EmptyLuckRows() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        listOf("流日", "天干留意:", "地支留意:", "大运神煞:", "流年神煞:", "流月神煞:", "流日神煞:").forEachIndexed { index, text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 34.dp else 36.dp)
                    .background(if (index % 2 == 0) Color.White else LightGray)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text, fontSize = 13.sp, color = DarkText)
            }
        }
    }
}

@Composable
private fun DayunDetailGrid(result: BaziResponse) {
    val items = result.dayun.items
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .horizontalScroll(scroll)
    ) {
        TableRow(
            cells = listOf("岁    年:") + items.map { "${it.age}岁" },
            background = Color.White,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("大运始于:") + items.map { it.year.toString() },
            background = Color.White,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("天干十神:") + result.dayunDetail.tianganTenGod,
            background = Color.White,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("大    运:") + items.map { it.ganzhi },
            background = Color.White,
            bigIndexes = (1..items.size).toSet(),
            colors = listOf(DarkText) + List(items.size) { RedTitle },
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("地支十神:") + result.dayunDetail.dizhiTenGod.map { it.joinToString("\n") },
            background = Color.White,
            height = 112.dp,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("十二长生:") + result.dayunDetail.changsheng,
            background = Color.White,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("大运止于:") + result.dayunDetail.endYear.map { it.toString() },
            background = Color.White,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
        TableRow(
            cells = listOf("流    年:") + buildCycleYears(items),
            background = Color.White,
            textColor = LinkBlue,
            height = 170.dp,
            cellWidth = 44.dp,
            labelWidth = 74.dp
        )
    }
}

@Composable
private fun SmallLuckBlock(result: BaziResponse) {
    val x = result.xiaoyun
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        TableRow(cells = listOf("小运十神:", x.tenGod.getOrElse(0) { "" }, x.tenGod.getOrElse(1) { "" }, x.tenGod.getOrElse(2) { "" }), background = Color.White, cellWidth = 84.dp, labelWidth = 90.dp)
        TableRow(cells = listOf("小    运:", x.ganzhi.getOrElse(0) { "" }, x.ganzhi.getOrElse(1) { "" }, x.ganzhi.getOrElse(2) { "" }), background = Color.White, cellWidth = 84.dp, labelWidth = 90.dp)
        TableRow(cells = listOf("", "${x.age.getOrElse(0) { 1 }}岁", "${x.age.getOrElse(1) { 2 }}岁", "${x.age.getOrElse(2) { 3 }}岁"), background = Color.White, cellWidth = 84.dp, labelWidth = 90.dp)
        TableRow(cells = listOf("流    年:", "丙午", "丁未", "戊申"), background = Color.White, cellWidth = 84.dp, labelWidth = 90.dp)
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    background: Color,
    height: Dp = 38.dp,
    labelWidth: Dp = 58.dp,
    cellWidth: Dp = 80.dp,
    bigIndexes: Set<Int> = emptySet(),
    colors: List<Color> = emptyList(),
    textColor: Color = DarkText
) {
    Row(modifier = Modifier.height(height)) {
        cells.forEachIndexed { index, cell ->
            val width = if (index == 0) labelWidth else cellWidth
            Box(
                modifier = Modifier
                    .width(width)
                    .fillMaxSize()
                    .background(background)
                    .border(0.5.dp, Color.White)
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell,
                    color = colors.getOrNull(index) ?: textColor,
                    fontSize = if (index in bigIndexes) 24.sp else 12.sp,
                    lineHeight = if (index in bigIndexes) 26.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun itemText(item: DayunItem?): String = item?.let { "${it.age}岁\n${it.year}" } ?: ""
private fun stem(item: DayunItem?): String = item?.ganzhi?.take(1).orEmpty()
private fun branch(item: DayunItem?): String = item?.ganzhi?.drop(1).orEmpty()

private fun buildCycleYears(items: List<DayunItem>): List<String> {
    val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    return items.mapIndexed { index, _ ->
        (0..8).joinToString("\n") { row ->
            stems[(index + row) % stems.size] + branches[(index + row) % branches.size]
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChartScreenPreview() {
    LunarAppTheme {
        ChartFormScreen(onResult = {})
    }
}
