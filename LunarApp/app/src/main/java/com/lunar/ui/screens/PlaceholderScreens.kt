package com.lunar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.TextView
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.data.AuthSession
import com.lunar.data.AiAnalyzeRequest
import com.lunar.data.ChartResult
import com.lunar.data.ChartRecordItem
import com.lunar.data.AiAnalysisCacheEntity
import com.lunar.data.LunarLocalDatabase
import com.lunar.data.UserInfo
import com.lunar.data.analyzeChartRecordStream
import com.lunar.data.appJson
import com.lunar.data.fetchChartRecords
import com.lunar.data.fetchCurrentUser
import com.lunar.data.userMessage
import com.lunar.ui.theme.DarkText
import com.lunar.ui.theme.LightGray
import com.lunar.ui.theme.MidGray
import com.lunar.ui.theme.RedTitle
import kotlinx.serialization.decodeFromString

@Composable
fun RecordScreen(
    authSession: AuthSession?,
    onRequireLogin: () -> Unit,
    onOpenRecord: (ChartRecordItem, ChartResult) -> Unit,
    onAiAnalysis: (ChartRecordItem) -> Unit,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    var records by remember { mutableStateOf<List<ChartRecordItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery) {
        val keyword = searchQuery.trim()
        if (keyword.isEmpty()) {
            records
        } else {
            records.filter { it.chartName.contains(keyword, ignoreCase = true) }
        }
    }

    LaunchedEffect(authSession?.token, refreshKey) {
        val session = authSession
        if (session == null) {
            onRequireLogin()
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        runCatching {
            userInfo = fetchCurrentUser(session.token)
            fetchChartRecords(session.token)
        }
            .onSuccess { records = it }
            .onFailure { errorMessage = it.userMessage() }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text("排盘记录", color = RedTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        RecordSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedTitle)
            }

            errorMessage != null -> EmptyBlock(errorMessage.orEmpty(), RedTitle)
            records.isEmpty() -> EmptyBlock("暂无排盘记录", DarkText)
            filteredRecords.isEmpty() -> EmptyBlock("没有匹配的记录", DarkText)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredRecords, key = { it.id }) { item ->
                    RecordItem(
                        item = item,
                        canUseAi = userInfo?.isVip == true || (userInfo?.aiRemainCount ?: 0) > 0,
                        aiRemainCount = userInfo?.aiRemainCount ?: 0,
                        isVip = userInfo?.isVip == true,
                        onAiAnalysis = { onAiAnalysis(item) },
                        onClick = {
                            runCatching {
                                appJson.decodeFromString<ChartResult>(item.resultJson)
                            }.onSuccess { result -> onOpenRecord(item, result) }
                                .onFailure { errorMessage = "记录解析失败，无法查看详情" }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color.White)
            .border(0.8.dp, Color(0xFFD0D0D0))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isBlank()) {
            Text("按姓名搜索", color = MidGray, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = DarkText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class AiCategory(
    val name: String,
    val label: String,
    val emoji: String
)

private val AI_CATEGORIES = listOf(
    AiCategory("总览", "命局总览", "🔮"),
    AiCategory("财运", "财运分析", "💰"),
    AiCategory("事业", "事业分析", "💼"),
    AiCategory("学业", "学业分析", "📚"),
    AiCategory("姻缘", "姻缘分析", "💞"),
    AiCategory("运势", "运势分析", "🧭"),
    AiCategory("流年", "流年运势", "📅"),
    AiCategory("健康", "健康分析", "🌿")
)

@Composable
fun AiAnalysisScreen(
    record: ChartRecordItem,
    authSession: AuthSession?,
    onBack: () -> Unit,
    onAnalysisDone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<AiCategory?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isStreaming by remember { mutableStateOf(false) }
    var analysisText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var analyzeKey by remember { mutableStateOf(0) }
    var forceReanalyze by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cacheDao = remember(context) {
        LunarLocalDatabase.getInstance(context).aiAnalysisCacheDao()
    }

    LaunchedEffect(analyzeKey) {
        val category = selectedCategory ?: return@LaunchedEffect
        if (analyzeKey == 0) return@LaunchedEffect
        val session = authSession ?: run { errorMessage = "请先登录"; return@LaunchedEffect }
        isLoading = true
        isStreaming = true
        errorMessage = null
        analysisText = ""
        try {
            val cached = if (!forceReanalyze) {
                runCatching { cacheDao.find(record.id, category.name) }.getOrNull()
            } else {
                null
            }
            if (cached != null) {
                analysisText = cached.content
                return@LaunchedEffect
            }

            val collectedText = StringBuilder()
            var notified = false
            analyzeChartRecordStream(
                token = session.token,
                request = AiAnalyzeRequest(
                    recordId = record.id,
                    resultJson = record.resultJson,
                    force = forceReanalyze,
                    category = category.name
                )
            ).collect { chunk ->
                if (isLoading) isLoading = false
                if (chunk.isEmpty()) return@collect
                if (!notified) {
                    notified = true
                    onAnalysisDone()
                }
                analysisText += chunk
                collectedText.append(chunk)
                delay(0L)
            }
            if (collectedText.isNotBlank()) {
                runCatching {
                    cacheDao.upsert(
                        AiAnalysisCacheEntity(
                            recordId = record.id,
                            category = category.name,
                            content = collectedText.toString(),
                            updateTime = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (analysisText.isEmpty()) errorMessage = e.userMessage()
        } finally {
            isLoading = false
            isStreaming = false
            forceReanalyze = false
        }
    }

    LaunchedEffect(analysisText) {
        if (analysisText.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("AI分析", color = RedTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(record.title, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("姓名：${record.chartName.ifBlank { "未命名" }}", color = DarkText, fontSize = 13.sp)
        Text("性别：${record.gender.ifBlank { "-" }}", color = DarkText, fontSize = 13.sp)
        Text("出生：${record.birthTime.ifBlank { "-" }}", color = DarkText, fontSize = 13.sp)
        Text("创建：${record.createTime}", color = DarkText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedCategory == null) {
            Text("请选择分析维度", color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AI_CATEGORIES.forEach { category ->
                    TextButton(
                        onClick = {
                            selectedCategory = category
                            forceReanalyze = false
                            analyzeKey++
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color(0xFFFFF5E6),
                            contentColor = DarkText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("${category.emoji} ${category.label}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${selectedCategory!!.emoji} ${selectedCategory!!.label}",
                    color = RedTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(
                    onClick = {
                        selectedCategory = null
                        analysisText = ""
                        errorMessage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = LightGray,
                        contentColor = DarkText
                    )
                ) {
                    Text("切换维度", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = RedTitle)
                    errorMessage != null -> Text(errorMessage.orEmpty(), color = RedTitle, fontSize = 14.sp)
                    analysisText.isBlank() -> Text("分析中...", color = DarkText, fontSize = 16.sp)
                    else -> AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 14f
                                setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
                                setLineSpacing(0f, 1.4f)
                                tag = Markwon.create(ctx)
                            }
                        },
                        update = { tv ->
                            val markwon = tv.tag as Markwon
                            val normalized = analysisText
                                .replace("\r\n", "\n")
                                .replace(Regex("(?m)^#{1,6}\\s*(.+)$")) { "\n**${it.groupValues[1].trim()}**\n" }
                                .replace(Regex("\n{3,}"), "\n\n")
                                .trimStart('\n')
                            markwon.setMarkdown(tv, normalized)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "本分析仅供兴趣参考，不涉及法律、医疗、投资等专业判断，不构成任何决策依据。",
            color = Color(0xFF9E9E9E),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0xFF333A42),
                    contentColor = Color.White
                )
            ) {
                Text("返回记录", fontSize = 14.sp)
            }
            if (selectedCategory != null) {
                TextButton(
                    onClick = {
                        forceReanalyze = true
                        analyzeKey++
                    },
                    enabled = !isStreaming,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = RedTitle,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB96861),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text("重新分析", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RecordItem(
    item: ChartRecordItem,
    canUseAi: Boolean,
    aiRemainCount: Int,
    isVip: Boolean,
    onAiAnalysis: () -> Unit,
    onClick: () -> Unit
) {
    val birthDate = item.birthTime.toChineseDateText()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray)
            .border(0.5.dp, Color(0xFFD0D0D0))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.chartName.ifBlank { "未命名" },
                    color = DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    item.gender.ifBlank { "-" },
                    color = DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                birthDate,
                color = DarkText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = { if (canUseAi) onAiAnalysis() },
            enabled = canUseAi,
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (canUseAi) RedTitle else Color(0xFFBDBDBD),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFBDBDBD),
                disabledContentColor = Color.White
            )
        ) {
            val label = when {
                isVip -> "AI分析"
                aiRemainCount > 0 -> "AI(${aiRemainCount}次)"
                else -> "VIP"
            }
            Text(label, fontSize = 13.sp)
        }
    }
}

private fun String.toChineseDateText(): String {
    val date = substringBefore(" ").ifBlank { return "-" }
    val parts = date.split("-")
    if (parts.size != 3) {
        return date
    }
    val year = parts[0].toIntOrNull() ?: return date
    val month = parts[1].toIntOrNull() ?: return date
    val day = parts[2].toIntOrNull() ?: return date
    return "${year}年${month}月${day}日"
}

@Composable
private fun EmptyBlock(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 16.sp)
    }
}
