package com.lunar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.data.AuthSession
import com.lunar.data.login
import com.lunar.data.register
import com.lunar.data.userMessage
import com.lunar.ui.theme.RedTitle
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (AuthSession) -> Unit,
    modifier: Modifier = Modifier
) {
    var account by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var saveAccount by rememberSaveable { mutableStateOf(true) }
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginInput(account, { account = it; errorMessage = null }, false, "账号")
        Spacer(modifier = Modifier.height(16.dp))
        LoginInput(password, { password = it; errorMessage = null }, true, "密码")
        if (registerMode) {
            Spacer(modifier = Modifier.height(16.dp))
            LoginInput(nickname, { nickname = it; errorMessage = null }, false, "昵称")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = saveAccount,
                onCheckedChange = { saveAccount = it },
                modifier = Modifier.size(28.dp)
            )
            Text("保存登录状态", color = Color.Black, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    runCatching {
                        if (registerMode) {
                            register(account.trim(), password, nickname.trim())
                        } else {
                            login(account.trim(), password)
                        }
                    }.onSuccess(onLoginSuccess)
                        .onFailure { errorMessage = it.userMessage() }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFF333A42),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF7B828A),
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Text(if (registerMode) "注册并登录" else "登录", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                registerMode = !registerMode
                errorMessage = null
            },
            enabled = !isLoading
        ) {
            val text = if (registerMode) {
                "已有账号，去登录"
            } else {
                "没有账号，去注册"
            }
            Text(text, color = RedTitle, fontSize = 14.sp)
        }
        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RedTitle)
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = RedTitle, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean,
    hint: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF2B3A4A)),
        decorationBox = { innerTextField ->
            if (value.isBlank()) {
                Text(hint, fontSize = 14.sp, color = Color(0xFF8A929A))
            }
            innerTextField()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(3.dp))
            .background(Color.White, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}
