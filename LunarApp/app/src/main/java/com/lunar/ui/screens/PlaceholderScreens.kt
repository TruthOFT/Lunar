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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.data.AuthSession
import com.lunar.data.AiAnalyzeRequest
import com.lunar.data.BaziResponse
import com.lunar.data.ChartRecordItem
import com.lunar.data.analyzeChartRecord
import com.lunar.data.appJson
import com.lunar.data.fetchChartRecords
import com.lunar.data.userMessage
import com.lunar.ui.theme.DarkText
import com.lunar.ui.theme.LightGray
import com.lunar.ui.theme.RedTitle
import kotlinx.serialization.decodeFromString

@Composable
fun RecordScreen(
    authSession: AuthSession?,
    onRequireLogin: () -> Unit,
    onOpenRecord: (BaziResponse) -> Unit,
    onAiAnalysis: (ChartRecordItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var records by remember { mutableStateOf<List<ChartRecordItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authSession?.token) {
        val session = authSession
        if (session == null) {
            onRequireLogin()
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        runCatching { fetchChartRecords(session.token) }
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
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedTitle)
            }

            errorMessage != null -> EmptyBlock(errorMessage.orEmpty(), RedTitle)
            records.isEmpty() -> EmptyBlock("暂无排盘记录", DarkText)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { item ->
                    RecordItem(
                        item = item,
                        onAiAnalysis = { onAiAnalysis(item) },
                        onClick = {
                            runCatching {
                                appJson.decodeFromString<BaziResponse>(item.resultJson)
                            }.onSuccess(onOpenRecord)
                                .onFailure { errorMessage = "记录解析失败，无法查看详情" }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiAnalysisScreen(
    record: ChartRecordItem,
    authSession: AuthSession?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var analysisText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(record.id, authSession?.token) {
        val session = authSession
        if (session == null) {
            errorMessage = "请先登录"
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        runCatching {
            analyzeChartRecord(
                token = session.token,
                request = AiAnalyzeRequest(
                    recordId = record.id,
                    resultJson = record.resultJson
                )
            )
        }.onSuccess { analysisText = it }
            .onFailure { errorMessage = it.userMessage() }
        isLoading = false
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
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = RedTitle)
                errorMessage != null -> Text(errorMessage.orEmpty(), color = RedTitle, fontSize = 14.sp)
                analysisText.isBlank() -> Text("暂无分析内容", color = DarkText, fontSize = 16.sp)
                else -> Text(analysisText, color = DarkText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFF333A42),
                contentColor = Color.White
            )
        ) {
            Text("返回记录", fontSize = 14.sp)
        }
    }
}

@Composable
fun CourseScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "课程", modifier = modifier)
}

@Composable
private fun RecordItem(
    item: ChartRecordItem,
    onAiAnalysis: () -> Unit,
    onClick: () -> Unit
) {
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
            Text(item.title, color = DarkText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("姓名：${item.chartName.ifBlank { "未命名" }}", color = DarkText, fontSize = 13.sp)
            Text("性别：${item.gender.ifBlank { "-" }}", color = DarkText, fontSize = 13.sp)
            Text("出生：${item.birthTime.ifBlank { "-" }}", color = DarkText, fontSize = 13.sp)
            Text("创建：${item.createTime}", color = DarkText, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(
            onClick = onAiAnalysis,
            colors = ButtonDefaults.textButtonColors(
                containerColor = RedTitle,
                contentColor = Color.White
            )
        ) {
            Text("AI分析", fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyBlock(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 16.sp)
    }
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = DarkText, fontSize = 18.sp)
    }
}
