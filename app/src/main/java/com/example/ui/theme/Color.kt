package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Tokens: White + Indigo + Purple Modern Study Workspace Palette
val RankifyPrimary = Color(0xFF4F46E5)          // Indigo 600
val RankifyPrimaryDark = Color(0xFF3730A3)      // Indigo 800
val RankifyPrimaryLight = Color(0xFF6366F1)     // Indigo 500
val RankifyPurple = Color(0xFF7C3AED)            // Violet / Purple 600
val RankifyPurpleLight = Color(0xFF8B5CF6)       // Purple 500
val RankifyPurpleSoft = Color(0xFFA855F7)        // Purple 400
val RankifyPurplePale = Color(0xFFF3E8FF)        // Purple 100
val RankifySecondary = Color(0xFF7C3AED)        // Accent Purple
val RankifyAccent = Color(0xFF7C3AED)           // Purple accent
val RankifyAccentDark = Color(0xFF6D28D9)

// Clean Study Workspace Backgrounds & Surfaces
val RankifyLightBg = Color(0xFFF8F9FE)          // Soft crisp lilac-white
val RankifyLightSurface = Color(0xFFFFFFFF)     // Pure White
val RankifyLightSurfaceVariant = Color(0xFFF1F3FA) // Soft tinted slate/lavender
val RankifyLightTextPrimary = Color(0xFF0F172A) // Deep Slate 900
val RankifyLightTextSecondary = Color(0xFF64748B) // Slate 500

// Glassmorphism & Gradient Tokens
val RankifyGlassSurface = Color(0xF2FFFFFF)      // 95% White
val RankifyGlassBorder = Color(0x1F6366F1)       // Subtle Indigo border glow (12% alpha)
val RankifyGlassHighlight = Color(0x66FFFFFF)    // Soft white highlight
val RankifyCardShadow = Color(0x0F4F46E5)        // Soft indigo drop shadow

// Soft Gradient Brushes
val RankifyGradient = Brush.linearGradient(
    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
)
val RankifySoftGradient = Brush.linearGradient(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
)
val RankifySubtleGradient = Brush.linearGradient(
    listOf(Color(0x144F46E5), Color(0x147C3AED))
)

// Dark Theme Workspace Tokens
val RankifyDarkBg = Color(0xFF0B0F19)           // Deep space blue/black
val RankifyDarkSurface = Color(0xFF131B2E)      // Dark glass slate
val RankifyDarkSurfaceVariant = Color(0xFF1E293B)
val RankifyDarkTextPrimary = Color(0xFFF8FAFC)
val RankifyDarkTextSecondary = Color(0xFF94A3B8)

// Subject Colors (Balanced with Indigo + Purple)
val PhysicsColor = Color(0xFF3B82F6)            // Electric Blue
val ChemistryColor = Color(0xFF10B981)          // Emerald Teal
val MathsColor = Color(0xFFF59E0B)              // Amber Gold
val ErrorMistakeColor = Color(0xFFEF4444)       // Coral Red
val SuccessColor = Color(0xFF10B981)            // Green

/**
 * Modern Material 3 Color Schemes for Rankify
 * Anchored in premium Indigo (Primary) and Purple/Violet (Secondary) tones
 * with crisp white surfaces and cohesive glass accents.
 */
val RankifyLightColorScheme: ColorScheme = lightColorScheme(
    primary = RankifyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    inversePrimary = RankifyPrimaryLight,
    secondary = RankifyPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = RankifyPurpleLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = RankifyLightBg,
    onBackground = RankifyLightTextPrimary,
    surface = RankifyLightSurface,
    onSurface = RankifyLightTextPrimary,
    surfaceVariant = RankifyLightSurfaceVariant,
    onSurfaceVariant = RankifyLightTextSecondary,
    surfaceTint = RankifyPrimary,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

val RankifyDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    inversePrimary = RankifyPrimary,
    secondary = RankifyPurpleLight,
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = RankifyPurpleSoft,
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF6B21A8),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = RankifyDarkBg,
    onBackground = RankifyDarkTextPrimary,
    surface = RankifyDarkSurface,
    onSurface = RankifyDarkTextPrimary,
    surfaceVariant = RankifyDarkSurfaceVariant,
    onSurfaceVariant = RankifyDarkTextSecondary,
    surfaceTint = Color(0xFF818CF8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

/** Default cohesive brand ColorScheme */
val RankifyColorScheme: ColorScheme = RankifyLightColorScheme



