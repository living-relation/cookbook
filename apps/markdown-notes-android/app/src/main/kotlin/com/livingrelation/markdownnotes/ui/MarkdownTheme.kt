package com.livingrelation.markdownnotes.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF1C1814)
private val Paper = Color(0xFFF3EEE4)
private val Moss = Color(0xFF1E5844)
private val MossSoft = Color(0xFFDCE8E1)
private val Rule = Color(0xFFD2C6B6)
private val Wash = Color(0xFFE8DCC8)

private val MarkdownColors =
    lightColorScheme(
        primary = Moss,
        onPrimary = Color.White,
        primaryContainer = MossSoft,
        onPrimaryContainer = Ink,
        secondary = Color(0xFF6B5344),
        onSecondary = Color.White,
        secondaryContainer = Wash,
        onSecondaryContainer = Ink,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        surfaceVariant = Wash,
        onSurfaceVariant = Color(0xFF4A433C),
        outline = Rule,
        outlineVariant = Color(0xFFE4D9CB),
    )

private val MarkdownTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                color = Ink,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                color = Ink,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                color = Ink,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = Ink,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = Ink,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = Ink,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Ink,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
    )

@Composable
fun MarkdownTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MarkdownColors,
        typography = MarkdownTypography,
        content = content,
    )
}
