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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.lunar.ui.theme.RedTitle

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var account by rememberSaveable { mutableStateOf("admin") }
    var password by rememberSaveable { mutableStateOf("admin") }
    var saveAccount by rememberSaveable { mutableStateOf(true) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginInput(
            value = account,
            onValueChange = {
                account = it
                errorMessage = null
            },
            password = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        LoginInput(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            password = true
        )
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
            Text("保存账号密码", color = Color.Black, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(
            onClick = {
                if (account == "admin" && password == "admin") {
                    onLoginSuccess()
                } else {
                    errorMessage = "账号或密码错误"
                }
            },
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFF333A42),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Text("登录", fontSize = 16.sp)
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
    password: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF2B3A4A)),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(3.dp))
            .background(Color.White, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}
