package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 30 Distinct, fully coordinated color palettes for Project ARISE.
 * Each palette defines coordinated colors for primary, secondary, accent,
 * background, surface, card borders, text, and status highlights.
 */
data class ArisePalette(
    val id: String,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBorder: Color,
    val textPrimary: Color = Color(0xFFF1F5F9),
    val textSecondary: Color = Color(0xFF94A3B8),
    val isLight: Boolean = false
)

object ArisePalettes {

    // 1. Midnight Blue
    val MidnightBlue = ArisePalette(
        id = "midnight_blue",
        name = "Midnight Blue",
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF6366F1),
        accent = Color(0xFF818CF8),
        background = Color(0xFF0B0F19),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF1F2937),
        cardBorder = Color(0xFF374151)
    )

    // 2. Ocean Blue
    val OceanBlue = ArisePalette(
        id = "ocean_blue",
        name = "Ocean Blue",
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF0EA5E9),
        accent = Color(0xFF38BDF8),
        background = Color(0xFF082F49),
        surface = Color(0xFF0C4A6E),
        surfaceVariant = Color(0xFF075985),
        cardBorder = Color(0xFF0284C7)
    )

    // 3. Electric Cyan (Default System)
    val ElectricCyan = ArisePalette(
        id = "electric_cyan",
        name = "Electric Cyan",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF00B4D8),
        accent = Color(0xFF90E0EF),
        background = Color(0xFF070A12),
        surface = Color(0xFF0D1322),
        surfaceVariant = Color(0xFF131D31),
        cardBorder = Color(0xFF1E2D4A)
    )

    // 4. Neon Purple
    val NeonPurple = ArisePalette(
        id = "neon_purple",
        name = "Neon Purple",
        primary = Color(0xFFD946EF),
        secondary = Color(0xFFA855F7),
        accent = Color(0xFFE879F9),
        background = Color(0xFF13091B),
        surface = Color(0xFF200F2E),
        surfaceVariant = Color(0xFF2E1065),
        cardBorder = Color(0xFF581C87)
    )

    // 5. Royal Violet (Monarch)
    val RoyalViolet = ArisePalette(
        id = "royal_violet",
        name = "Royal Violet",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFF8B5CF6),
        accent = Color(0xFFC084FC),
        background = Color(0xFF0E071A),
        surface = Color(0xFF170C2A),
        surfaceVariant = Color(0xFF24143D),
        cardBorder = Color(0xFF4C1D95)
    )

    // 6. Cyberpunk Pink
    val CyberpunkPink = ArisePalette(
        id = "cyberpunk_pink",
        name = "Cyberpunk Pink",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFF06B6D4),
        accent = Color(0xFFFB7185),
        background = Color(0xFF140810),
        surface = Color(0xFF220C1C),
        surfaceVariant = Color(0xFF38102B),
        cardBorder = Color(0xFF881337)
    )

    // 7. Crimson Red (Red Gate)
    val CrimsonRed = ArisePalette(
        id = "crimson_red",
        name = "Crimson Red",
        primary = Color(0xFFEF4444),
        secondary = Color(0xFFDC2626),
        accent = Color(0xFFF87171),
        background = Color(0xFF130808),
        surface = Color(0xFF220D0D),
        surfaceVariant = Color(0xFF3B1212),
        cardBorder = Color(0xFF7F1D1D)
    )

    // 8. Sunset Orange
    val SunsetOrange = ArisePalette(
        id = "sunset_orange",
        name = "Sunset Orange",
        primary = Color(0xFFF97316),
        secondary = Color(0xFFFB923C),
        accent = Color(0xFFFDBA74),
        background = Color(0xFF140B06),
        surface = Color(0xFF23140A),
        surfaceVariant = Color(0xFF3B1E0C),
        cardBorder = Color(0xFF7C2D12)
    )

    // 9. Golden Amber (Sovereign)
    val GoldenAmber = ArisePalette(
        id = "golden_amber",
        name = "Golden Amber",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFEAB308),
        accent = Color(0xFFFDE047),
        background = Color(0xFF141005),
        surface = Color(0xFF221C09),
        surfaceVariant = Color(0xFF382F10),
        cardBorder = Color(0xFF78350F)
    )

    // 10. Emerald Green
    val EmeraldGreen = ArisePalette(
        id = "emerald_green",
        name = "Emerald Green",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF059669),
        accent = Color(0xFF34D399),
        background = Color(0xFF06140E),
        surface = Color(0xFF0C2219),
        surfaceVariant = Color(0xFF123628),
        cardBorder = Color(0xFF064E3B)
    )

    // 11. Forest Green
    val ForestGreen = ArisePalette(
        id = "forest_green",
        name = "Forest Green",
        primary = Color(0xFF22C55E),
        secondary = Color(0xFF16A34A),
        accent = Color(0xFF86EFAC),
        background = Color(0xFF05170B),
        surface = Color(0xFF0C2614),
        surfaceVariant = Color(0xFF143B20),
        cardBorder = Color(0xFF14532D)
    )

    // 12. Mint
    val Mint = ArisePalette(
        id = "mint",
        name = "Mint",
        primary = Color(0xFF2DD4BF),
        secondary = Color(0xFF14B8A6),
        accent = Color(0xFF5EEAD4),
        background = Color(0xFF061413),
        surface = Color(0xFF0A2220),
        surfaceVariant = Color(0xFF113834),
        cardBorder = Color(0xFF134E48)
    )

    // 13. Teal
    val Teal = ArisePalette(
        id = "teal",
        name = "Teal",
        primary = Color(0xFF0D9488),
        secondary = Color(0xFF14B8A6),
        accent = Color(0xFF2DD4BF),
        background = Color(0xFF041716),
        surface = Color(0xFF082624),
        surfaceVariant = Color(0xFF0F3B38),
        cardBorder = Color(0xFF115E59)
    )

    // 14. Ice Blue
    val IceBlue = ArisePalette(
        id = "ice_blue",
        name = "Ice Blue",
        primary = Color(0xFF7DD3FC),
        secondary = Color(0xFFBAE6FD),
        accent = Color(0xFFE0F2FE),
        background = Color(0xFF0A1319),
        surface = Color(0xFF111E26),
        surfaceVariant = Color(0xFF1B2F3C),
        cardBorder = Color(0xFF075985)
    )

    // 15. Lavender
    val Lavender = ArisePalette(
        id = "lavender",
        name = "Lavender",
        primary = Color(0xFFC084FC),
        secondary = Color(0xFFD8B4FE),
        accent = Color(0xFFE9D5FF),
        background = Color(0xFF120E18),
        surface = Color(0xFF1C1626),
        surfaceVariant = Color(0xFF2B223B),
        cardBorder = Color(0xFF581C87)
    )

    // 16. Rose Pink
    val RosePink = ArisePalette(
        id = "rose_pink",
        name = "Rose Pink",
        primary = Color(0xFFFB7185),
        secondary = Color(0xFFFDA4AF),
        accent = Color(0xFFFECDD3),
        background = Color(0xFF180A0E),
        surface = Color(0xFF261217),
        surfaceVariant = Color(0xFF3D1C25),
        cardBorder = Color(0xFF881337)
    )

    // 17. Coral
    val Coral = ArisePalette(
        id = "coral",
        name = "Coral",
        primary = Color(0xFFFF6B6B),
        secondary = Color(0xFFFF8E72),
        accent = Color(0xFFFFA07A),
        background = Color(0xFF190C0B),
        surface = Color(0xFF271513),
        surfaceVariant = Color(0xFF3E211E),
        cardBorder = Color(0xFF7F1D1D)
    )

    // 18. Monochrome
    val Monochrome = ArisePalette(
        id = "monochrome",
        name = "Monochrome",
        primary = Color(0xFFE2E8F0),
        secondary = Color(0xFF94A3B8),
        accent = Color(0xFFF8FAFC),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF334155),
        cardBorder = Color(0xFF475569)
    )

    // 19. Graphite
    val Graphite = ArisePalette(
        id = "graphite",
        name = "Graphite",
        primary = Color(0xFFA1A1AA),
        secondary = Color(0xFF71717A),
        accent = Color(0xFFD4D4D8),
        background = Color(0xFF121214),
        surface = Color(0xFF18181B),
        surfaceVariant = Color(0xFF27272A),
        cardBorder = Color(0xFF3F3F46)
    )

    // 20. Pure AMOLED Black
    val PureAmoledBlack = ArisePalette(
        id = "amoled_black",
        name = "Pure AMOLED Black",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFFA855F7),
        accent = Color(0xFF00E5FF),
        background = Color(0xFF000000),
        surface = Color(0xFF080808),
        surfaceVariant = Color(0xFF121212),
        cardBorder = Color(0xFF222222)
    )

    // 21. Warm Sand
    val WarmSand = ArisePalette(
        id = "warm_sand",
        name = "Warm Sand",
        primary = Color(0xFFD97706),
        secondary = Color(0xFFB45309),
        accent = Color(0xFFFDE68A),
        background = Color(0xFF18130B),
        surface = Color(0xFF251F14),
        surfaceVariant = Color(0xFF382F20),
        cardBorder = Color(0xFF54442E)
    )

    // 22. Cream and Brown
    val CreamAndBrown = ArisePalette(
        id = "cream_brown",
        name = "Cream and Brown",
        primary = Color(0xFFC29B7F),
        secondary = Color(0xFFA67C52),
        accent = Color(0xFFDFCCB7),
        background = Color(0xFF16120E),
        surface = Color(0xFF241D17),
        surfaceVariant = Color(0xFF382D24),
        cardBorder = Color(0xFF544436)
    )

    // 23. Pastel Rainbow
    val PastelRainbow = ArisePalette(
        id = "pastel_rainbow",
        name = "Pastel Rainbow",
        primary = Color(0xFFF472B6),
        secondary = Color(0xFF60A5FA),
        accent = Color(0xFF34D399),
        background = Color(0xFF140F18),
        surface = Color(0xFF201827),
        surfaceVariant = Color(0xFF31253C),
        cardBorder = Color(0xFF4C3A5D)
    )

    // 24. Aurora
    val Aurora = ArisePalette(
        id = "aurora",
        name = "Aurora",
        primary = Color(0xFF34D399),
        secondary = Color(0xFF38BDF8),
        accent = Color(0xFFA78BFA),
        background = Color(0xFF061414),
        surface = Color(0xFF0B2121),
        surfaceVariant = Color(0xFF133434),
        cardBorder = Color(0xFF1D5151)
    )

    // 25. Galaxy
    val Galaxy = ArisePalette(
        id = "galaxy",
        name = "Galaxy",
        primary = Color(0xFF818CF8),
        secondary = Color(0xFFC084FC),
        accent = Color(0xFF38BDF8),
        background = Color(0xFF0B0919),
        surface = Color(0xFF14102A),
        surfaceVariant = Color(0xFF201B40),
        cardBorder = Color(0xFF342B66)
    )

    // 26. Solar Flare
    val SolarFlare = ArisePalette(
        id = "solar_flare",
        name = "Solar Flare",
        primary = Color(0xFFFBBF24),
        secondary = Color(0xFFF97316),
        accent = Color(0xFFEF4444),
        background = Color(0xFF170E04),
        surface = Color(0xFF261808),
        surfaceVariant = Color(0xFF3D270E),
        cardBorder = Color(0xFF633F17)
    )

    // 27. Arctic
    val Arctic = ArisePalette(
        id = "arctic",
        name = "Arctic",
        primary = Color(0xFFE0F2FE),
        secondary = Color(0xFF38BDF8),
        accent = Color(0xFF0284C7),
        background = Color(0xFF0B141C),
        surface = Color(0xFF13222E),
        surfaceVariant = Color(0xFF1F3547),
        cardBorder = Color(0xFF2E506B)
    )

    // 28. Retro Synthwave
    val RetroSynthwave = ArisePalette(
        id = "retro_synthwave",
        name = "Retro Synthwave",
        primary = Color(0xFFFF007F),
        secondary = Color(0xFF00F0FF),
        accent = Color(0xFFFFD700),
        background = Color(0xFF1A0A2A),
        surface = Color(0xFF281140),
        surfaceVariant = Color(0xFF3E1B63),
        cardBorder = Color(0xFF662D91)
    )

    // 29. Minimal White (Clean Light Mode)
    val MinimalWhite = ArisePalette(
        id = "minimal_white",
        name = "Minimal White",
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF4F46E5),
        accent = Color(0xFF0EA5E9),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        cardBorder = Color(0xFFE2E8F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        isLight = true
    )

    // 30. Custom User Palette (Dynamic template)
    val CustomUserPalette = ArisePalette(
        id = "custom_user",
        name = "Custom User Palette",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFFA855F7),
        accent = Color(0xFFF59E0B),
        background = Color(0xFF070A12),
        surface = Color(0xFF0D1322),
        surfaceVariant = Color(0xFF131D31),
        cardBorder = Color(0xFF1E2D4A)
    )

    val allPalettes: List<ArisePalette> = listOf(
        MidnightBlue, OceanBlue, ElectricCyan, NeonPurple, RoyalViolet,
        CyberpunkPink, CrimsonRed, SunsetOrange, GoldenAmber, EmeraldGreen,
        ForestGreen, Mint, Teal, IceBlue, Lavender,
        RosePink, Coral, Monochrome, Graphite, PureAmoledBlack,
        WarmSand, CreamAndBrown, PastelRainbow, Aurora, Galaxy,
        SolarFlare, Arctic, RetroSynthwave, MinimalWhite, CustomUserPalette
    )

    fun getById(id: String): ArisePalette {
        return allPalettes.find { it.id.equals(id, ignoreCase = true) } ?: ElectricCyan
    }
}
