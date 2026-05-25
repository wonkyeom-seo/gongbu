package kr.kro.gongbu33

import android.content.Context
import androidx.compose.ui.graphics.Color

data class ThemeSettings(
    val presetId: String,
    val backgroundArgb: Int,
    val textArgb: Int
)

data class ThemePreset(
    val id: String,
    val label: String,
    val backgroundArgb: Int,
    val textArgb: Int
)

data class AppColors(
    val background: Color,
    val text: Color,
    val surface: Color,
    val outline: Color,
    val mutedText: Color,
    val primaryButton: Color,
    val primaryButtonText: Color,
    val secondaryButton: Color,
    val dangerButton: Color,
    val dangerText: Color,
    val disabledButton: Color,
    val disabledText: Color
)

object ThemeSettingsStore {
    const val CUSTOM_PRESET_ID = "custom"

    val presets = listOf(
        ThemePreset("black", "기본", 0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
        ThemePreset("warm", "편안함", 0xFF11100C.toInt(), 0xFFF7F0DD.toInt()),
        ThemePreset("paper", "밝게", 0xFFFAFAF7.toInt(), 0xFF111111.toInt()),
        ThemePreset("navy", "네이비", 0xFF061120.toInt(), 0xFFEAF3FF.toInt()),
        ThemePreset("green", "그린", 0xFF06150E.toInt(), 0xFFE8FFF0.toInt())
    )

    fun read(context: Context): ThemeSettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fallback = presets.first()
        val presetId = prefs.getString(KEY_PRESET, fallback.id).orEmpty()
        return ThemeSettings(
            presetId = presetId,
            backgroundArgb = prefs.getInt(KEY_BACKGROUND, fallback.backgroundArgb),
            textArgb = prefs.getInt(KEY_TEXT, fallback.textArgb)
        )
    }

    fun savePreset(context: Context, preset: ThemePreset): ThemeSettings {
        val settings = ThemeSettings(
            presetId = preset.id,
            backgroundArgb = preset.backgroundArgb,
            textArgb = preset.textArgb
        )
        save(context, settings)
        return settings
    }

    fun saveCustom(context: Context, backgroundArgb: Int, textArgb: Int): ThemeSettings {
        val settings = ThemeSettings(
            presetId = CUSTOM_PRESET_ID,
            backgroundArgb = backgroundArgb,
            textArgb = textArgb
        )
        save(context, settings)
        return settings
    }

    fun parseHexColor(input: String): Int? {
        val raw = input.trim().removePrefix("#")
        if (raw.length != 6 && raw.length != 8) return null
        if (!raw.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null

        val argb = if (raw.length == 6) "FF$raw" else raw
        return argb.toLong(16).toInt()
    }

    fun toHexColor(argb: Int): String {
        return "#%06X".format(argb and 0x00FFFFFF)
    }

    private fun save(context: Context, settings: ThemeSettings) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESET, settings.presetId)
            .putInt(KEY_BACKGROUND, settings.backgroundArgb)
            .putInt(KEY_TEXT, settings.textArgb)
            .apply()
    }

    private const val PREFS = "theme_settings"
    private const val KEY_PRESET = "preset"
    private const val KEY_BACKGROUND = "background"
    private const val KEY_TEXT = "text"
}

fun ThemeSettings.toAppColors(): AppColors {
    val background = backgroundArgb.toColor()
    val text = textArgb.toColor()
    return AppColors(
        background = background,
        text = text,
        surface = mix(backgroundArgb, textArgb, 0.09f).toColor(),
        outline = mix(backgroundArgb, textArgb, 0.28f).toColor(),
        mutedText = mix(backgroundArgb, textArgb, 0.68f).toColor(),
        primaryButton = text,
        primaryButtonText = background,
        secondaryButton = mix(backgroundArgb, textArgb, 0.18f).toColor(),
        dangerButton = mix(backgroundArgb, 0xFFFF5555.toInt(), 0.32f).toColor(),
        dangerText = Color(0xFFFF6B6B),
        disabledButton = mix(backgroundArgb, textArgb, 0.12f).toColor(),
        disabledText = mix(backgroundArgb, textArgb, 0.42f).toColor()
    )
}

private fun Int.toColor(): Color = Color(this)

private fun mix(fromArgb: Int, toArgb: Int, amount: Float): Int {
    val ratio = amount.coerceIn(0f, 1f)
    val inverse = 1f - ratio
    val alpha = (255 * 1f).toInt()
    val red = (((fromArgb shr 16) and 0xFF) * inverse + ((toArgb shr 16) and 0xFF) * ratio).toInt()
    val green = (((fromArgb shr 8) and 0xFF) * inverse + ((toArgb shr 8) and 0xFF) * ratio).toInt()
    val blue = ((fromArgb and 0xFF) * inverse + (toArgb and 0xFF) * ratio).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
