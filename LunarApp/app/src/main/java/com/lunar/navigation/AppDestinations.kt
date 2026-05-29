package com.lunar.navigation

import com.lunar.R

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    CHART("排盘", R.drawable.home),
    RECORD("记录", R.drawable.record),
    MINE("我的", R.drawable.mine),
}
