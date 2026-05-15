package com.lunar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunar.navigation.AppDestinations
import com.lunar.ui.theme.DarkText
import com.lunar.ui.theme.NavBackground

@Composable
fun BottomNavBar(
    currentDestination: AppDestinations,
    onDestinationSelected: (AppDestinations) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(NavBackground)
            .border(0.5.dp, Color(0xFFCFCFCF))
    ) {
        AppDestinations.entries.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .border(0.5.dp, Color(0xFFCFCFCF))
                    .clickable { onDestinationSelected(item) }
                    .padding(top = 5.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = item.label,
                    modifier = Modifier.size(23.dp),
                    tint = if (item == currentDestination) Color.Black else DarkText
                )
                Text(
                    item.label,
                    color = if (item == currentDestination) Color.Black else DarkText,
                    fontSize = 12.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
