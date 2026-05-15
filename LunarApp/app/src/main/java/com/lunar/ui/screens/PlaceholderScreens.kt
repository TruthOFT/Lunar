package com.lunar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.lunar.ui.theme.DarkText

@Composable
fun RecordScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "记录", modifier = modifier)
}

@Composable
fun CourseScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "课程", modifier = modifier)
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
