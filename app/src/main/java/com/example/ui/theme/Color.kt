package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isDarkModeActiveBySystemOrUser by mutableStateOf(false)

val MotoDarkBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF141218) else Color(0xFFFEF7FF)
val MotoCardBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF1D1B20) else Color(0xFFFFFFFF)
val MotoSecondaryBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF2B2930) else Color(0xFFF3EDF7)
val MotoInputBg: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF25232A) else Color(0xFFF3EDF7)

val MotoPrimary: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFD0BCFF) else Color(0xFF6750A4)
val MotoAccent: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF4F378B) else Color(0xFFEADDFF)
val MotoSuccess: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF81C784) else Color(0xFF2E7D32)
val MotoDanger: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFF2B8B5) else Color(0xFFB3261E)

val MotoTextMain: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
val MotoTextMuted: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFFCAC4D0) else Color(0xFF49454F)
val MotoBorder: Color get() = if (isDarkModeActiveBySystemOrUser) Color(0xFF49454F) else Color(0xFFCAC4D0)
