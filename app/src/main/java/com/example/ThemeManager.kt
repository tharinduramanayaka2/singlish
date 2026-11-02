package com.example

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Manages Keyboard Themes (Light, Dark, Helakuru Sinhala Blue, and Custom/Emerald).
 * Persists theme selection to SharedPreferences and provides dynamic styling helpers.
 */
object ThemeManager {

    private const val PREFS_NAME = "singlish_keyboard_prefs"
    private const val KEY_THEME = "keyboard_theme"
    private const val KEY_HAPTIC = "keyboard_haptic_feedback"
    private const val KEY_SOUND = "keyboard_sound_feedback"

    enum class KeyboardTheme(val displayName: String) {
        LIGHT("Clean Light"),
        DARK("Midnight Dark"),
        SINHALA_BLUE("Helakuru Royal Blue"),
        CUSTOM_EMERALD("Ceylon Emerald")
    }

    data class ThemeColors(
        val keyboardBackground: Int,
        val keyBackground: Int,
        val keyActionBackground: Int,
        val keyPrimaryBackground: Int,
        val keyTextColor: Int,
        val keyActionTextColor: Int,
        val keySubTextColor: Int,
        val candidateBarBackground: Int,
        val candidateChipBackground: Int,
        val candidateChipTextColor: Int,
        val candidatePrimaryChipBackground: Int,
        val candidatePrimaryChipTextColor: Int,
        val dividerColor: Int
    )

    private val lightColors = ThemeColors(
        keyboardBackground = Color.parseColor("#E2E8F0"), // Soft Slate-200
        keyBackground = Color.parseColor("#FFFFFF"),      // Pure White
        keyActionBackground = Color.parseColor("#CBD5E1"),// Slate-300
        keyPrimaryBackground = Color.parseColor("#2563EB"),// Royal Blue
        keyTextColor = Color.parseColor("#0F172A"),       // Slate-900
        keyActionTextColor = Color.parseColor("#1E293B"), // Slate-800
        keySubTextColor = Color.parseColor("#64748B"),    // Slate-500
        candidateBarBackground = Color.parseColor("#F8FAFC"),
        candidateChipBackground = Color.parseColor("#E2E8F0"),
        candidateChipTextColor = Color.parseColor("#1E293B"),
        candidatePrimaryChipBackground = Color.parseColor("#2563EB"),
        candidatePrimaryChipTextColor = Color.parseColor("#FFFFFF"),
        dividerColor = Color.parseColor("#CBD5E1")
    )

    private val darkColors = ThemeColors(
        keyboardBackground = Color.parseColor("#0F172A"), // Dark Slate-900
        keyBackground = Color.parseColor("#1E293B"),      // Slate-800
        keyActionBackground = Color.parseColor("#334155"),// Slate-700
        keyPrimaryBackground = Color.parseColor("#3B82F6"),// Electric Blue
        keyTextColor = Color.parseColor("#F8FAFC"),       // Crisp White
        keyActionTextColor = Color.parseColor("#E2E8F0"), // Off-white
        keySubTextColor = Color.parseColor("#94A3B8"),    // Muted grey
        candidateBarBackground = Color.parseColor("#1E293B"),
        candidateChipBackground = Color.parseColor("#334155"),
        candidateChipTextColor = Color.parseColor("#F1F5F9"),
        candidatePrimaryChipBackground = Color.parseColor("#3B82F6"),
        candidatePrimaryChipTextColor = Color.parseColor("#FFFFFF"),
        dividerColor = Color.parseColor("#334155")
    )

    private val sinhalaBlueColors = ThemeColors(
        keyboardBackground = Color.parseColor("#0A192F"), // Deep Sapphire
        keyBackground = Color.parseColor("#172554"),      // Royal Blue Card
        keyActionBackground = Color.parseColor("#1E3A8A"),// Ocean Blue
        keyPrimaryBackground = Color.parseColor("#F59E0B"),// Golden Accent
        keyTextColor = Color.parseColor("#FFFFFF"),       // White
        keyActionTextColor = Color.parseColor("#FEF08A"), // Soft Gold
        keySubTextColor = Color.parseColor("#93C5FD"),    // Sky Blue
        candidateBarBackground = Color.parseColor("#0F172A"),
        candidateChipBackground = Color.parseColor("#1E3A8A"),
        candidateChipTextColor = Color.parseColor("#FFFFFF"),
        candidatePrimaryChipBackground = Color.parseColor("#F59E0B"),
        candidatePrimaryChipTextColor = Color.parseColor("#0F172A"),
        dividerColor = Color.parseColor("#1E3A8A")
    )

    private val emeraldColors = ThemeColors(
        keyboardBackground = Color.parseColor("#064E3B"), // Deep Emerald
        keyBackground = Color.parseColor("#065F46"),      // Forest Slate
        keyActionBackground = Color.parseColor("#047857"),// Emerald Accent
        keyPrimaryBackground = Color.parseColor("#10B981"),// Mint Emerald
        keyTextColor = Color.parseColor("#ECFDF5"),       // Pale Green
        keyActionTextColor = Color.parseColor("#A7F3D0"), // Mint
        keySubTextColor = Color.parseColor("#6EE7B7"),    // Soft Emerald
        candidateBarBackground = Color.parseColor("#022C22"),
        candidateChipBackground = Color.parseColor("#065F46"),
        candidateChipTextColor = Color.parseColor("#ECFDF5"),
        candidatePrimaryChipBackground = Color.parseColor("#10B981"),
        candidatePrimaryChipTextColor = Color.parseColor("#064E3B"),
        dividerColor = Color.parseColor("#047857")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentTheme(context: Context): KeyboardTheme {
        val themeName = getPrefs(context).getString(KEY_THEME, KeyboardTheme.SINHALA_BLUE.name)
        return try {
            KeyboardTheme.valueOf(themeName ?: KeyboardTheme.SINHALA_BLUE.name)
        } catch (_: Exception) {
            KeyboardTheme.SINHALA_BLUE
        }
    }

    fun setTheme(context: Context, theme: KeyboardTheme) {
        getPrefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getThemeColors(theme: KeyboardTheme): ThemeColors {
        return when (theme) {
            KeyboardTheme.LIGHT -> lightColors
            KeyboardTheme.DARK -> darkColors
            KeyboardTheme.SINHALA_BLUE -> sinhalaBlueColors
            KeyboardTheme.CUSTOM_EMERALD -> emeraldColors
        }
    }

    fun isHapticEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTIC, true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    /**
     * Helper to create rounded key backgrounds with specific colors
     */
    fun createKeyDrawable(backgroundColor: Int, cornerRadiusDp: Float = 6f, strokeColor: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * 2.5f
            setColor(backgroundColor)
            if (strokeColor != 0) {
                setStroke(1, strokeColor)
            }
        }
    }
}
