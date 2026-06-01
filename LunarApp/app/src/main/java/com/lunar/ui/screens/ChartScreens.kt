package com.lunar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.data.AuthSession
import com.lunar.data.BaziResponse
import com.lunar.data.BaziTreeItem
import com.lunar.data.ChartResult
import com.lunar.data.ChartLuckItem
import com.lunar.data.DayunItem
import com.lunar.data.RecordSaveRequest
import com.lunar.data.SolarRequest
import com.lunar.data.UserInfo
import com.lunar.data.activateLicence
import com.lunar.data.appJson
import com.lunar.data.fetchBaziCalculate
import com.lunar.data.fetchBaziTreeItems
import com.lunar.data.fetchCurrentUser
import com.lunar.data.saveChartRecord
import com.lunar.data.userMessage
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Composable
fun ChartRoute(
    result: ChartResult?,
    authSession: AuthSession?,
    onResult: (ChartResult) -> Unit,
    onReset: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) {
        ChartFormScreen(
            authSession = authSession,
            onResult = onResult,
            onRequireLogin = onRequireLogin,
            modifier = modifier
        )
    } else {
        BaziResultScreen(
            result = result,
            authSession = authSession,
            onReset = onReset,
            onRequireLogin = onRequireLogin,
            modifier = modifier
        )
    }
}

@Composable
fun ChartFormScreen(
    authSession: AuthSession? = null,
    onResult: (ChartResult) -> Unit,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    var name by rememberSaveable { mutableStateOf("") }
    var calendarType by rememberSaveable { mutableStateOf("公历排盘") }
    var gender by rememberSaveable { mutableStateOf("女") }
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
                CompactRadio("女", gender, onSelect = { gender = it })
                CompactRadio("男", gender, onSelect = { gender = it })
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
                        val response = fetchBaziCalculate(
                            name = name,
                            sex = if (gender == "女") 0 else 1,
                            dateType = if (calendarType == "农历排盘") "农历" else "公历",
                            solar = SolarRequest(year, month, day, hour, minute)
                        )
                        if (shouldSave == "保存") {
                            val activeSession = authSession
                            if (activeSession == null) {
                                Toast.makeText(context, "请先登录后保存记录", Toast.LENGTH_SHORT).show()
                            } else {
                                saveChartRecord(
                                    token = activeSession.token,
                                    request = RecordSaveRequest(
                                        title = buildRecordTitle(name, year, month, day),
                                        chartName = name.ifBlank { "未命名" },
                                        gender = gender,
                                        birthTime = buildBirthTime(year, month, day, hour, minute),
                                        resultJson = appJson.encodeToString(response)
                                    )
                                )
                                Toast.makeText(context, "排盘记录已保存", Toast.LENGTH_SHORT).show()
                            }
                        }
                        response
                    }.onSuccess(onResult)
                        .onFailure { errorMessage = it.userMessage() }
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

private fun buildRecordTitle(name: String, year: Int, month: Int, day: Int): String {
    return "${name.ifBlank { "未命名" }} $year-$month-$day"
}

private fun buildBirthTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
    return "%04d-%02d-%02d %02d:%02d".format(year, month, day, hour, minute)
}

@Composable
fun BaziResultScreen(
    result: ChartResult,
    authSession: AuthSession? = null,
    onReset: () -> Unit,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var userReloadKey by remember { mutableStateOf(0) }
    var showActivateDialog by remember { mutableStateOf(false) }
    var activateCode by rememberSaveable { mutableStateOf("") }
    var isActivating by rememberSaveable { mutableStateOf(false) }
    var showNotebookDialog by rememberSaveable(result.birthTime) { mutableStateOf(false) }
    var noteList by rememberSaveable(result.birthTime) { mutableStateOf<List<String>>(emptyList()) }
    var draftNote by rememberSaveable(result.birthTime) { mutableStateOf("") }
    val buttonWidth = 88.dp
    val buttonHeight = 44.dp
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonWidthPx = with(density) { buttonWidth.toPx() }
    val buttonHeightPx = with(density) { buttonHeight.toPx() }
    val maxNoteButtonX = (screenWidthPx - buttonWidthPx).coerceAtLeast(0f)
    val maxNoteButtonY = (screenHeightPx - buttonHeightPx).coerceAtLeast(0f)
    var noteButtonX by rememberSaveable(result.birthTime) {
        mutableStateOf((screenWidthPx - buttonWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(0f))
    }
    var noteButtonY by rememberSaveable(result.birthTime) {
        mutableStateOf((screenHeightPx - buttonHeightPx - with(density) { 96.dp.toPx() }).coerceAtLeast(0f))
    }

    LaunchedEffect(authSession?.token, userReloadKey) {
        val session = authSession
        currentUser = null
        if (session != null) {
            runCatching { fetchCurrentUser(session.token) }
                .onSuccess { currentUser = it }
        }
    }

    LaunchedEffect(maxNoteButtonX, maxNoteButtonY) {
        noteButtonX = noteButtonX.coerceIn(0f, maxNoteButtonX)
        noteButtonY = noteButtonY.coerceIn(0f, maxNoteButtonY)
    }

    if (showActivateDialog) {
        ActivateVipDialog(
            code = activateCode,
            onCodeChange = { activateCode = it },
            isActivating = isActivating,
            onDismiss = {
                if (!isActivating) {
                    showActivateDialog = false
                }
            },
            onConfirm = {
                val session = authSession
                if (session == null) {
                    showActivateDialog = false
                    Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                    onRequireLogin()
                } else if (activateCode.isBlank()) {
                    Toast.makeText(context, "请输入激活码", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        isActivating = true
                        runCatching { activateLicence(session.token, activateCode) }
                            .onSuccess {
                                Toast.makeText(context, "激活成功", Toast.LENGTH_SHORT).show()
                                activateCode = ""
                                showActivateDialog = false
                                userReloadKey++
                            }
                            .onFailure {
                                Toast.makeText(context, it.userMessage(), Toast.LENGTH_SHORT).show()
                            }
                        isActivating = false
                    }
                }
            }
        )
    }

    if (showNotebookDialog) {
        FourPillarNotebookDialog(
            notes = noteList,
            note = draftNote,
            onNoteChange = { draftNote = it },
            onSave = {
                val newNote = draftNote.trim()
                if (newNote.isNotBlank()) {
                    noteList = noteList + newNote
                    draftNote = ""
                }
                showNotebookDialog = false
            },
            onClear = {
                draftNote = ""
                noteList = emptyList()
                showNotebookDialog = false
            },
            onDismiss = {
                draftNote = ""
                showNotebookDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                RawResultHeader(result)
                InteractiveTreePanel(
                    result = result,
                    isVip = currentUser?.isVip == true,
                    onActivateVip = {
                        if (authSession == null) {
                            Toast.makeText(context, "请先登录后激活 VIP", Toast.LENGTH_SHORT).show()
                            onRequireLogin()
                        } else {
                            showActivateDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
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

        FloatingNotebookButton(
            hasNote = noteList.isNotEmpty(),
            x = noteButtonX,
            y = noteButtonY,
            maxX = maxNoteButtonX,
            maxY = maxNoteButtonY,
            width = buttonWidth,
            height = buttonHeight,
            onPositionChange = { x, y ->
                noteButtonX = x
                noteButtonY = y
            },
            onOpen = {
                draftNote = ""
                showNotebookDialog = true
            }
        )
    }
}

@Composable
private fun ActivateVipDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    isActivating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("激活 VIP", color = RedTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("请输入激活码", color = DarkText, fontSize = 14.sp)
                BasicTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    enabled = !isActivating,
                    singleLine = true,
                    textStyle = TextStyle(color = DarkText, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isActivating) {
                Text(if (isActivating) "激活中" else "激活", color = RedTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isActivating) {
                Text("取消", color = DarkText)
            }
        }
    )
}

@Composable
private fun FourPillarNotebookDialog(
    notes: List<String>,
    note: String,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("四柱记事本", color = RedTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (notes.isEmpty()) {
                    Text("暂无记录", color = Color(0xFF777777), fontSize = 13.sp)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .verticalScroll(rememberScrollState())
                            .border(1.dp, Color(0xFFE0D2BF), RoundedCornerShape(3.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notes.forEachIndexed { index, item ->
                            Text(
                                text = "${index + 1}. $item",
                                color = DarkText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
                Text("当前排盘临时备注", color = DarkText, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                        .padding(10.dp)
                ) {
                    if (note.isBlank()) {
                        Text("点击输入备注", color = Color(0xFF999999), fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        textStyle = TextStyle(color = DarkText, fontSize = 15.sp, lineHeight = 21.sp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存", color = RedTitle)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text("清空", color = DarkText)
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DarkText)
                }
            }
        }
    )
}

@Composable
private fun FloatingNotebookButton(
    hasNote: Boolean,
    x: Float,
    y: Float,
    maxX: Float,
    maxY: Float,
    width: Dp,
    height: Dp,
    onPositionChange: (Float, Float) -> Unit,
    onOpen: () -> Unit
) {
    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)

    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(if (hasNote) Color(0xFFFFF1D6) else RedTitle)
            .border(1.dp, if (hasNote) BrownText else RedTitle, RoundedCornerShape(22.dp))
            .pointerInput(maxX, maxY) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    val touchSlop = viewConfiguration.touchSlop
                    var totalMove = Offset.Zero
                    var dragged = false
                    var currentX = latestX
                    var currentY = latestY

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            if (!dragged) {
                                onOpen()
                            }
                            break
                        }

                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            totalMove += delta
                            if (!dragged && totalMove.getDistance() > touchSlop) {
                                dragged = true
                            }
                            if (dragged) {
                                currentX = (currentX + delta.x).coerceIn(0f, maxX)
                                currentY = (currentY + delta.y).coerceIn(0f, maxY)
                                onPositionChange(currentX, currentY)
                                change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasNote) "已记事" else "记事本",
            color = if (hasNote) BrownText else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RawResultHeader(result: ChartResult) {
    val summary = result.summary
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SummaryLine(
            "姓名：" to Color.Black,
            result.name to DarkText,
            "    五行：" to Color.Black,
            summary.wuxing.summaryValue() to DarkText
        )
        SummaryLine(
            "性别：" to Color.Black,
            summary.gender.ifBlank { result.gender } to DarkText,
            "    胎元：" to Color.Black,
            summary.taiyuan.summaryValue() to GreenText,
            "    命宫：" to Color.Black,
            summary.minggong.summaryValue() to GreenText
        )
        SummaryLine(
            "节气：" to Color.Black,
            summary.solarTerms.summaryValue() to BrownText
        )
        SummaryLine(
            "起运：" to Color.Black,
            summary.startYun.summaryValue() to GreenText,
            "    排盘方式：" to Color.Black,
            result.dateType to DarkText
        )
        SummaryLine(
            "交运：" to Color.Black,
            summary.handoverYun.summaryValue() to BrownText
        )
        SummaryLine(
            "换运：" to Color.Black,
            summary.changeYun.summaryValue() to BrownText
        )
        SummaryLine(
            "公历：" to Color.Black,
            summary.gregorianDatetime.ifBlank { result.birthTime } to BrownText
        )
        SummaryLine(
            "农历：" to Color.Black,
            summary.lunarDatetime.summaryValue() to BrownText,
            summary.zodiac.takeIf { it.isNotBlank() }?.let { "（生肖$it）" }.orEmpty() to BrownText
        )
    }
}

@Composable
private fun SummaryLine(vararg parts: Pair<String, Color>) {
    val annotated = buildAnnotatedString {
        parts.forEach { (text, color) ->
            if (text.isBlank()) return@forEach
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(text)
            }
        }
    }
    Text(annotated, fontSize = 15.sp, lineHeight = 24.sp)
}

private fun String.summaryValue(): String = ifBlank { "暂无" }

@Composable
private fun RawResultText(text: String) {
    Text(
        text = text,
        color = DarkText,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFBF3))
            .border(0.5.dp, Color(0xFFD0A77A))
            .padding(10.dp)
    )
}

@Composable
private fun InteractiveTreePanel(
    result: ChartResult,
    isVip: Boolean,
    onActivateVip: () -> Unit
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    var selectedLuckStart by rememberSaveable(result.birthTime) {
        mutableStateOf(result.luckItems.findLuckForYear(currentYear)?.startYear ?: result.luckItems.firstOrNull()?.startYear)
    }
    var selectedYear by rememberSaveable(result.birthTime) {
        mutableStateOf(result.treeItems.firstOrNull { it.year == currentYear }?.year ?: result.treeItems.firstOrNull()?.year)
    }
    var selectedMonth by rememberSaveable(result.birthTime) { mutableStateOf<Int?>(null) }
    var selectedDay by rememberSaveable(result.birthTime) { mutableStateOf<Int?>(null) }
    var monthItems by remember(result.birthTime) { mutableStateOf<List<BaziTreeItem>>(emptyList()) }
    var dayItems by remember(result.birthTime) { mutableStateOf<List<BaziTreeItem>>(emptyList()) }
    var loadingMonths by rememberSaveable(result.birthTime) { mutableStateOf(false) }
    var loadingDays by rememberSaveable(result.birthTime) { mutableStateOf(false) }
    var errorMessage by rememberSaveable(result.birthTime) { mutableStateOf<String?>(null) }

    LaunchedEffect(result.birthTime, selectedYear) {
        val year = selectedYear ?: return@LaunchedEffect
        loadingMonths = true
        errorMessage = null
        monthItems = emptyList()
        dayItems = emptyList()
        selectedMonth = null
        selectedDay = null
        runCatching {
            fetchBaziTreeItems(
                gender = result.gender,
                dateType = result.dateType,
                birthTime = result.birthTime,
                queryYear = year
            )
        }.onSuccess { items ->
            monthItems = items
            selectedMonth = items.firstOrNull { it.month == currentMonth }?.month ?: items.firstOrNull()?.month
        }.onFailure {
            errorMessage = it.userMessage()
        }
        loadingMonths = false
    }

    LaunchedEffect(result.birthTime, selectedYear, selectedMonth) {
        val year = selectedYear ?: return@LaunchedEffect
        val month = selectedMonth ?: return@LaunchedEffect
        loadingDays = true
        errorMessage = null
        dayItems = emptyList()
        selectedDay = null
        runCatching {
            fetchBaziTreeItems(
                gender = result.gender,
                dateType = result.dateType,
                birthTime = result.birthTime,
                queryYear = year,
                queryMonth = month
            )
        }.onSuccess { items ->
            dayItems = items
            selectedDay = items.firstOrNull()?.idx
        }.onFailure {
            errorMessage = it.userMessage()
        }
        loadingDays = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        errorMessage?.let {
            Text(it, color = RedTitle, fontSize = 13.sp)
        }

        LinkedDetailGrid(
            result = result,
            isVip = isVip,
            yearItem = result.treeItems.firstOrNull { it.year == selectedYear },
            monthItem = monthItems.firstOrNull { it.month == selectedMonth },
            dayItem = dayItems.firstOrNull { it.idx == selectedDay },
            monthItems = monthItems,
            dayItems = dayItems,
            selectedLuckStart = selectedLuckStart,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            loadingMonths = loadingMonths,
            loadingDays = loadingDays,
            onActivateVip = onActivateVip,
            onLuckSelected = { luck ->
                selectedLuckStart = luck.startYear
                selectedYear = if (currentYear in luck.startYear until (luck.startYear + 10)) currentYear else luck.startYear
            },
            onYearSelected = { item -> selectedYear = item.year },
            onMonthSelected = { item -> selectedMonth = item.month },
            onDaySelected = { item -> selectedDay = item.idx }
        )
    }
}

@Composable
private fun TreeSectionTitle(text: String) {
    Text(text, color = RedTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun TreeLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = RedTitle)
        Spacer(modifier = Modifier.width(8.dp))
        Text("加载中", color = DarkText, fontSize = 13.sp)
    }
}

@Composable
private fun TreeEmpty(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(46.dp).background(LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = DarkText, fontSize = 13.sp)
    }
}

@Composable
private fun TreeItemStrip(
    items: List<BaziTreeItem>,
    selectedKey: Int?,
    keyOf: (BaziTreeItem) -> Int?,
    titleOf: (BaziTreeItem) -> String,
    subtitleOf: (BaziTreeItem) -> String,
    onSelect: (BaziTreeItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            val selected = keyOf(item) == selectedKey
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .height(58.dp)
                    .background(if (selected) Color(0xFFE6B67A) else LightGray)
                    .border(0.5.dp, if (selected) RedTitle else Color(0xFFD0D0D0))
                    .clickable { onSelect(item) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(titleOf(item), color = DarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitleOf(item), color = RedTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LinkedDetailGrid(
    result: ChartResult,
    isVip: Boolean,
    yearItem: BaziTreeItem?,
    monthItem: BaziTreeItem?,
    dayItem: BaziTreeItem?,
    monthItems: List<BaziTreeItem>,
    dayItems: List<BaziTreeItem>,
    selectedLuckStart: Int?,
    selectedYear: Int?,
    selectedMonth: Int?,
    selectedDay: Int?,
    loadingMonths: Boolean,
    loadingDays: Boolean,
    onActivateVip: () -> Unit,
    onLuckSelected: (ChartLuckItem) -> Unit,
    onYearSelected: (BaziTreeItem) -> Unit,
    onMonthSelected: (BaziTreeItem) -> Unit,
    onDaySelected: (BaziTreeItem) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val topLabelWidth = 45.dp
    val topCellWidth = ((screenWidth - 20.dp - topLabelWidth) / 8).coerceIn(38.dp, 64.dp)
    val timelineLabelWidth = 45.dp
    val timelineCellWidth = ((screenWidth - 20.dp - timelineLabelWidth) / 10).coerceIn(30.dp, 52.dp)
    val monthCellWidth = ((screenWidth - 20.dp - timelineLabelWidth) / 12).coerceIn(24.dp, 34.dp)
    val dayCellWidth = monthCellWidth
    val luckItem = result.luckItems.firstOrNull { it.startYear == selectedLuckStart }
        ?: result.luckItems.findLuckForYear(yearItem?.year)
    val pillars = listOf(
        result.pillars.year,
        result.pillars.month,
        result.pillars.day,
        result.pillars.hour
    )
    val headers = listOf("日期", "流日", "流月", "流年", "大运", "年柱", "月柱", "日柱", "时柱")
    val dateRow = listOf(
        "岁年",
        dayItem.treeLabel(),
        monthItem.treeLabel(),
        yearItem.treeLabel(),
        luckItem?.let { "${it.age}岁\n${it.startYear}" }.orEmpty(),
    ) + List(4) { "*" }
    val ganRow = listOf(
        "天干",
        dayItem.gan(),
        monthItem.gan(),
        yearItem.gan(),
        luckItem.gan(),
    ) + pillars.map { it.gan }
    val zhiRow = listOf(
        "地支",
        dayItem.zhi(),
        monthItem.zhi(),
        yearItem.zhi(),
        luckItem.zhi(),
    ) + pillars.map { it.zhi }
    val godRow = listOf(
        "十神",
        dayItem.godText(),
        monthItem.godText(),
        yearItem.godText(),
        luckItem?.god.orEmpty(),
    ) + pillars.map { it.ganGod }
    val nayinRow = listOf(
        "纳音",
        dayItem?.nayin.orEmpty(),
        monthItem?.nayin.orEmpty(),
        yearItem?.nayin.orEmpty(),
        luckItem?.state.orEmpty(),
    ) + pillars.map { it.nayin }
    val kongWangRow = listOf(
        "空亡",
        calcXunKong(dayItem?.gz),
        calcXunKong(monthItem?.gz),
        calcXunKong(yearItem?.gz),
        calcXunKong(luckItem?.gz),
    ) + listOf(
        calcXunKong(result.pillars.year.gz()),
        calcXunKong(result.pillars.month.gz()),
        calcXunKong(result.pillars.day.gz()),
        calcXunKong(result.pillars.hour.gz())
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD0A77A))
    ) {
        TableRow(
            cells = headers,
            background = DarkGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = dateRow,
            background = MidGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = ganRow,
            background = LightGray,
            bigIndexes = (1..8).toSet(),
            colors = listOf(DarkText) + ganRow.drop(1).map { stemColor(it) },
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = zhiRow,
            background = MidGray,
            bigIndexes = (1..8).toSet(),
            colors = listOf(DarkText) + zhiRow.drop(1).map { stemColor(it) },
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = nayinRow,
            background = LightGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = kongWangRow,
            background = MidGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = godRow,
            background = LightGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = listOf("藏干", dayItem.hiddenText(), monthItem.hiddenText(), yearItem.hiddenText(), "") + pillars.map { it.hidden.joinToString("\n") },
            background = MidGray,
            height = 78.dp,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = listOf("神煞", dayItem.starText(), monthItem.starText(), yearItem.starText(), luckItem?.stars.orEmpty()) + List(4) { "" },
            background = LightGray,
            height = 96.dp,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )

        Spacer(modifier = Modifier.height(8.dp))
        TimelineRows(
            luckItems = result.luckItems,
            yearItems = result.treeItems,
            monthItems = monthItems,
            dayItems = dayItems,
            tianganNote = calcLiuyi(ganRow.drop(1), tianganOnly = true),
            dizhiNote = calcLiuyi(zhiRow.drop(1), tianganOnly = false),
            dayunShensha = luckItem?.stars.orEmpty(),
            wenChangText = buildWenChangText(result.pillars.day.gan),
            isVip = isVip,
            timelineLabelWidth = timelineLabelWidth,
            timelineCellWidth = timelineCellWidth,
            monthCellWidth = monthCellWidth,
            dayCellWidth = dayCellWidth,
            selectedLuckStart = selectedLuckStart,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            loadingMonths = loadingMonths,
            loadingDays = loadingDays,
            onActivateVip = onActivateVip,
            onLuckSelected = onLuckSelected,
            onYearSelected = onYearSelected,
            onMonthSelected = onMonthSelected,
            onDaySelected = onDaySelected
        )
    }
}

private fun BaziTreeItem?.treeLabel(): String {
    val item = this ?: return ""
    return when {
        item.year != null -> "${item.age ?: "-"}岁\n${item.year}"
        item.month != null -> item.name ?: "${item.month}月"
        item.idx != null -> "${item.name ?: ""}\n${item.idx}日"
        else -> item.name.orEmpty()
    }
}

private fun BaziTreeItem?.hiddenText(): String = this?.cangGanSS?.joinToString("\n").orEmpty()
private fun BaziTreeItem?.starText(): String = this?.sx?.joinToString("\n").orEmpty()
private fun yearAgeText(item: BaziTreeItem): String {
    val age = item.age?.let { "${it}岁" }.orEmpty()
    val year = item.year?.toString().orEmpty()
    return listOf(age, year).filter { it.isNotBlank() }.joinToString("\n")
}

@Composable
private fun TimelineRows(
    luckItems: List<ChartLuckItem>,
    yearItems: List<BaziTreeItem>,
    monthItems: List<BaziTreeItem>,
    dayItems: List<BaziTreeItem>,
    tianganNote: String,
    dizhiNote: String,
    dayunShensha: String,
    wenChangText: String,
    isVip: Boolean,
    timelineLabelWidth: Dp,
    timelineCellWidth: Dp,
    monthCellWidth: Dp,
    dayCellWidth: Dp,
    selectedLuckStart: Int?,
    selectedYear: Int?,
    selectedMonth: Int?,
    selectedDay: Int?,
    loadingMonths: Boolean,
    loadingDays: Boolean,
    onActivateVip: () -> Unit,
    onLuckSelected: (ChartLuckItem) -> Unit,
    onYearSelected: (BaziTreeItem) -> Unit,
    onMonthSelected: (BaziTreeItem) -> Unit,
    onDaySelected: (BaziTreeItem) -> Unit
) {
    val terms = listOf("立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪", "小寒")
    val visibleYears = selectedLuckStart
        ?.let { start -> (start until start + 10).mapNotNull { year -> yearItems.firstOrNull { it.year == year } } }
        .orEmpty()
        .ifEmpty { yearItems.take(10) }
    ClickableTimelineRow(
        label = "",
        items = luckItems.take(9),
        selected = { it.startYear == selectedLuckStart },
        text = { "${it.age}岁\n${it.startYear}" },
        onClick = onLuckSelected,
        background = MidGray,
        height = 56.dp,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth
    )
    ClickableTimelineRow(
        label = "大运",
        items = luckItems.take(9),
        selected = { it.startYear == selectedLuckStart },
        text = { it.gz },
        onClick = onLuckSelected,
        background = LightGray,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth
    )
    ClickableTimelineRow(
        label = "",
        items = visibleYears,
        selected = { it.year == selectedYear },
        text = { yearAgeText(it) },
        onClick = onYearSelected,
        background = MidGray,
        height = 58.dp,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth
    )
    ClickableTimelineRow(
        label = "流年",
        items = visibleYears,
        selected = { it.year == selectedYear },
        text = { it.gz },
        onClick = onYearSelected,
        background = LightGray,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth
    )
    TableRow(
        cells = listOf("") + terms,
        background = MidGray,
        cellWidth = monthCellWidth,
        labelWidth = timelineLabelWidth
    )
    if (loadingMonths) {
        TableRow(cells = listOf("流月", "加载中"), background = LightGray, cellWidth = monthCellWidth, labelWidth = timelineLabelWidth)
    } else {
        ClickableTimelineRow(
            label = "流月",
            items = monthItems.take(12),
            selected = { it.month == selectedMonth },
            text = { it.gz },
            onClick = onMonthSelected,
            background = LightGray,
            cellWidth = monthCellWidth,
            labelWidth = timelineLabelWidth
        )
    }
    if (loadingDays) {
        TableRow(cells = listOf("流日", "加载中"), background = MidGray, cellWidth = dayCellWidth, labelWidth = timelineLabelWidth)
    } else {
        ClickableTimelineRow(
            label = "流日",
            items = dayItems.take(31),
            selected = { it.idx == selectedDay },
            text = { it.gz },
            onClick = onDaySelected,
            background = MidGray,
            height = 52.dp,
            cellWidth = dayCellWidth,
            labelWidth = timelineLabelWidth,
            scrollItems = true
        )
    }
    if (isVip) {
        TableRow(
            cells = listOf("天干留意:", tianganNote),
            background = LightGray,
            height = 42.dp,
            cellWidth = timelineCellWidth * 10,
            labelWidth = timelineLabelWidth
        )
        TableRow(
            cells = listOf("地支留意:", dizhiNote),
            background = Color.White,
            height = 58.dp,
            cellWidth = timelineCellWidth * 10,
            labelWidth = timelineLabelWidth
        )
        TableRow(
            cells = listOf("大运神煞:", dayunShensha),
            background = LightGray,
            height = 58.dp,
            cellWidth = timelineCellWidth * 10,
            labelWidth = timelineLabelWidth
        )
        TableRow(
            cells = listOf("文昌阵:", wenChangText),
            background = Color.White,
            height = 96.dp,
            cellWidth = timelineCellWidth * 10,
            labelWidth = timelineLabelWidth
        )
    } else {
        VipLockedRows(onActivateVip = onActivateVip)
    }
}

@Composable
private fun VipLockedRows(onActivateVip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(254.dp)
            .background(Color(0xFFFFFBF3))
            .border(0.5.dp, Color(0xFFD0A77A))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("开通 VIP 后查看阵法分析", color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = onActivateVip,
                colors = ButtonDefaults.textButtonColors(containerColor = RedTitle, contentColor = Color.White),
                modifier = Modifier
                    .width(138.dp)
                    .height(38.dp)
            ) {
                Text("激活 VIP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val tianganSet = setOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val dizhiSet = setOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

private val liuyiRules = listOf(
    "甲己" to "合土",
    "乙庚" to "合金",
    "丙辛" to "合水",
    "丁壬" to "合木",
    "戊癸" to "合火",
    "甲庚" to "冲",
    "乙辛" to "冲",
    "丙壬" to "冲",
    "丁癸" to "冲",
    "巳申" to "合化水",
    "辰酉" to "合化金",
    "卯戌" to "合化火",
    "寅亥" to "合化木",
    "子丑" to "合化土",
    "午未" to "合化火或土",
    "申子辰" to "合化水",
    "寅午戌" to "合化火",
    "亥卯未" to "合化木",
    "巳酉丑" to "合化金",
    "亥子丑" to "汇聚北方水",
    "寅卯辰" to "汇聚东方木",
    "巳午未" to "汇聚南方火",
    "申酉戌" to "汇聚西方金",
    "子卯" to "为无礼之刑",
    "丑未戌" to "为恃势之刑",
    "寅巳申" to "为无恩之刑",
    "辰辰" to "为自刑",
    "午午" to "为自刑",
    "酉酉" to "为自刑",
    "亥亥" to "为自刑",
    "子午" to "相冲",
    "卯酉" to "相冲",
    "寅申" to "相冲",
    "巳亥" to "相冲",
    "辰戌" to "相冲",
    "丑未" to "相冲",
    "子未" to "相害",
    "丑午" to "相害",
    "寅巳" to "相害",
    "卯辰" to "相害",
    "申亥" to "相害",
    "酉戌" to "相害",
    "寅午" to "暗合土",
    "子巳" to "暗合火",
    "巳酉" to "暗合水",
    "卯申" to "暗合金",
    "亥午" to "暗合木",
    "子酉" to "相破",
    "寅亥" to "相破",
    "卯午" to "相破",
    "辰丑" to "相破",
    "巳申" to "相破",
    "未戌" to "相破"
)

private fun calcLiuyi(values: List<String>, tianganOnly: Boolean): String {
    val usableChars = if (tianganOnly) tianganSet else dizhiSet
    val source = values
        .flatMap { text -> text.map { it.toString() } }
        .filter { it in usableChars }
        .toMutableList()
    if (source.isEmpty()) return ""

    val notes = liuyiRules.mapNotNull { (pattern, suffix) ->
        if (pattern.any { it.toString() !in usableChars }) return@mapNotNull null
        val remaining = source.toMutableList()
        val matched = buildString {
            pattern.forEach { char ->
                val index = remaining.indexOf(char.toString())
                if (index >= 0) append(remaining.removeAt(index))
            }
        }
        if (matched == pattern) "$pattern$suffix" else null
    }
    return notes.distinct().joinToString("; ").let { if (it.isBlank()) "" else "$it;" }
}

private fun buildWenChangText(dayGan: String): String {
    val star = when (dayGan.trim().take(1)) {
        "甲" -> "巳"
        "乙" -> "午"
        "丙", "戊" -> "申"
        "丁", "己" -> "酉"
        "庚" -> "亥"
        "辛" -> "子"
        "壬" -> "寅"
        "癸" -> "卯"
        else -> ""
    }
    if (star.isBlank()) return "日干缺失，无法计算文昌阵"
    val position = when (star) {
        "巳" -> "东南"
        "午" -> "正南"
        "申" -> "西南"
        "酉" -> "正西"
        "亥" -> "西北"
        "子" -> "正北"
        "寅" -> "东北"
        "卯" -> "正东"
        else -> "未知方位"
    }
    val color = when (star) {
        "巳", "午" -> "红色/紫色"
        "申", "酉" -> "金色/白色"
        "亥", "子" -> "黑色/蓝色"
        "寅", "卯" -> "绿色/青色"
        else -> "未知颜色"
    }
    return listOf(
        "本命文昌星：$star",
        "文昌位：$position",
        "文昌塔：1个，颜色：$color",
        "北斗七星灯：1盏，颜色：$color，摆放：文昌塔旁边"
    ).joinToString("\n")
}

@Composable
private fun <T> ClickableTimelineRow(
    label: String,
    items: List<T>,
    selected: (T) -> Boolean,
    text: (T) -> String,
    onClick: (T) -> Unit,
    background: Color,
    height: Dp = 38.dp,
    labelWidth: Dp = 60.dp,
    cellWidth: Dp = 58.dp,
    scrollItems: Boolean = false
) {
    Row(modifier = Modifier.height(height)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .fillMaxSize()
                .background(background)
                .border(0.5.dp, Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = DarkText, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Row(
            modifier = if (scrollItems) {
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            items.forEach { item ->
                TimelineCell(
                    item = item,
                    selected = selected,
                    text = text,
                    onClick = onClick,
                    background = background,
                    cellWidth = cellWidth
                )
            }
        }
    }
}

@Composable
private fun <T> TimelineCell(
    item: T,
    selected: (T) -> Boolean,
    text: (T) -> String,
    onClick: (T) -> Unit,
    background: Color,
    cellWidth: Dp
) {
    val cellText = text(item)
    val isSelected = selected(item)
    Box(
        modifier = Modifier
            .width(cellWidth)
            .fillMaxSize()
            .background(if (isSelected) Color(0xFF9A9A9A) else background)
            .border(0.5.dp, Color.White)
            .clickable { onClick(item) }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cellText,
            color = stemColor(cellText),
            fontSize = if (cellWidth < 32.dp) 10.sp else 11.sp,
            lineHeight = if (cellWidth < 32.dp) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun List<ChartLuckItem>.findLuckForYear(year: Int?): ChartLuckItem? {
    val y = year ?: return null
    return lastOrNull { it.startYear <= y } ?: firstOrNull()
}

private fun BaziTreeItem?.gan(): String = this?.gz?.take(1).orEmpty()
private fun BaziTreeItem?.zhi(): String = this?.gz?.drop(1)?.take(1).orEmpty()
private fun BaziTreeItem?.godText(): String = this?.ss?.replace("/", "\n").orEmpty()
private fun ChartLuckItem?.gan(): String = this?.gz?.take(1).orEmpty()
private fun ChartLuckItem?.zhi(): String = this?.gz?.drop(1)?.take(1).orEmpty()
private fun com.lunar.data.ChartPillar.gz(): String = gan + zhi

private fun calcXunKong(gz: String?): String {
    val value = gz.orEmpty().trim().take(2)
    if (value.length < 2) return ""
    val groups = listOf(
        listOf("甲子", "乙丑", "丙寅", "丁卯", "戊辰", "己巳", "庚午", "辛未", "壬申", "癸酉") to "戌亥",
        listOf("甲戌", "乙亥", "丙子", "丁丑", "戊寅", "己卯", "庚辰", "辛巳", "壬午", "癸未") to "申酉",
        listOf("甲申", "乙酉", "丙戌", "丁亥", "戊子", "己丑", "庚寅", "辛卯", "壬辰", "癸巳") to "午未",
        listOf("甲午", "乙未", "丙申", "丁酉", "戊戌", "己亥", "庚子", "辛丑", "壬寅", "癸卯") to "辰巳",
        listOf("甲辰", "乙巳", "丙午", "丁未", "戊申", "己酉", "庚戌", "辛亥", "壬子", "癸丑") to "寅卯",
        listOf("甲寅", "乙卯", "丙辰", "丁巳", "戊午", "己未", "庚申", "辛酉", "壬戌", "癸亥") to "子丑"
    )
    return groups.firstOrNull { (items, _) -> value in items }?.second.orEmpty()
}

private fun stemColor(text: String): Color {
    return when (text.take(1)) {
        "甲", "乙", "寅", "卯" -> GreenText
        "丙", "丁", "巳", "午" -> RedTitle
        "戊", "己", "辰", "戌", "丑", "未" -> BrownText
        "庚", "辛", "申", "酉" -> OrangeText
        "壬", "癸", "亥", "子" -> LinkBlue
        else -> DarkText
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
        ChartFormScreen(onResult = { _ -> })
    }
}
