package com.lunar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.lunar.data.UserInfo
import com.lunar.data.fetchCurrentUser
import com.lunar.data.isLoginInvalid
import com.lunar.data.userMessage
import com.lunar.ui.theme.DarkText
import com.lunar.ui.theme.RedTitle

@Composable
fun MineScreen(
    authSession: AuthSession?,
    onRequireLogin: () -> Unit,
    onLoginInvalid: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(authSession?.token, reloadKey) {
        val session = authSession
        currentUser = null
        errorMessage = null
        if (session == null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        runCatching { fetchCurrentUser(session.token) }
            .onSuccess { currentUser = it }
            .onFailure { error ->
                if (error.isLoginInvalid()) {
                    onLoginInvalid()
                } else {
                    errorMessage = error.userMessage()
                }
            }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text("我的", color = RedTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(18.dp))

        when {
            authSession == null -> LoginPrompt(onRequireLogin = onRequireLogin)
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedTitle)
            }
            errorMessage != null -> ErrorBlock(
                message = errorMessage.orEmpty(),
                onRetry = { reloadKey++ }
            )
            currentUser != null -> {
                val user = currentUser
                if (user != null) {
                    UserBlock(
                        user = user,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginPrompt(onRequireLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("请先登录", color = DarkText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(14.dp))
        PrimaryButton(text = "去登录", onClick = onRequireLogin)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = RedTitle, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(14.dp))
        PrimaryButton(text = "重试", onClick = onRetry)
    }
}

@Composable
private fun UserBlock(user: UserInfo, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InfoLine(label = "用户 ID", value = user.id.toString())
        InfoLine(label = "账号", value = user.account)
        InfoLine(label = "昵称", value = user.nickname)
        InfoLine(label = "会员状态", value = if (user.isVip) "VIP 用户" else "普通用户")
        if (user.isVip) {
            InfoLine(label = "VIP 到期时间", value = user.vipExpireTime)
        }
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "退出登录", onClick = onLogout, color = RedTitle)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(label, color = Color(0xFF707070), fontSize = 13.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Text(value.ifBlank { "-" }, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF333A42)
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Text(text, fontSize = 15.sp)
    }
}
