package com.chenyue404.sharetext

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

object UiStyle {
    val ScreenBackground = Color(0xFFF2F6F4)
    val TopBarBackground = Color(0xFFE6EFEA)
    val StatusCardBackground = Color(0xFFEAF4F0)
    val ListCardBackground = Color(0xFFFFFFFF)
    val EmptyCardBackground = Color(0xFFFFFFFF)
    val InputCardBackground = Color(0xFFFFFFFF)
    val PrimaryAction = Color(0xFF1E6F5C)
    val SecondaryText = Color(0xFF5B6475)
    val ErrorText = Color(0xFFB42318)
    val RunningText = Color(0xFF1E6F5C)
    val StoppedText = Color(0xFF64748B)

    val ScreenPadding = 12.dp
    val CardPadding = 12.dp
    val InnerSpacing = 8.dp
    val CornerRadius = 14.dp
    val IconButtonSize = 36.dp
    val InputMaxLines = 3
}

fun Activity.applyStatusBarStyle() {
    window.statusBarColor = UiStyle.TopBarBackground.toArgb()
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
}
