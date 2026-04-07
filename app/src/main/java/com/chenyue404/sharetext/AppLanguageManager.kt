package com.chenyue404.sharetext

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguageOption(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-CN")
}

object AppLanguageManager {

    fun currentOption(context: Context): AppLanguageOption {
        val tag = currentTag(context)
        return when {
            tag.startsWith("zh", ignoreCase = true) -> AppLanguageOption.SIMPLIFIED_CHINESE
            tag.startsWith("en", ignoreCase = true) -> AppLanguageOption.ENGLISH
            else -> AppLanguageOption.SYSTEM
        }
    }

    fun apply(context: Context, option: AppLanguageOption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales = LocaleList.forLanguageTags(option.tag)
            return
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(option.tag))
    }

    private fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java)
            return manager?.applicationLocales?.toLanguageTags().orEmpty()
        }
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}
