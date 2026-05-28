package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isDarkModeActiveBySystemOrUser by mutableStateOf(false)

// Palette based on Deep Blue / Azure
val MotoDarkBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF0B141B) else Color(0xFFF0F4F8)
val MotoCardBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF15202B) else Color(0xFFFFFFFF)
val MotoSecondaryBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF1E2D3D) else Color(0xFFE1EBF5)
val MotoInputBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF1A2633) else Color(0xFFEDF2F7)

val MotoPrimary: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF8ECAFF) else Color(0xFF0061A4)
val MotoAccent: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF00487B) else Color(0xFFD1E4FF)
val MotoSuccess: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF7DD99B) else Color(0xFF006D39)
val MotoDanger: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)

val MotoTextMain: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFE2E2E6) else Color(0xFF191C1E)
val MotoTextMuted: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFA5AABF) else Color(0xFF43474E)
val MotoBorder: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF3F474E) else Color(0xFFDDE2EA)
