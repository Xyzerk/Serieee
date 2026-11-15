package com.seriee.app.data

/**
 * Portable snapshot of the user's theme, shared as a short text code.
 *
 * Colours are stored as unsigned 32-bit ARGB values so a code survives a
 * copy/paste round-trip through the clipboard without locale issues.
 */
data class ThemePreset(
    val mode: AppThemeMode = AppThemeMode.System,
    val style: AppThemeStyle = AppThemeStyle.Classic,
    val accent: Long = 0xFF3B82F6L,
    val backgroundStart: Long = 0xFF1D4ED8L,
    val backgroundEnd: Long = 0xFF7C2A64L,
    val gradient: Boolean = true,
    val reduceTransparency: Boolean = false
)

/**
 * `seriee-theme:1;mode=Dark;style=Liquid;accent=FFF0559B;bg1=FF1D4ED8;...`
 *
 * Unknown fields are ignored and missing ones fall back to defaults, so older
 * codes keep working when new fields are added.
 */
object ThemePresetCodec {

    private const val PREFIX = "seriee-theme:1"

    fun encode(preset: ThemePreset): String {
        val fields = linkedMapOf(
            "mode" to preset.mode.name,
            "style" to preset.style.name,
            "accent" to hex(preset.accent),
            "bg1" to hex(preset.backgroundStart),
            "bg2" to hex(preset.backgroundEnd),
            "gradient" to if (preset.gradient) "1" else "0",
            "flat" to if (preset.reduceTransparency) "1" else "0"
        )
        return "$PREFIX;" + fields.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    /** Returns null when the text is not a Seriee theme code. */
    fun decode(raw: String?): ThemePreset? {
        val text = raw?.trim().orEmpty()
        if (!text.startsWith("seriee-theme:")) return null
        val head = text.substringBefore(';')
        if (head != PREFIX) return null
        val fields = text.split(';')
            .drop(1)
            .mapNotNull { entry ->
                val key = entry.substringBefore('=', "").trim()
                val value = entry.substringAfter('=', "").trim()
                if (key.isEmpty() || value.isEmpty()) null else key to value
            }
            .toMap(LinkedHashMap())

        val defaults = ThemePreset()
        return ThemePreset(
            mode = fields["mode"]?.let { AppThemeMode.fromName(it) } ?: defaults.mode,
            style = fields["style"]?.let { AppThemeStyle.fromName(it) } ?: defaults.style,
            accent = fields["accent"]?.let(::parseColor) ?: defaults.accent,
            backgroundStart = fields["bg1"]?.let(::parseColor) ?: defaults.backgroundStart,
            backgroundEnd = fields["bg2"]?.let(::parseColor) ?: defaults.backgroundEnd,
            gradient = fields["gradient"]?.let { it != "0" } ?: defaults.gradient,
            reduceTransparency = fields["flat"]?.let { it != "0" } ?: defaults.reduceTransparency
        )
    }

    private fun hex(value: Long): String =
        value.toInt().toUInt().toString(16).uppercase().padStart(8, '0')

    private fun parseColor(value: String): Long? =
        runCatching { value.removePrefix("#").toLong(16) and 0xFFFFFFFFL }.getOrNull()
}
