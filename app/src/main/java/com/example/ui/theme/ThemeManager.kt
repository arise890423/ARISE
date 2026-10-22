package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

enum class AppearanceMode(val label: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    AMOLED_BLACK("Pure AMOLED Black"),
    HIGH_CONTRAST("High Contrast"),
    SYSTEM_DEFAULT("System Default"),
    CUSTOM("Custom Hybrid")
}

enum class VisualStyle(val label: String, val cornerRadius: Dp, val borderAlpha: Float, val glowEffect: Boolean) {
    SCI_FI_FUTURISTIC("Futuristic Sci-Fi", 6.dp, 0.7f, true),
    CYBERPUNK("Cyberpunk Neon", 2.dp, 0.9f, true),
    GAMING_HUD("Gaming HUD", 4.dp, 0.8f, true),
    GLASSMORPHISM("Glassmorphism", 18.dp, 0.35f, false),
    MINIMAL_CLEAN("Minimal & Clean", 12.dp, 0.25f, false),
    NEON_GLOW("Neon Glow", 14.dp, 0.95f, true),
    SOFT_GRADIENT("Soft Gradient", 16.dp, 0.4f, false),
    RETRO("Retro Synthwave", 8.dp, 0.75f, true)
}

enum class LayoutDensity(val label: String, val cardPadding: Dp, val itemSpacing: Dp) {
    COMPACT("Compact", 10.dp, 8.dp),
    COMFORTABLE("Comfortable", 16.dp, 12.dp),
    SPACIOUS("Spacious", 20.dp, 16.dp)
}

data class ThemeConfiguration(
    val appearanceMode: AppearanceMode = AppearanceMode.DARK,
    val selectedPaletteId: String = "electric_cyan",
    val visualStyle: VisualStyle = VisualStyle.SCI_FI_FUTURISTIC,
    val layoutDensity: LayoutDensity = LayoutDensity.COMFORTABLE,
    val reducedMotion: Boolean = false,
    val backgroundBlurEnabled: Boolean = true,
    val customPrimaryHex: String? = null,
    val customSecondaryHex: String? = null,
    val customAccentHex: String? = null,
    val customBackgroundHex: String? = null,
    val customSurfaceHex: String? = null
)

object ThemeManager {

    private const val PREFS_NAME = "arise_theme_prefs"
    private var prefs: SharedPreferences? = null

    private val _themeConfig = MutableStateFlow(ThemeConfiguration())
    val themeConfig: StateFlow<ThemeConfiguration> = _themeConfig.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val p = prefs ?: return
        val rawJson = p.getString("theme_config", null) ?: return
        try {
            val obj = JSONObject(rawJson)
            val config = ThemeConfiguration(
                appearanceMode = AppearanceMode.valueOf(obj.optString("appearanceMode", AppearanceMode.DARK.name)),
                selectedPaletteId = obj.optString("selectedPaletteId", "electric_cyan"),
                visualStyle = VisualStyle.valueOf(obj.optString("visualStyle", VisualStyle.SCI_FI_FUTURISTIC.name)),
                layoutDensity = LayoutDensity.valueOf(obj.optString("layoutDensity", LayoutDensity.COMFORTABLE.name)),
                reducedMotion = obj.optBoolean("reducedMotion", false),
                backgroundBlurEnabled = obj.optBoolean("backgroundBlurEnabled", true),
                customPrimaryHex = obj.optString("customPrimaryHex", null.toString()).takeIf { it != "null" && it.isNotBlank() },
                customSecondaryHex = obj.optString("customSecondaryHex", null.toString()).takeIf { it != "null" && it.isNotBlank() },
                customAccentHex = obj.optString("customAccentHex", null.toString()).takeIf { it != "null" && it.isNotBlank() },
                customBackgroundHex = obj.optString("customBackgroundHex", null.toString()).takeIf { it != "null" && it.isNotBlank() },
                customSurfaceHex = obj.optString("customSurfaceHex", null.toString()).takeIf { it != "null" && it.isNotBlank() }
            )
            _themeConfig.value = config
        } catch (_: Exception) {}
    }

    private fun saveToPrefs() {
        val p = prefs ?: return
        val config = _themeConfig.value
        val obj = JSONObject().apply {
            put("appearanceMode", config.appearanceMode.name)
            put("selectedPaletteId", config.selectedPaletteId)
            put("visualStyle", config.visualStyle.name)
            put("layoutDensity", config.layoutDensity.name)
            put("reducedMotion", config.reducedMotion)
            put("backgroundBlurEnabled", config.backgroundBlurEnabled)
            put("customPrimaryHex", config.customPrimaryHex ?: "")
            put("customSecondaryHex", config.customSecondaryHex ?: "")
            put("customAccentHex", config.customAccentHex ?: "")
            put("customBackgroundHex", config.customBackgroundHex ?: "")
            put("customSurfaceHex", config.customSurfaceHex ?: "")
        }
        p.edit().putString("theme_config", obj.toString()).apply()
    }

    fun setPalette(paletteId: String) {
        _themeConfig.update { it.copy(selectedPaletteId = paletteId) }
        saveToPrefs()
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        _themeConfig.update { it.copy(appearanceMode = mode) }
        saveToPrefs()
    }

    fun setVisualStyle(style: VisualStyle) {
        _themeConfig.update { it.copy(visualStyle = style) }
        saveToPrefs()
    }

    fun setLayoutDensity(density: LayoutDensity) {
        _themeConfig.update { it.copy(layoutDensity = density) }
        saveToPrefs()
    }

    fun setReducedMotion(reduced: Boolean) {
        _themeConfig.update { it.copy(reducedMotion = reduced) }
        saveToPrefs()
    }

    fun setHybridColors(
        primaryHex: String? = null,
        secondaryHex: String? = null,
        accentHex: String? = null,
        bgHex: String? = null,
        surfaceHex: String? = null
    ) {
        _themeConfig.update {
            it.copy(
                appearanceMode = AppearanceMode.CUSTOM,
                customPrimaryHex = primaryHex ?: it.customPrimaryHex,
                customSecondaryHex = secondaryHex ?: it.customSecondaryHex,
                customAccentHex = accentHex ?: it.customAccentHex,
                customBackgroundHex = bgHex ?: it.customBackgroundHex,
                customSurfaceHex = surfaceHex ?: it.customSurfaceHex
            )
        }
        saveToPrefs()
    }

    fun resetToDefaults() {
        _themeConfig.value = ThemeConfiguration()
        saveToPrefs()
    }

    /**
     * Resolves the active ArisePalette taking into account appearance mode and hybrid overrides.
     */
    fun resolveActivePalette(config: ThemeConfiguration): ArisePalette {
        val base = when (config.appearanceMode) {
            AppearanceMode.AMOLED_BLACK -> ArisePalettes.PureAmoledBlack
            AppearanceMode.LIGHT -> ArisePalettes.MinimalWhite
            AppearanceMode.HIGH_CONTRAST -> ArisePalette(
                id = "high_contrast",
                name = "High Contrast",
                primary = Color(0xFF00FFFF),
                secondary = Color(0xFFFF00FF),
                accent = Color(0xFFFFFF00),
                background = Color(0xFF000000),
                surface = Color(0xFF101010),
                surfaceVariant = Color(0xFF202020),
                cardBorder = Color(0xFFFFFFFF),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFD4D4D4)
            )
            else -> ArisePalettes.getById(config.selectedPaletteId)
        }

        // Apply custom hybrid overrides if defined
        return base.copy(
            primary = parseHexColor(config.customPrimaryHex) ?: base.primary,
            secondary = parseHexColor(config.customSecondaryHex) ?: base.secondary,
            accent = parseHexColor(config.customAccentHex) ?: base.accent,
            background = parseHexColor(config.customBackgroundHex) ?: base.background,
            surface = parseHexColor(config.customSurfaceHex) ?: base.surface
        )
    }

    fun parseHexColor(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        return try {
            val clean = hex.trim().removePrefix("#")
            val fullHex = when (clean.length) {
                6 -> "FF$clean"
                8 -> clean
                else -> return null
            }
            Color(fullHex.toLong(16))
        } catch (_: Exception) {
            null
        }
    }
}
