package com.lunar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
                CoarseChartPanel(result)
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
                Spacer(modifier = Modifier.height(10.dp))
                PromoDivider()
                Spacer(modifier = Modifier.height(10.dp))
                DayunDetailPanel(result)
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
internal fun ActivateVipDialog(
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
private fun ShenshaDetailDialog(name: String, onDismiss: () -> Unit) {
    val info = SHENSHA_DATA[name] ?: return
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            Column {
                // 标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = info.name,
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF1A1A1A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "×",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { onDismiss() }
                            .padding(4.dp),
                        color = Color(0xFF888888),
                        fontSize = 22.sp,
                        lineHeight = 22.sp
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE0E0E0)))
                // 正文（可滚动）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("精评", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(info.summary, color = Color(0xFF1A1A1A), fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("古诀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(info.formula, color = Color(0xFF1A1A1A), fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("查法", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(info.lookup, color = Color(0xFF1A1A1A), fontSize = 14.sp, lineHeight = 22.sp)
                    Text(info.detail, color = Color(0xFF1A1A1A), fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }
    }
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
private fun CoarseNoteRow(label: String, value: String, background: Color) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .border(0.5.dp, Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontSize = 13.sp, color = DarkText)
    }
}

@Composable
private fun PromoDivider() {
    Image(
        painter = painterResource(com.lunar.R.drawable.promo_banner),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth()
    )
}

// 袁天罡称骨：4 张表查重，按总重查评语
private val YEAR_BONE_FEN = mapOf(
    "甲子" to 12,"乙丑" to 9,"丙寅" to 6,"丁卯" to 7,"戊辰" to 12,"己巳" to 5,
    "庚午" to 9,"辛未" to 8,"壬申" to 7,"癸酉" to 8,
    "甲戌" to 15,"乙亥" to 9,"丙子" to 16,"丁丑" to 8,"戊寅" to 8,"己卯" to 19,
    "庚辰" to 12,"辛巳" to 6,"壬午" to 8,"癸未" to 7,
    "甲申" to 5,"乙酉" to 15,"丙戌" to 6,"丁亥" to 16,"戊子" to 15,"己丑" to 7,
    "庚寅" to 9,"辛卯" to 12,"壬辰" to 10,"癸巳" to 7,
    "甲午" to 15,"乙未" to 6,"丙申" to 5,"丁酉" to 14,"戊戌" to 14,"己亥" to 9,
    "庚子" to 7,"辛丑" to 7,"壬寅" to 9,"癸卯" to 12,
    "甲辰" to 8,"乙巳" to 7,"丙午" to 13,"丁未" to 5,"戊申" to 14,"己酉" to 5,
    "庚戌" to 9,"辛亥" to 17,"壬子" to 5,"癸丑" to 7,
    "甲寅" to 12,"乙卯" to 8,"丙辰" to 8,"丁巳" to 6,"戊午" to 19,"己未" to 6,
    "庚申" to 8,"辛酉" to 16,"壬戌" to 10,"癸亥" to 6
) // 单位：分（1钱=10分，1两=10钱）
private val MONTH_BONE_FEN = listOf(0, 60, 70, 180, 90, 50, 160, 90, 150, 180, 80, 90, 50)
private val DAY_BONE_FEN = listOf(
    0, 50,100,80,150,160,150,80,160,80,160,
    90,170,80,170,100,80,90,180,50,150,
    100,90,80,90,150,180,70,80,160,60
)
private val HOUR_BONE_FEN = mapOf(
    "子" to 160,"丑" to 60,"寅" to 70,"卯" to 100,"辰" to 90,"巳" to 160,
    "午" to 100,"未" to 80,"申" to 80,"酉" to 90,"戌" to 60,"亥" to 60
)
private val LUNAR_MONTH_NAMES = mapOf(
    "正月" to 1,"一月" to 1,"二月" to 2,"三月" to 3,"四月" to 4,"五月" to 5,"六月" to 6,
    "七月" to 7,"八月" to 8,"九月" to 9,"十月" to 10,"冬月" to 11,"十一月" to 11,"腊月" to 12,"十二月" to 12
)
private val LUNAR_DAY_NAMES = mapOf(
    "初一" to 1,"初二" to 2,"初三" to 3,"初四" to 4,"初五" to 5,"初六" to 6,"初七" to 7,"初八" to 8,"初九" to 9,"初十" to 10,
    "十一" to 11,"十二" to 12,"十三" to 13,"十四" to 14,"十五" to 15,"十六" to 16,"十七" to 17,"十八" to 18,"十九" to 19,"二十" to 20,
    "廿一" to 21,"廿二" to 22,"廿三" to 23,"廿四" to 24,"廿五" to 25,"廿六" to 26,"廿七" to 27,"廿八" to 28,"廿九" to 29,"三十" to 30
)

private fun parseLunar(s: String): Triple<Int, Int, String>? {
    // 形如 "2026 年四月十六 子时"
    val m = Regex("(.+?)月(.+?)\\s*(.)时").find(s) ?: return null
    val mo = LUNAR_MONTH_NAMES["${m.groupValues[1].takeLast(2)}月"] ?: LUNAR_MONTH_NAMES["${m.groupValues[1].takeLast(1)}月"] ?: return null
    val dy = LUNAR_DAY_NAMES[m.groupValues[2].trim()] ?: return null
    val hr = m.groupValues[3]
    return Triple(mo, dy, hr)
}

private val BONE_VERDICTS: Map<String, String> = mapOf(
    "2两1钱" to "短命非业谓大空，灵魂入夜归阴去，命运不好少福禄，行善积德子孙旺。",
    "2两2钱" to "身寒骨冷苦伶仃，此命推来行乞人，劳劳碌碌无度日，终生困苦泪淋淋。",
    "2两3钱" to "此命推来骨格轻，求谋作事事难成，妻儿兄弟应难许，别处他乡作散人。",
    "2两4钱" to "此命推来福禄无，门庭困苦总难荣，六亲骨肉皆无靠，流到他乡作老翁。",
    "2两5钱" to "此命推来事不成，知识同行有误人，妻儿兄弟难依靠，奔走他乡作散人。",
    "2两6钱" to "平生衣禄苦中求，独自经营事不休，离祖出门宜早计，晚来衣禄自无忧。",
    "2两7钱" to "一生作事少商量，难靠祖宗作主张，独马单枪空做去，早来晚岁总无长。",
    "2两8钱" to "一生作事似飘蓬，祖宗产业在梦中，若不过房改名姓，也当移徙二三通。",
    "2两9钱" to "初年运限未曾享，纵有功名在后头，须过四旬方可立，移居改姓事休休。",
    "3两" to "劳劳碌碌苦中求，东奔西走何日休，若使终身勤与俭，老来稍可免忧愁。",
    "3两1钱" to "忙忙碌碌苦中求，何日云开见日头，难得祖基家可立，中年衣食渐无忧。",
    "3两2钱" to "初年作事事难成，欲靠功名身不闲，难把瓢儿来比挂，得抛得歇渐无忧。",
    "3两3钱" to "早年做事事难成，百年勤劳枉费心，半世自如流水去，后来运到得黄金。",
    "3两4钱" to "此命福气果如何，僧道门中衣禄多，离祖出家方得稳，终朝拜佛念弥陀。",
    "3两5钱" to "生平福量不周全，祖业根基觉少传，营事生涯宜守旧，时来衣食胜从前。",
    "3两6钱" to "不须劳碌过平生，独自成家显六亲，离祖出门宜早计，晚来衣禄自然增。",
    "3两7钱" to "此命般般事不成，弟兄少力自孤行，虽然祖业须微有，来寻盛少又消停。",
    "3两8钱" to "一生骨肉最清高，早入皇都姓名标，待看年将三十六，蓝衫脱去换红袍。",
    "3两9钱" to "此命终身运不通，劳劳作事尽皆空，苦心竭力成家计，到得那时在梦中。",
    "4两" to "平生衣禄是绵长，件件心中自主张，前面风霜多受过，从来必定享安康。",
    "4两1钱" to "此命推来旺末年，妻荣子贵自怡然，平生原有滔滔福，可有財也有錢。",
    "4两2钱" to "得宽怀处且宽怀，何用双眉皱不开，若使中年命运济，那时名利一齐来。",
    "4两3钱" to "为人心性最聪明，作事轩昂近贵人，衣禄一生天数定，不须劳碌是丰享。",
    "4两4钱" to "万事由天莫苦求，须知福禄自悠悠，平生且听天公定，富贵荣华迟早来。",
    "4两5钱" to "名利推求竟若何，前番辛苦后奔波，命中难养男与女，骨肉扶持也不多。",
    "4两6钱" to "东西南北尽皆通，出姓移名更觉隆，衣禄无亏天数定，中年晚景一般同。",
    "4两7钱" to "此命推来事不同，为人能干异凡庸，中年还有逍遥福，不比前番目下穷。",
    "4两8钱" to "幼年运道未曾享，没有功名在后头，须过四旬方成器，得来衣禄胜前番。",
    "4两9钱" to "此命推来福不轻，自成自立显门庭，从来富贵人钦敬，使婢差奴过一生。",
    "5两" to "为人作事自勤劳，傲气凌人胆气豪，富贵荣华来祖业，平生衣禄自坚牢。",
    "5两1钱" to "一世荣华事事通，不须劳碌自亨通，弟兄叔侄皆如意，家业丰隆自始终。",
    "5两2钱" to "一世享通事事能，不须劳碌自然丰，宗祖产业留得多，安享荣华自如愿。",
    "5两3钱" to "此命福气大不同，公侯卿相在其中，钱粮家有数千石，门前奴仆唤西东。",
    "5两4钱" to "此格推来礼仪通，一生福禄用无穷，甜酸苦辣皆尝过，财源滚滚似春风。",
    "5两5钱" to "走马扬鞭争名利，少年作事费筹论，一朝福禄源源至，富贵荣华显六亲。",
    "5两6钱" to "此格推来福泽宏，兴家立业在其中，一生衣禄安排定，却是人间一福翁。",
    "5两7钱" to "福禄丰盈万事全，一生荣耀显双亲，名扬威振人钦敬，处世逍遥似遇春。",
    "5两8钱" to "平生福禄自然来，名利兼全福禄偕，雁塔提名为贵客，紫袍金带走金阶。",
    "5两9钱" to "细推此命福不轻，安富尊荣享现成，员外科甲虽不就，财源也有数千金。",
    "6两" to "一朝金榜快提名，显祖荣宗官爵明，一生衣食皆无亏，回头还家拜祖宗。",
    "6两1钱" to "不做朝中金榜客，定为世上大财翁，聪明天赋经书熟，名显皇都自是荣。",
    "6两2钱" to "此命生来福不穷，读书必定显亲宗，紫衣金带为卿相，富贵荣华皆可同。",
    "6两3钱" to "命主为官福禄长，得来富贵实非常，名题雁塔传金榜，定中高高第一名。",
    "6两4钱" to "此格人间一福人，堆金积玉满堂春，从来富贵由天定，不须辛苦自然能。",
    "6两5钱" to "细推此命福非轻，富贵荣华孰得均，定有刺史巡按格，安享高官受国恩。",
    "6两6钱" to "此命生来福自宏，田园家业最高隆，平生衣禄丰盈足，一世荣华万事通。",
    "6两7钱" to "此命生来福不轻，封妻荫子受皇恩，世代受贵守原典，腰身常带紫金鱼。",
    "6两8钱" to "富贵由天莫苦求，万事乘除天作主，将相公侯天上有，何须用力强为吏。",
    "6两9钱" to "君是人间衣禄星，一生富贵众人钦，纵然福禄由天定，安享荣华过一生。",
    "7两" to "此命生成大不同，公侯卿相在其中，一生自有逍遥福，富贵荣华极品隆。",
    "7两1钱" to "此命推来福不同，一生富贵众人钦，定为社稷栋梁臣，一身荣耀显门庭。"
)

private fun calcBoneWeight(result: ChartResult): Pair<String, String>? {
    val yearGz = result.pillars.year.gan + result.pillars.year.zhi
    val hourZhi = result.pillars.hour.zhi
    val lunar = parseLunar(result.summary.lunarDatetime) ?: return null
    val (mo, dy, _) = lunar
    val y = YEAR_BONE_FEN[yearGz] ?: return null
    val m = MONTH_BONE_FEN.getOrElse(mo) { return null }
    val d = DAY_BONE_FEN.getOrElse(dy) { return null }
    val h = HOUR_BONE_FEN[hourZhi] ?: return null
    val total = y + m + d + h
    val liang = total / 100
    val qian = (total % 100) / 10
    val fen = total % 10
    val cnNum = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    val weight = buildString {
        append(cnNum[liang]).append("两")
        if (qian > 0) append(cnNum[qian]).append("钱")
        if (fen > 0) append(cnNum[fen]).append("分")
    }
    val key = if (fen > 0) "${liang}两${qian}钱${fen}分" else "${liang}两${qian}钱"
    val verdict = BONE_VERDICTS[key] ?: BONE_VERDICTS["${liang}两${qian}钱"].orEmpty()
    return weight to verdict
}

private fun ganzhiOfYear(year: Int): String {
    val offset = ((year - 1984) % 60 + 60) % 60
    return TIANGAN[offset % 10] + DIZHI[offset % 12]
}

/** 每格按干支拆成 stem+branch，分别按五行配色。vertical=true 竖排（上干下支），否则横排。 */
@Composable
private fun GanZhiTableRow(
    label: String,
    gzCells: List<String>,
    background: Color,
    height: Dp,
    labelWidth: Dp,
    cellWidth: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    vertical: Boolean,
    overrideColor: Color? = null
) {
    Row(modifier = Modifier.height(height)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .fillMaxSize()
                .background(background)
                .border(0.5.dp, Color.White)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = DarkText, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        gzCells.forEach { gz ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxSize()
                    .background(background)
                    .border(0.5.dp, Color.White)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (gz.length == 2) {
                    val stem = gz[0].toString()
                    val branch = gz[1].toString()
                    val stemC = overrideColor ?: stemColor(stem)
                    val branchC = overrideColor ?: stemColor(branch)
                    if (vertical) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stem, color = stemC, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold)
                            Text(branch, color = branchC, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stem, color = stemC, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold)
                            Text(branch, color = branchC, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(gz, color = overrideColor ?: DarkText, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun DayunDetailPanel(result: ChartResult) {
    val luckItems = result.luckItems
    if (luckItems.isEmpty()) return
    val dayGan = result.pillars.day.gan
    val configuration = LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val labelW = 48.dp
    val cellW = ((screenW - 20.dp - labelW) / luckItems.size).coerceAtLeast(32.dp)
    val fs = 10.sp
    val lh = 12.sp

    fun verticalChars(s: String) = s.toCharArray().joinToString("\n")

    val ageRow = listOf("岁年:") + luckItems.map { "%02d岁".format(it.age) }
    val startYearRow = listOf("大运始于:") + luckItems.map { it.startYear.toString() }
    val ganGodRow = listOf("天干十神:") + luckItems.map {
        verticalChars((it.god.split("/").firstOrNull() ?: "").trim())
    }
    val dayunGz = luckItems.map { it.gz }
    val dzGodRow = listOf("地支十神:") + luckItems.map { li ->
        val zhi = li.gz.drop(1).take(1)
        val idx = DIZHI.indexOf(zhi)
        if (idx < 0) "" else verticalChars(
            DZ_CANGGAN[idx].map { ch ->
                val g = ch.toString()
                "$g${getShishen(g, dayGan)}"
            }.joinToString("")
        )  
    }
    val stateRow = listOf("十二长生:") + luckItems.map { it.state }
    val endYearRow = listOf("大运止于:") + luckItems.map { (it.startYear + 9).toString() }
    val liunianGzPerRow = (0..9).map { i ->
        luckItems.map { li -> ganzhiOfYear(li.startYear + i) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, Color(0xFFD0A77A))
    ) {
        TableRow(ageRow, MidGray, height = 26.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        TableRow(startYearRow, LightGray, height = 26.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        TableRow(ganGodRow, MidGray, height = 36.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        GanZhiTableRow(
            label = "大运:",
            gzCells = dayunGz,
            background = LightGray,
            height = 36.dp,
            labelWidth = labelW,
            cellWidth = cellW,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            vertical = false,
            overrideColor = RedTitle
        )
        TableRow(dzGodRow, MidGray, height = 110.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        TableRow(stateRow, LightGray, height = 26.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        TableRow(endYearRow, MidGray, height = 26.dp, cellWidth = cellW, labelWidth = labelW, fontSize = fs, lineHeight = lh)
        liunianGzPerRow.forEachIndexed { i, gzs ->
            GanZhiTableRow(
                label = if (i == 0) "流年:" else "",
                gzCells = gzs,
                background = if (i % 2 == 0) LightGray else MidGray,
                height = 32.dp,
                labelWidth = labelW,
                cellWidth = cellW,
                fontSize = fs,
                lineHeight = lh,
                vertical = true,
                overrideColor = DarkText
            )
        }
    }
}

@Composable
private fun CoarseChartPanel(result: ChartResult) {
    val configuration = LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val labelW = 48.dp
    val cellW = ((screenW - 20.dp - labelW) / 4).coerceIn(60.dp, 140.dp)
    val pillars = listOf(result.pillars.year, result.pillars.month, result.pillars.day, result.pillars.hour)
    val dayGan = result.pillars.day.gan
    val isFemale = result.gender.contains("女")

    // 藏干 cell: 每个地支按 DZ_CANGGAN 展开 → "干（十神）" 多行
    val hiddenCells = pillars.map { p ->
        val idx = DIZHI.indexOf(p.zhi)
        if (idx < 0) "" else DZ_CANGGAN[idx].map { ch ->
            val g = ch.toString()
            "$g（${getShishen(g, dayGan)}）"
        }.joinToString("\n")
    }

    // 神煞从 rawText 抽取，保留为列表方便逐项点击
    val shenshaLists = listOf("年柱", "月柱", "日柱", "时柱").map { p ->
        Regex("【${p}神煞】：([^\\n]+)").find(result.rawText)?.groupValues?.getOrNull(1)
            ?.split("、")?.filter { it.isNotBlank() }.orEmpty()
    }
    var selectedShensha by remember { mutableStateOf<String?>(null) }
    if (selectedShensha != null) {
        ShenshaDetailDialog(name = selectedShensha!!, onDismiss = { selectedShensha = null })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD0A77A))
    ) {
        TableRow(
            cells = listOf("四柱:", "年柱", "月柱", "日柱", "时柱"),
            background = DarkGray, cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("十神:") + pillars.mapIndexed { i, p ->
                if (i == 2) (if (isFemale) "元女" else "元男") else p.ganGod
            },
            background = LightGray, cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("天干:") + pillars.map { it.gan },
            background = MidGray,
            bigIndexes = (1..4).toSet(),
            colors = listOf(DarkText) + pillars.map { stemColor(it.gan) },
            cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("地支:") + pillars.map { it.zhi },
            background = LightGray,
            bigIndexes = (1..4).toSet(),
            colors = listOf(DarkText) + pillars.map { stemColor(it.zhi) },
            cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("藏干:") + hiddenCells,
            background = MidGray, height = 78.dp,
            cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("纳音:") + pillars.map { it.nayin },
            background = LightGray, cellWidth = cellW, labelWidth = labelW
        )
        TableRow(
            cells = listOf("空亡:") + pillars.map { calcXunKong(it.gz()) },
            background = MidGray, cellWidth = cellW, labelWidth = labelW
        )
        ShenshaTableRow(
            shenshaPerColumn = shenshaLists,
            background = LightGray,
            cellWidth = cellW,
            labelWidth = labelW,
            onShenshaClick = { selectedShensha = it }
        )
        val tgLiuyi = calcLiuyi(pillars.map { it.gan }, tianganOnly = true)
        val dzLiuyi = calcLiuyi(pillars.map { it.zhi }, tianganOnly = false)
        CoarseNoteRow("天干留意:", tgLiuyi, MidGray)
        CoarseNoteRow("地支留意:", dzLiuyi, LightGray)
        val bone = calcBoneWeight(result)
        if (bone != null) {
            CoarseNoteRow("称骨重量:", bone.first, MidGray)
            CoarseNoteRow("称骨评语:", bone.second, LightGray)
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    PromoDivider()
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun RawResultHeader(result: ChartResult) {
    val summary = result.summary
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
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
    Text(annotated, fontSize = 15.sp, lineHeight = 20.sp)
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
    // 缓存：避免重复点流年/流月刷接口造成 UI 闪动
    val monthCache = remember(result.birthTime) { mutableStateMapOf<Int, List<BaziTreeItem>>() }
    val dayCache = remember(result.birthTime) { mutableStateMapOf<Pair<Int, Int>, List<BaziTreeItem>>() }

    LaunchedEffect(result.birthTime, selectedYear) {
        val year = selectedYear ?: return@LaunchedEffect
        errorMessage = null
        val cached = monthCache[year]
        if (cached != null) {
            monthItems = cached
            selectedMonth = cached.firstOrNull { it.month == currentMonth }?.month ?: cached.firstOrNull()?.month
            return@LaunchedEffect
        }
        loadingMonths = true
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
            monthCache[year] = items
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
        errorMessage = null
        val key = year to month
        val cached = dayCache[key]
        if (cached != null) {
            dayItems = cached
            selectedDay = cached.firstOrNull()?.idx
            return@LaunchedEffect
        }
        loadingDays = true
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
            dayCache[key] = items
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
    val riganMap = rememberRiganMap()
    val dayGanChar = result.pillars.day.gan.take(1)
    val lunarMonth = parseLunar(result.summary.lunarDatetime)?.first
    val riganGeneral = riganMap[dayGanChar]?.get(0)?.translate.orEmpty()
    val riganGeneralOriginal = riganMap[dayGanChar]?.get(0)?.original.orEmpty()
    val riganMonthContent = lunarMonth?.let { riganMap[dayGanChar]?.get(it) }
    val riganMonthOriginal = riganMonthContent?.original.orEmpty()
    val riganMonthText = riganMonthContent?.translate.orEmpty()
    val riganMonthSource = riganMonthContent?.source.orEmpty()
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
    val dayGan = result.pillars.day.gan
    val isFemale = result.gender.contains("女")
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
    val ganCells = listOf(StackedCell("天干", isLabel = true)) +
        ganRow.drop(1).mapIndexed { idx, g ->
            val isDayPillar = idx == 6 // 第 6 个（label 之外）= 日柱
            val sub = when {
                g.isEmpty() -> ""
                isDayPillar -> if (isFemale) "元女" else "元男"
                else -> shishenShort(g, dayGan)
            }
            StackedCell(main = g, sub = sub, mainColor = stemColor(g))
        }
    val zhiCells = listOf(StackedCell("地支", isLabel = true)) +
        zhiRow.drop(1).map { z ->
            StackedCell(main = z, sub = dzShishenShort(z, dayGan), mainColor = stemColor(z))
        }
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
        StackedTableRow(
            cells = ganCells,
            background = LightGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        StackedTableRow(
            cells = zhiCells,
            background = MidGray,
            cellWidth = topCellWidth,
            labelWidth = topLabelWidth
        )
        TableRow(
            cells = kongWangRow,
            background = LightGray,
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
            riganGeneral = riganGeneral,
            riganGeneralOriginal = riganGeneralOriginal,
            riganMonthOriginal = riganMonthOriginal,
            riganMonthText = riganMonthText,
            riganMonthSource = riganMonthSource,
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
    riganGeneral: String,
    riganGeneralOriginal: String,
    riganMonthOriginal: String,
    riganMonthText: String,
    riganMonthSource: String,
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
    val luckScrollState = rememberScrollState()
    val visibleYears = selectedLuckStart
        ?.let { start -> (start until start + 10).mapNotNull { year -> yearItems.firstOrNull { it.year == year } } }
        .orEmpty()
        .ifEmpty { yearItems.take(10) }
    ClickableTimelineRow(
        label = "",
        items = luckItems,
        selected = { it.startYear == selectedLuckStart },
        text = { "${it.age}岁\n${it.startYear}" },
        onClick = onLuckSelected,
        background = MidGray,
        height = 56.dp,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth,
        scrollItems = true,
        scrollState = luckScrollState
    )
    ClickableTimelineRow(
        label = "大运",
        items = luckItems,
        selected = { it.startYear == selectedLuckStart },
        text = { it.gz },
        onClick = onLuckSelected,
        background = LightGray,
        cellWidth = timelineCellWidth,
        labelWidth = timelineLabelWidth,
        scrollItems = true,
        scrollState = luckScrollState
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
        CoarseNoteRow("日干总论:", riganGeneralOriginal, MidGray)
        CoarseNoteRow("总论译文:", riganGeneral, LightGray)
        CoarseNoteRow("日干月论:", riganMonthOriginal, MidGray)
        CoarseNoteRow("月论译文:", riganMonthText, LightGray)
        CoarseNoteRow("出处:", riganMonthSource, MidGray)
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
    scrollItems: Boolean = false,
    scrollState: ScrollState? = null
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
                    .horizontalScroll(scrollState ?: rememberScrollState())
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
    val fs = if (cellWidth < 32.dp) 10.sp else 11.sp
    val lh = if (cellWidth < 32.dp) 12.sp else 14.sp
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
        if (isGanZhiPair(cellText)) {
            val stem = cellText[0].toString()
            val branch = cellText[1].toString()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stem, color = stemColor(stem), fontSize = fs, lineHeight = lh, fontWeight = FontWeight.Bold)
                Text(branch, color = stemColor(branch), fontSize = fs, lineHeight = lh, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = cellText,
                color = stemColor(cellText),
                fontSize = fs,
                lineHeight = lh,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun isGanZhiPair(s: String): Boolean =
    s.length == 2 && s[0].toString() in TIANGAN && s[1].toString() in DIZHI

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

private val TIANGAN = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val DIZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val SS_SHORTER = mapOf(
    "比肩" to "比", "劫财" to "劫", "正印" to "印", "偏印" to "枭",
    "正官" to "官", "七杀" to "杀", "正财" to "财", "偏财" to "才",
    "伤官" to "伤", "食神" to "食"
)
// 地支藏干（顺序：子丑寅卯辰巳午未申酉戌亥）
private val DZ_CANGGAN = listOf(
    "癸", "己癸辛", "甲丙戊", "乙", "戊乙癸", "丙戊庚",
    "丁己", "己丁乙", "庚壬戊", "辛", "戊辛丁", "壬甲"
)

private data class ShenshaInfo(val name: String, val summary: String, val formula: String, val lookup: String, val detail: String)

private val SHENSHA_DATA: Map<String, ShenshaInfo> = mapOf(
    "天乙贵人" to ShenshaInfo("天乙贵人", "一生人缘佳，遇事有人解救危难，化险为夷。", "甲戊庚牛羊，乙己鼠猴乡，丙丁猪鸡位，壬癸兔蛇藏，六辛逢虎马，此是贵人方。", "以日干或年干查四地支。甲戊庚见丑未；乙己见子申；丙丁见亥酉；壬癸见巳卯；辛见午寅。", "天乙贵人是命中最吉之神，所到之处一切凶煞隐然而避。命带天乙贵人者，心性聪明，出入近贵，一生少病，人缘极佳。大运流年逢之，有生官发财之机。女命天乙贵人入命且日主自坐二德者，可嫁贵夫。"),
    "太极贵人" to ShenshaInfo("太极贵人", "聪明好学，喜神秘事物（与华盖并用，又临戌亥，在易学方面多有成就）。", "甲乙生人子午中，丙丁鸡兔定亨通，戊己两干临四季，庚辛寅亥禄丰隆，壬癸巳申偏喜美，值此应当福气钟，更须贵格来相扶，候封万户到三公。", "以日干或年干查四地支：甲乙见子或午；丙丁见卯或酉；戊己见辰戌丑未；庚辛见寅或亥；壬癸见巳或申。", "太极贵人是造物之初、造化始终相保之贵。命带太极贵人者，聪明好学，有钻研精神，做事有始有终，为人正直，喜文史哲宗教。如得生旺及有贵格吉星相扶，主气宇轩昂，福寿双全，富贵人间。"),
    "天德贵人" to ShenshaInfo("天德贵人", "具有天地德秀之气，为逢凶化吉之神，主福寿。", "正丁二坤中，三壬四辛同，五乾六甲上，七癸八寅同，九丙十归乙，子巽丑庚中。", "以月支查四柱天干地支：正月见丁，二月见申，三月见壬，四月见辛，五月见亥，六月见甲，七月见癸，八月见寅，九月见丙，十月见乙，十一月见巳，十二月见庚。", "天德贵人是祖先积德余荫遗留给子孙的福德，可化解各种凶煞。命带天德者，心性善良，忠孝贤能，一生吉利，荣华富贵。"),
    "月德贵人" to ShenshaInfo("月德贵人", "太阴之德，功能与天德略同，主福分深厚，能逢凶化吉。", "寅午戌月见丙，申子辰月见壬，亥卯未月见甲，巳酉丑月见庚。", "以月支查四柱天干：寅午戌月见丙；申子辰月见壬；亥卯未月见甲；巳酉丑月见庚。", "月德贵人以月支为主，为解救之神，遇危难时可幸免于难。命带月德者，吉命增吉，凶命减凶，为人多仁慈敏慧。虽有枭杀伤劫，也可以逢凶化吉。但最忌逢刑冲，遇之则无力化解。"),
    "德秀贵人" to ShenshaInfo("德秀贵人", "仪容清秀，温柔爽朗，涵养出众，很有才华。", "寅午戌月，丙丁为德，戊癸为秀；申子辰月，壬癸戊己为德，丙辛甲己为秀；巳酉丑月，庚辛为德，乙庚为秀；亥卯未月，甲乙为德，丁壬为秀。", "以生月为主查四柱天干。寅午戌月天干见丙丁为德，见戊癸合为秀；申子辰月见壬癸戊己为德，见丙辛合或甲己合为秀；巳酉丑月见庚辛为德，见乙庚合为秀；亥卯未月见甲乙为德，见丁壬合为秀。", "德者阴阳解凶之神，秀者天地清秀之气。命带德秀贵人且无冲破克压者，聪明晓事，温厚和气，文业通达，逢凶化吉。若遇学堂，更带财官，主贵。多可能在公检法或事业单位工作。"),
    "天德合" to ShenshaInfo("天德合", "与天德贵人同功，虽稍逊，亦有化解凶厄之力。", "正月见壬，二月见巳，三月见丁，四月见丙，五月见寅，六月见己，七月见戊，八月见亥，九月见辛，十月见庚，十一月见申，十二月见乙。", "天德与天干五合或地支六合者即为天德合。正月天德丁，壬与丁合；二月天德申，巳与申合；三月天德壬，丁与壬合；以此类推。", "天德合是与天德贵人相合的神煞，如没有天德贵人，有天德合也可起到天德贵人的作用。吉庆程度比天德稍次，但仍主吉上加吉、福泽加倍。"),
    "月德合" to ShenshaInfo("月德合", "与月德贵人同功，能解百祸，主贵人相助。", "正月见辛，二月见己，三月见丁，四月见乙，五月见辛，六月见己，七月见丁，八月见乙，九月见辛，十月见己，十一月见丁，十二月见乙。", "月德与天干五合者即为月德合。正月月德丙，丙与辛合；二月月德甲，甲与己合；三月月德壬，壬与丁合；以此类推。", "月德合是与月德贵人相合的神煞，作用同月德，但吉庆程度稍次。入课主解百祸，有贵人帮扶。在择日中，月德合日为吉日。"),
    "福星贵人" to ShenshaInfo("福星贵人", "主一生福禄安康，衣食无忧，遇事多得贵人相助。", "甲丙相邀入虎乡，更游鼠穴最高强，戊猴己未丁宜亥，乙癸逢牛卯禄昌，庚赶马头辛到巳，壬骑龙背喜非常。", "以年干或日干查四柱地支。甲丙见寅或子；戊见申；己见未；丁见亥；乙癸见丑或卯；庚见午；辛见巳；壬见辰。", "福星贵人是福气之所在，人命逢之，一生衣食无忧，多遇好事，生活安稳富足，少有大的灾祸。与文昌、学堂并见，更利学业功名。"),
    "文昌贵人" to ShenshaInfo("文昌贵人", "聪明过人，才华横溢，利于考试和文化事业。", "甲乙巳午丙戊申，丁己鸡位庚猪辛鼠壬虎癸卯。", "以日干查四地支：甲见巳、乙见午、丙戊见申、丁己见酉、庚见亥、辛见子、壬见寅、癸见卯。", "文昌者，食神之临官所在，为建禄之称。命带文昌贵人，主天资聪明，文笔好，记性好，举止文雅，好学上进，一生近官利贵。文昌坐于喜用神且生旺，多能取得高学历。"),
    "学堂" to ShenshaInfo("学堂", "主学业功名，聪明智巧，登科及第，学业大展宏图。", "金命见巳，辛巳为正；木命见亥，己亥为正；水命见申，甲申为正；火命见寅，丙寅为正；土命见申，戊申为正。", "以年纳音查月日时支。年柱纳音五行：金命见巳，木命见亥，水命见申，火命见寅，土命见申。", "学堂如人读书在学堂，是文星所在，主学业功名之事。此星入命，主人聪明，可登科及第，文章出众。学堂为用神，岁运逢之，利考学。"),
    "词馆" to ShenshaInfo("词馆", "文章冠世，聪明智巧，一生富贵，声名远播。", "甲干见庚寅，乙干见辛卯；丙干见乙巳，丁干见戊午；戊干见丁巳，己干见庚午；庚干见壬申，辛干见癸酉；壬干见癸亥，癸干见壬戌。", "以日干查四柱地支。甲见庚寅，乙见辛卯，丙见乙巳，丁见戊午，戊见丁巳，己见庚午，庚见壬申，辛见癸酉，壬见癸亥，癸见壬戌。", "词馆者，如今之翰林院，取学业精专、文章出类之意。词馆入命，主人秀气生发，聪明智巧，文章冠世，一生富贵。宜生旺，不宜克害冲破。"),
    "魁罡" to ShenshaInfo("魁罡", "聪明刚烈，性急好胜，不喜受人约束。", "壬辰、庚辰、庚戌、戊戌四日。", "以日柱查。日柱为壬辰、庚辰、庚戌、戊戌之一者即为魁罡。", "魁罡乃天罡地煞之星，命带魁罡者，性急刚烈，聪明果断，好胜心强，不喜受人约束。若行运得宜，多主有权势。男命魁罡多为掌权之人，女命魁罡则婚姻多波折。"),
    "国印贵人" to ShenshaInfo("国印贵人", "主诚实可靠，有掌印之权，办事稳妥，受人信任。", "甲见戌，乙见亥，丙见丑，丁见寅，戊见丑，己见寅，庚见辰，辛见巳，壬见未，癸见申。", "以日干或年干查四柱地支。甲见戌，乙见亥，丙见丑，丁见寅，戊见丑，己见寅，庚见辰，辛见巳，壬见未，癸见申。", "国印贵人是管理国家印信之星，命带国印贵人者，诚实可靠，做事稳重，办事公允，易得领导信任，有掌印之权或从事公职之缘。"),
    "驿马" to ShenshaInfo("驿马", "主奔波、变动、升迁、调动、远行。", "申子辰马在寅，寅午戌马在申，巳酉丑马在亥，亥卯未马在巳。", "以年支或日支查四柱地支。申子辰见寅；寅午戌见申；巳酉丑见亥；亥卯未见巳。", "驿马主走动、外出、奔波、变迁。贵人驿马多升擢，常人驿马多奔波。吉神坐马有乔迁之喜或顺动之利；凶神坐马则奔走四方，忙于生计。"),
    "华盖" to ShenshaInfo("华盖", "聪明好学，艺术天赋，与宗教有缘，但易孤独。", "寅午戌见戌，巳酉丑见丑，申子辰见辰，亥卯未见未。", "以年支或日支查四柱地支。寅午戌见戌；巳酉丑见丑；申子辰见辰；亥卯未见未。", "华盖是艺术与孤独之星。命带华盖者，聪明好学，研究心盛，格调高尚，有艺术天赋，但也较孤僻，不喜随波逐流。与太极贵人并见者，必为五术中人，佛缘更深。"),
    "将星" to ShenshaInfo("将星", "有领导才能，可在军警、管理领域掌权。", "申子辰见子，寅午戌见午，巳酉丑见酉，亥卯未见卯。", "以年支或日支查四柱地支。申子辰见子；寅午戌见午；巳酉丑见酉；亥卯未见卯。", "将星是三合局中神之位，代表领导力和威严。命带将星者，有统率才能，威望高，适合担任管理或军警职务，易在专业领域出类拔萃。"),
    "金舆" to ShenshaInfo("金舆", "主富贵荣华，出行有车马之便，生活安稳。", "甲龙乙蛇丙戊羊，丁己猴歌庚犬方，辛猪壬牛癸逢虎。", "以日干查四柱地支。甲见辰、乙见巳、丙戊见未、丁己见申、庚见戌、辛见亥、壬见丑、癸见寅。", "金舆为金车之象，命带金舆贵人者，多富贵荣华，出行有车马之便，生活安稳。女命逢金舆，主旺夫益子。"),
    "金神" to ShenshaInfo("金神", "个性刚毅，聪明多才，然人缘较差。", "乙丑，己巳，癸酉。", "查日柱或时柱，如日柱或时柱是乙丑、己巳、癸酉，则命带金神。", "金神为刚金之精，命带金神者性多威猛强烈，胆大好胜，常使人敬而远之。刚金要得火炼，故有金神入火乡、发如猛虎之说。弱命喜运行火乡，便为贵命。"),
    "五鬼" to ShenshaInfo("五鬼", "主小人暗算，官非口舌，阴人作祟，易招灾祸。", "甲己巳午癸未存，乙庚寅卯守黄昏，丙辛申酉，丁壬亥子，戊癸寅卯。", "以年干查四柱地支，依五鬼歌诀查之。", "五鬼为凶煞之一，主小人暗算、官非口舌、阴人作祟。命带五鬼者，易有莫名其妙的是非和灾祸，需多加防范，行善积德可减轻其凶性。"),
    "天医" to ShenshaInfo("天医", "掌管疾病之星，宜从事医药、心理学、哲学等职业。", "正月戌，二月亥，三月子，四月丑，五月寅，六月卯，七月辰，八月巳，九月午，十月未，十一月申，十二月酉。", "以月支查其它地支，月建后五辰为天医。正戌、二亥、三子、四丑、五寅、六卯、七辰、八巳、九午、十未、十一申、十二酉。", "天医是掌管疾病之事的星神。四柱逢天医，如不旺又无贵人吉神相扶，常患疾病或身弱无力。若生旺又有贵人相助，不仅身体健壮，且特别适合从事医学、心理学、哲学等职业。"),
    "禄神" to ShenshaInfo("禄神", "日主得根有利，身体健康，充满信心。", "甲禄寅，乙禄卯，丙禄巳，丁禄午，戊禄巳，己禄午，庚禄申，辛禄酉，壬禄亥，癸禄子。", "以日干查四柱地支。甲见寅，乙见卯，丙戊见巳，丁己见午，庚见申，辛见酉，壬见亥，癸见子。", "禄神代表食禄、俸禄、福气。命带禄神者，身体健康，充满信心，一生勤劳积累财富。禄在年支叫岁禄，月支叫建禄，日支叫专禄，时支叫归禄。建禄生是月，财官喜透天。"),
    "天赦" to ShenshaInfo("天赦", "能解人灾祸，遇难成祥，逢凶化吉，尤其对犯法之人有宽大处理之可能。", "春戊寅，夏甲午，秋戊申，冬甲子。", "春季（寅卯辰月）生人见戊寅日；夏季（巳午未月）生人见甲午日；秋季（申酉戌月）生人见戊申日；冬季（亥子丑月）生人见甲子日。", "天赦是神煞中的吉神，主赦免。八字命带天赦之人，能解灾祸，遇难成祥，即使是触犯了法律也有可能获得宽大处理。若日元得旺，配合得宜，多为大人物极品之贵。"),
    "红鸾" to ShenshaInfo("红鸾", "主婚姻喜庆，异性缘佳，有良缘之兆。", "子见卯，丑见寅，寅见丑，卯见子，辰见亥，巳见戌，午见酉，未见申，申见未，酉见午，戌见巳，亥见辰。", "以年支查四柱地支。子年见卯，丑年见寅，寅年见丑，卯年见子，辰年见亥，巳年见戌，午年见酉，未年见申，申年见未，酉年见午，戌年见巳，亥年见辰。", "红鸾星属阴水，主婚姻喜庆。中年之前遇之主吉，属喜庆之事，也有升迁之喜。命带红鸾者，多主婚恋顺利，有良缘之兆。红鸾在疾厄宫时主血光之灾。"),
    "天喜" to ShenshaInfo("天喜", "主缘订、喜庆及生育，配偶条件好。", "子见酉，丑见申，寅见未，卯见午，辰见巳，巳见辰，午见卯，未见寅，申见丑，酉见子，戌见亥，亥见戌。", "以年支查四柱地支。子年见酉，丑年见申，寅年见未，卯年见午，辰年见巳，巳年见辰，午年见卯，未年见寅，申年见丑，酉年见子，戌年见亥，亥年见戌。", "天喜星属阳水，主缘订、喜庆及生育。命带天喜者，主配偶条件好，婚恋顺利，生活喜庆。天喜在疾厄宫时主脑神经衰弱。"),
    "流霞" to ShenshaInfo("流霞", "主血光之灾，男犯血光被伤刀，女犯流霞产后死。", "甲鸡乙犬丙羊加，丁猴戊巳日时查；己马庚龙辛见兔，壬猪癸虎是流霞。", "以日干或年干查四柱地支。甲见酉，乙见戌，丙见未，丁见申，戊见巳，己见午，庚见辰，辛见卯，壬见亥，癸见寅。", "流霞煞主血光之灾，男命逢之主血光被伤刀，女命逢之主产后血崩或难产。贵人寿终遭非命，恶人临死苦挣扎。岁运逢之，须防意外伤害。"),
    "红艳" to ShenshaInfo("红艳", "有魅力，多情，婚姻不顺，易有外遇桃花。", "甲日午，乙日申，丙日寅，丁日未，戊日辰，己日辰，庚日戌，辛日酉，壬日子，癸日申。", "以日干查四柱地支。甲见午，乙见申，丙见寅，丁见未，戊见辰，己见辰，庚见戌，辛见酉，壬见子，癸见申。", "红艳煞是桃花的一种。命见红艳煞，风流多情，好美色，多数有外遇桃花，男女感情方面把控不好，容易有纠纷。女命见之，难免私情，婚前失贞，一谈恋爱就可能同居。"),
    "天罗" to ShenshaInfo("天罗", "主疾病之灾、牢狱之灾，男命忌之。", "辰为天罗，火命人逢戌亥为天罗，戌见亥，亥见戌为天罗。", "以年支或日支查四柱地支。火命人（年柱纳音为火）四柱地支见戌或亥；或四柱地支中戌见亥、亥见戌。", "天罗主疾病、牢狱之灾，大运流年遇之人不利，男命更忌。如得天月二德解救可无忧。"),
    "地网" to ShenshaInfo("地网", "主疾病之灾、牢狱之灾，女命忌之。", "巳为地网，水土命逢辰巳为地网，辰见巳，巳见辰为地网。", "以年支或日支查四柱地支。水土命（年柱纳音为水或土）四柱地支见辰或巳；或四柱地支中辰见巳、巳见辰。", "地网主疾病、牢狱之灾，大运流年遇之人不利，女命更忌。与天罗并论，如得天月二德解救可无忧。"),
    "羊刃" to ShenshaInfo("羊刃", "刚强威猛，性烈易招灾祸，或伤及六亲。", "甲卯，乙寅，丙戊午，丁己巳，庚酉，辛申，壬子，癸亥。", "以日干查四柱地支。甲见卯，乙见寅，丙戊见午，丁己见巳，庚见酉，辛见申，壬见子，癸见亥。", "羊刃为刚强威猛之星，禄过则刃生。身强不喜羊刃，因能夺财劫官，若不刑克妻子，必然六亲不和。身弱遇七杀驾羊刃，则主武贵非凡。有杀无刃不显，有刃无杀不威。"),
    "飞刃" to ShenshaInfo("飞刃", "主血光、意外伤害，好勇斗狠，易惹是非。", "甲辰，乙卯，丙午，丁巳，戊午，己巳，庚未，辛申，壬子，癸亥。", "以日干查四柱地支。羊刃对冲之位为飞刃。甲见辰，乙见卯，丙见午，丁见巳，戊见午，己见巳，庚见未，辛见申，壬见子，癸见亥。", "飞刃是与羊刃对冲的凶煞，主血光、意外伤害。命带飞刃者好勇斗狠，易惹是非，行运逢之多有灾厄。与羊刃并见时，更需谨慎。"),
    "血刃" to ShenshaInfo("血刃", "主血光之灾，意外伤害，易有外伤或手术。", "申子辰见巳，寅午戌见亥，巳酉丑见寅，亥卯未见申。", "以年支或日支查四柱地支，与劫煞相同。申子辰见巳，寅午戌见亥，巳酉丑见寅，亥卯未见申。", "血刃主血光之灾，意外伤害。命带血刃者一生易有外伤或手术之险，大运流年逢之，须防刀伤、车祸等意外。"),
    "八专" to ShenshaInfo("八专", "主好色纵欲，男女感情混乱，多淫欲之事。", "甲寅、乙卯、丁未、戊戌、己未、庚申、辛酉、癸丑八日。", "以日柱查。日柱为甲寅、乙卯、丁未、戊戌、己未、庚申、辛酉、癸丑之一者，即为八专日。", "八专专指淫欲之煞，命值八专日者，多好色纵欲，男女感情混乱。男命逢之多淫欲，女命逢之尤忌，多主不贞。日柱为八专者尤重。"),
    "九丑" to ShenshaInfo("九丑", "主外貌俊美但情欲重，易有风流韵事，婚姻不顺。", "戊子、戊午、己酉、己卯、乙卯、辛酉、辛卯、丁酉、壬子九日。", "以日柱查。日柱为戊子、戊午、己酉、己卯、乙卯、辛酉、辛卯、丁酉、壬子之一者，即为九丑日。", "九丑为妨害之煞，主外貌俊美但情欲重，易有风流韵事，婚姻不顺。女命逢之，多主不贞淫乱；男命逢之，多主好色纵欲。与八专并称。"),
    "劫煞" to ShenshaInfo("劫煞", "外来的、突发的灾祸或劫难。", "申子辰见巳，亥卯未见申，寅午戌见亥，巳酉丑见寅。", "以年支或日支查四柱地支。申子辰见巳；亥卯未见申；寅午戌见亥；巳酉丑见寅。", "劫煞为三合局绝地，劫者夺也，自外夺之为劫。命带劫煞者，须防意外事故、小人争夺之事。劫在五行绝处，其性凶暴，为灾不可当。"),
    "灾煞" to ShenshaInfo("灾煞", "主血光横死之灾，官刑牢狱之事，又名白虎煞。", "申子辰见午，寅午戌见子，巳酉丑见卯，亥卯未见酉。", "以年支查四柱地支。申子辰年生人见午；寅午戌年生人见子；巳酉丑年生人见卯；亥卯未年生人见酉。", "灾煞即白虎煞，多主血光横死之灾。运走忌神逢白虎煞临太岁，多有孝服。居柱中水火支，防火烧水溺；居金木支，防棍杖剑刃；居土支，防摔跌瘟疫。"),
    "元辰" to ShenshaInfo("元辰", "主事多磨，耗财败家，内心烦乱，又名大耗。", "阳男阴女：子未、丑申、寅酉、卯戌、辰亥、巳子、午丑、未寅、申卯、酉辰、戌巳、亥午；阴男阳女相反。", "阳男阴女，子年见未，丑年见申，寅年见酉，卯年见戌，辰年见亥，巳年见子，午年见丑，未年见寅，申年见卯，酉年见辰，戌年见巳，亥年见午。阴男阳女反之。", "元辰又名大耗，是凶煞。命带元辰者，事多磨，耗财败家，内心烦乱，与六亲缘薄。生旺稍可，死绝尤甚。大运流年逢元辰，多有耗损之事。"),
    "空亡" to ShenshaInfo("空亡", "主谋事难成，有名无实，劳而无功，易有失落感。", "甲己申酉，乙庚午未，丙辛辰巳，丁壬寅卯，戊癸子丑。", "以日柱或年柱查旬空，每旬中地支两位为空亡。甲子旬戌亥空，甲戌旬申酉空，甲申旬午未空，甲午旬辰巳空，甲辰旬寅卯空，甲寅旬子丑空。", "空亡为十干不到之地，主谋事难成，有名无实，劳而无功。命带空亡者，易有失落感，一生多虚名虚利。吉神逢空亡则减福，凶神逢空亡则减凶。与贵人并见可稍解。"),
    "童子煞" to ShenshaInfo("童子煞", "主婚姻不顺，多灾多难，易有精神困扰。", "春秋寅子贵，冬夏卯未辰；金木马卯合，水火鸡犬多；土命逢辰巳，童子定不错。", "春秋季（寅卯辰月）生人日时支见寅或子；冬夏季（亥子丑月、巳午未月）生人日时支见卯、未、辰；纳音金木命见午卯；纳音水火命见酉戌；纳音土命见辰巳。", "童子煞是民间流传较广的凶煞，主婚姻不顺，多灾多难，易有精神困扰。查法流派众多，不同地域存在差异。通常以生月、日时支、纳音五行综合判断。"),
    "天厨" to ShenshaInfo("天厨", "一生不愁吃穿，食禄不虞匮乏，福禄满堂。", "甲见巳，乙见午，丙见子，丁见巳，戊见午，己见申，庚见寅，辛见午，壬见酉，癸见亥。", "以日干查四柱地支。甲见巳，乙见午，丙见子，丁见巳，戊见午，己见申，庚见寅，辛见午，壬见酉，癸见亥。", "天厨乃食神建禄之宫，食神是人命福星，食神得禄，其福必厚。天厨入命，如不逢刑冲克破空亡，一生衣食无忧，可享天赐之福。八字带天厨贵人，一生平安吉顺，福禄优游。"),
    "孤辰" to ShenshaInfo("孤辰", "主孤独固执，不利六亲，男命尤忌。", "亥子丑见寅，寅卯辰见巳，巳午未见申，申酉戌见亥。", "以年支查四柱地支。亥子丑年生人见寅；寅卯辰年生人见巳；巳午未年生人见申；申酉戌年生人见亥。", "孤辰主孤独、孤僻。男命遇孤辰，主形孤肉露，面无和气，不利六亲。与空亡并见，自小无倚；与驿马并见，放荡他乡。"),
    "寡宿" to ShenshaInfo("寡宿", "主孤独寡和，不利婚姻，女命尤忌。", "亥子丑见戌，寅卯辰见丑，巳午未见辰，申酉戌见未。", "以年支查四柱地支。亥子丑年生人见戌；寅卯辰年生人见丑；巳午未年生人见辰；申酉戌年生人见未。", "寡宿主孤独寡和，不利婚姻。女命逢寡宿，多愁善感，消极悲观，婚姻多波折。孤辰寡宿两星并见者，更显孤独。"),
    "亡神" to ShenshaInfo("亡神", "主城府深，老谋深算，与凶神并见则性狭易燥，常犯官讼。", "申子辰见亥，亥卯未见寅，寅午戌见巳，巳酉丑见申。", "以年支或日支查四柱地支。申子辰见亥；亥卯未见寅；寅午戌见巳；巳酉丑见申。", "亡者失也，自内失之为亡。亡神临财局为财旺，临官局为官旺。如临喜用与贵人同柱，主人城府深，老谋深算；临绝地或忌神，则为人轻浮狂妄，常犯是非官讼。"),
    "十恶大败" to ShenshaInfo("十恶大败", "主花钱如流水，不善理财，财富难聚。", "甲辰乙巳与壬申，丙申丁亥及庚辰，戊戌癸亥加辛巳，己丑都来十位神。", "以日柱查。日柱为甲辰、乙巳、丙申、丁亥、戊戌、己丑、庚辰、辛巳、壬申、癸亥之一者，即为十恶大败日。", "十恶大败实际是日主禄位值空亡。命带十恶大败者，善意外破财，事物成空，屡受挫折。如带贵气者则有殊福。诀云：十恶大败若真，贵为将，贱为寇。"),
    "桃花" to ShenshaInfo("桃花", "主异性缘佳，风流多情，聪慧灵巧，有艺术天赋。", "申子辰见酉，寅午戌见卯，巳酉丑见午，亥卯未见子。", "以年支或日支查四柱地支。申子辰见酉；寅午戌见卯；巳酉丑见午；亥卯未见子。", "桃花是五行沐浴之地，又名咸池。命带桃花者，聪明俊秀，容貌出众，多才多艺，异性缘佳。桃花在年柱、月柱为内桃花，主夫妻恩爱；在日柱、时柱为外桃花，易有情感纠纷。"),
    "孤鸾" to ShenshaInfo("孤鸾", "主婚姻不顺，晚婚或无婚，男克妻女克夫。", "甲寅、乙巳、丙午、丁巳、戊午、戊申、辛亥、壬子八日。", "以日柱查。日柱为甲寅、乙巳、丙午、丁巳、戊午、戊申、辛亥、壬子之一者，即为孤鸾日。", "孤鸾煞主婚姻不顺，男克妻，女克夫，夫妻难得和睦，多为晚婚或无婚之命。女命逢孤鸾尤忌，多主婚姻多波折。"),
    "阴差阳错" to ShenshaInfo("阴差阳错", "主婚姻不顺，夫妻不和，家道不宁。", "丙子丁丑戊寅日，辛卯壬辰癸巳时，丙午丁未戊申时，辛酉壬戌癸亥时。", "以日柱查。日柱为丙子、丁丑、戊寅、辛卯、壬辰、癸巳、丙午、丁未、戊申、辛酉、壬戌、癸亥之一者，即为阴差阳错日。", "阴差阳错主婚姻不顺，夫妻不和，家道不宁。男命逢之，主外家凌替；女命逢之，主夫家败亡。大运流年逢之，多有家宅不宁之事。"),
    "四废" to ShenshaInfo("四废", "主一生谋事无成，进退无据，多败少成。", "春庚申辛酉，夏壬子癸亥，秋甲寅乙卯，冬丙午丁巳。", "以日柱查。春三月（寅卯辰月）生人见庚申、辛酉；夏三月生人见壬子、癸亥；秋三月生人见甲寅、乙卯；冬三月生人见丙午、丁巳。", "四废为五行死绝之地，主一生谋事无成，进退无据，多败少成。命带四废者，事业上难有大成，多挫折和反复。"),
    "丧门" to ShenshaInfo("丧门", "主孝丧之事，多主家中长辈有灾。", "子寅辰午申戌年见辰，丑卯巳未酉亥年见戌。", "以年支查岁前十二神：年日支前两位为丧门。子年见寅，丑年见卯，寅年见辰，卯年见巳，辰年见午，巳年见未，午年见申，未年见酉，申年见戌，酉年见亥，戌年见子，亥年见丑。", "丧门为凶星，主孝丧之事。如大运、流年遇之，再加上八字组合不好，往往会有灾祸发生，多主长辈有灾或家庭变故。"),
    "吊客" to ShenshaInfo("吊客", "主孝服哭泣之事，多主亲属有丧。", "子寅辰午申戌年见戌，丑卯巳未酉亥年见辰。", "以年支查岁前十二神：年日支后两位为吊客。子年见戌，丑年见亥，寅年见子，卯年见丑，辰年见寅，巳年见卯，午年见辰，未年见巳，申年见午，酉年见未，戌年见申，亥年见酉。", "吊客为凶星，主孝服哭泣之事。与丧门相似，多主亲属有丧。大运流年逢之，须防家人健康问题或亲属变故。"),
    "披麻" to ShenshaInfo("披麻", "主孝服、家宅不宁、亲人灾病。", "子见酉，丑见戌，寅见亥，卯见子，辰见丑，巳见寅，未见辰，申见巳，酉见午，戌见未，亥见申。", "以年支或日支查，年日支后三位为披麻。子年见酉，丑年见戌，寅年见亥，卯年见子，辰年见丑，巳年见寅，午年见卯，未年见辰，申年见巳，酉年见午，戌年见未，亥年见申。", "披麻为凶星，与丧门、吊客同，皆主孝服丧事。如大运、流年遇之，多主人身意外、伤病等事，不易聚财，须防亲人灾病。"),
    "十灵" to ShenshaInfo("十灵", "主聪明伶俐，有悟性，易通灵性，与玄学有缘。", "甲辰乙亥丙辰丁酉戊寅己未庚戌辛丑壬寅癸未十日。", "以日柱查。日柱为甲辰、乙亥、丙辰、丁酉、戊寅、己未、庚戌、辛丑、壬寅、癸未之一者，即为十灵日。", "十灵日为神煞中的吉日，命带十灵日者，聪明伶俐，有悟性，灵性较强，易与玄学、神秘事物结缘。十灵日与十恶大败相对，吉凶各异。")
)

/** 以日干为基准计算某天干的十神（返回全名）。 */
private fun getShishen(targetGan: String, dayGan: String): String {
    val ri = TIANGAN.indexOf(dayGan)
    val tg = TIANGAN.indexOf(targetGan)
    if (ri < 0 || tg < 0) return ""
    val cha = ri - tg
    return if (cha >= 0) when (cha) {
        0 -> "比肩"
        1 -> if (ri % 2 == 1) "劫财" else "正印"
        2 -> "偏印"
        3 -> if (ri % 2 == 1) "正印" else "正官"
        4 -> "七杀"
        5 -> if (ri % 2 == 1) "正官" else "正财"
        6 -> "偏财"
        7 -> if (ri % 2 == 1) "正财" else "伤官"
        8 -> "食神"
        9 -> "伤官"
        else -> ""
    } else when (-cha) {
        1 -> if (ri % 2 == 0) "劫财" else "伤官"
        2 -> "食神"
        3 -> if (ri % 2 == 0) "伤官" else "正财"
        4 -> "偏财"
        5 -> if (ri % 2 == 0) "正财" else "正官"
        6 -> "七杀"
        7 -> if (ri % 2 == 0) "正官" else "正印"
        8 -> "偏印"
        9 -> "正印"
        else -> ""
    }
}

/** 简写十神（1 字）。 */
private fun shishenShort(targetGan: String, dayGan: String): String {
    if (targetGan.isEmpty() || dayGan.isEmpty()) return ""
    val full = getShishen(targetGan, dayGan)
    // 日柱天干特殊处理：日干本身在 paipan 里显示「元男/元女」
    return SS_SHORTER[full] ?: ""
}

/** 地支藏干的十神简写串，例如 申(庚壬戊) -> "财官伤"。 */
private fun dzShishenShort(zhi: String, dayGan: String): String {
    val idx = DIZHI.indexOf(zhi)
    if (idx < 0 || dayGan.isEmpty()) return ""
    val cangGan = DZ_CANGGAN[idx]
    return cangGan.map { ch -> shishenShort(ch.toString(), dayGan) }.joinToString("")
}

@Serializable
private data class RiganEntry(val gan: String, val month: String, val source: String, val original: String, val translate: String)

private data class RiganContent(val original: String, val translate: String, val source: String)

private val RIGAN_MONTH_TO_INT = mapOf(
    "总论" to 0,
    "正月(寅月)" to 1, "二月(卯月)" to 2, "三月(辰月)" to 3,
    "四月(巳月)" to 4, "五月(午月)" to 5, "六月(未月)" to 6,
    "七月(申月)" to 7, "八月(酉月)" to 8, "九月(戌月)" to 9,
    "十月(亥月)" to 10, "十一月(子月)" to 11, "十二月(丑月)" to 12
)

@Composable
private fun rememberRiganMap(): Map<String, Map<Int, RiganContent>> {
    val context = LocalContext.current
    return remember {
        try {
            val text = context.assets.open("rigan.json").bufferedReader().readText()
            val entries = appJson.decodeFromString<List<RiganEntry>>(text)
            entries.groupBy { it.gan.take(1) }
                .mapValues { (_, list) ->
                    list.mapNotNull { e ->
                        val idx = RIGAN_MONTH_TO_INT[e.month] ?: return@mapNotNull null
                        idx to RiganContent(e.original, e.translate, e.source)
                    }.toMap()
                }
        } catch (_: Exception) {
            emptyMap()
        }
    }
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
        "龙门八字四柱八字排盘",
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
        Text("龙门八字出门不下雨万年历", color = LinkBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        DizhiTenGodTableRow(
            label = "地支十神",
            items = result.dayunDetail.dizhiTenGod,
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
        DizhiTenGodTableRow(
            label = "地支十神:",
            items = result.dayunDetail.dizhiTenGod,
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
private fun ShenshaTableRow(
    shenshaPerColumn: List<List<String>>,
    background: Color,
    labelWidth: Dp,
    cellWidth: Dp,
    onShenshaClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .fillMaxHeight()
                .background(background)
                .border(0.5.dp, Color.White)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("神煞:", color = DarkText, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        shenshaPerColumn.forEach { names ->
            Column(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .background(background)
                    .border(0.5.dp, Color.White)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (names.isEmpty()) {
                    Text("—", color = DarkGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                } else {
                    names.forEach { name ->
                        val known = SHENSHA_DATA.containsKey(name)
                        Text(
                            text = name,
                            color = DarkText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (known) FontWeight.Medium else FontWeight.Normal,
                            textDecoration = if (known) TextDecoration.Underline else null,
                            textAlign = TextAlign.Center,
                            modifier = if (known) Modifier.clickable { onShenshaClick(name) } else Modifier
                        )
                    }
                }
            }
        }
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
    textColor: Color = DarkText,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 17.sp
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
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell,
                    color = colors.getOrNull(index) ?: textColor,
                    fontSize = if (index in bigIndexes) 24.sp else fontSize,
                    lineHeight = if (index in bigIndexes) 26.sp else lineHeight,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 地支十神专用 row — 每项单独 Text，Column spacedBy 间隔。 */
@Composable
private fun DizhiTenGodTableRow(
    label: String,
    items: List<List<String>>,
    background: Color,
    height: Dp = 104.dp,
    labelWidth: Dp = 58.dp,
    cellWidth: Dp = 58.dp,
    textColor: Color = DarkText,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    Row(modifier = Modifier.height(height)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .fillMaxSize()
                .background(background)
                .border(0.5.dp, Color.White)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        items.forEach { subItems ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxSize()
                    .background(background)
                    .border(0.5.dp, Color.White)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subItems.forEach { item ->
                        Text(
                            text = item,
                            color = textColor,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** 主字+小字旁注 cell 数据。 */
private data class StackedCell(
    val main: String,
    val sub: String = "",
    val mainColor: Color? = null,
    val isLabel: Boolean = false
)

@Composable
private fun StackedTableRow(
    cells: List<StackedCell>,
    background: Color,
    height: Dp = 44.dp,
    labelWidth: Dp = 58.dp,
    cellWidth: Dp = 80.dp,
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
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cell.isLabel || index == 0) {
                    Text(
                        text = cell.main,
                        color = cell.mainColor ?: textColor,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cell.main,
                            color = cell.mainColor ?: textColor,
                            fontSize = 22.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (cell.sub.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(1.dp))
                            Text(
                                text = cell.sub.toCharArray().joinToString("\n"),
                                color = textColor,
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
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
