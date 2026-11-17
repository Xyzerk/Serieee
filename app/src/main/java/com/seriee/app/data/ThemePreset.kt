package com.seriee.app.data

/**
 * Fine-grained custom theme settings layered on top of the basic accent and
 * background. Every colour is an optional ARGB override: null means "derive it
 * automatically", so untouched tokens keep following the base theme.
 */
data class CustomThemeExtras(
    val text: Long? = null,
    val muted: Long? = null,
    val card: Long? = null,
    val surface: Long? = null,
    val border: Long? = null,
    val success: Long? = null,
    val warning: Long? = null,
    val danger: Long? = null,
    val info: Long? = null,
    val reminder: Long? = null,
    val star: Long? = null,
    /** Absolute path of the picked background image inside app storage. */
    val imagePath: String? = null,
    val imageEnabled: Boolean = false,
    /** 0..0.85 black scrim over the image so text stays readable. */
    val imageDim: Float = 0.35f,
    /** Blur radius in dp (only rendered on Android 12+). */
    val imageBlur: Float = 0f,
    /** Opacity of cards and surfaces while an image is the background. */
    val surfaceOpacity: Float = 0.72f
) {
    val hasImage: Boolean get() = imageEnabled && !imagePath.isNullOrBlank()

    fun hasColorOverrides(): Boolean = listOf(
        text, muted, card, surface, border, success, warning, danger, info, reminder, star
    ).any { it != null }

    fun toJson(): String = org.json.JSONObject().apply {
        // Must not be named `put`: a local `put(String, Long?)` would shadow
        // JSONObject.put and call itself forever (StackOverflowError).
        fun putColor(key: String, v: Long?) { if (v != null) put(key, v) }
        putColor("text", text); putColor("muted", muted); putColor("card", card)
        putColor("surface", surface); putColor("border", border); putColor("success", success)
        putColor("warning", warning); putColor("danger", danger); putColor("info", info)
        putColor("reminder", reminder); putColor("star", star)
        imagePath?.let { put("imagePath", it) }
        put("imageEnabled", imageEnabled)
        put("imageDim", imageDim.toDouble())
        put("imageBlur", imageBlur.toDouble())
        put("surfaceOpacity", surfaceOpacity.toDouble())
    }.toString()

    companion object {
        fun fromJson(raw: String?): CustomThemeExtras {
            if (raw.isNullOrBlank()) return CustomThemeExtras()
            return runCatching {
                val o = org.json.JSONObject(raw)
                fun color(key: String): Long? =
                    if (o.has(key)) o.getLong(key) and 0xFFFFFFFFL else null
                val d = CustomThemeExtras()
                CustomThemeExtras(
                    text = color("text"), muted = color("muted"), card = color("card"),
                    surface = color("surface"), border = color("border"),
                    success = color("success"), warning = color("warning"),
                    danger = color("danger"), info = color("info"),
                    reminder = color("reminder"), star = color("star"),
                    imagePath = o.optString("imagePath", "").ifBlank { null },
                    imageEnabled = o.optBoolean("imageEnabled", d.imageEnabled),
                    imageDim = o.optDouble("imageDim", d.imageDim.toDouble()).toFloat().coerceIn(0f, 0.85f),
                    imageBlur = o.optDouble("imageBlur", d.imageBlur.toDouble()).toFloat().coerceIn(0f, 25f),
                    surfaceOpacity = o.optDouble("surfaceOpacity", d.surfaceOpacity.toDouble()).toFloat().coerceIn(0.25f, 1f)
                )
            }.getOrDefault(CustomThemeExtras())
        }
    }
}

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
    val glass: Boolean = false,
    val highContrast: Boolean = false,
    val reduceTransparency: Boolean = false,
    /** Neo-Brutalism tone name (e.g. "Yellow", "Pink"). */
    val neoTone: String = "Yellow",
    /** Colour overrides and image tuning. The image file itself is never part of a code. */
    val extras: CustomThemeExtras = CustomThemeExtras()
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
            "glass" to if (preset.glass) "1" else "0",
            "contrast" to if (preset.highContrast) "1" else "0",
            "flat" to if (preset.reduceTransparency) "1" else "0",
            "neo" to preset.neoTone
        )
        val x = preset.extras
        listOf(
            "t_text" to x.text, "t_muted" to x.muted, "t_card" to x.card,
            "t_surface" to x.surface, "t_border" to x.border, "t_success" to x.success,
            "t_warning" to x.warning, "t_danger" to x.danger, "t_info" to x.info,
            "t_reminder" to x.reminder, "t_star" to x.star
        ).forEach { (k, v) -> if (v != null) fields[k] = hex(v) }
        fields["i_dim"] = "%.2f".format(java.util.Locale.US, x.imageDim)
        fields["i_blur"] = "%.1f".format(java.util.Locale.US, x.imageBlur)
        fields["i_op"] = "%.2f".format(java.util.Locale.US, x.surfaceOpacity)
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
            glass = fields["glass"]?.let { it != "0" } ?: defaults.glass,
            highContrast = fields["contrast"]?.let { it != "0" } ?: defaults.highContrast,
            reduceTransparency = fields["flat"]?.let { it != "0" } ?: defaults.reduceTransparency,
            neoTone = fields["neo"] ?: defaults.neoTone,
            extras = CustomThemeExtras(
                text = fields["t_text"]?.let(::parseColor),
                muted = fields["t_muted"]?.let(::parseColor),
                card = fields["t_card"]?.let(::parseColor),
                surface = fields["t_surface"]?.let(::parseColor),
                border = fields["t_border"]?.let(::parseColor),
                success = fields["t_success"]?.let(::parseColor),
                warning = fields["t_warning"]?.let(::parseColor),
                danger = fields["t_danger"]?.let(::parseColor),
                info = fields["t_info"]?.let(::parseColor),
                reminder = fields["t_reminder"]?.let(::parseColor),
                star = fields["t_star"]?.let(::parseColor),
                imageDim = fields["i_dim"]?.toFloatOrNull()?.coerceIn(0f, 0.85f)
                    ?: defaults.extras.imageDim,
                imageBlur = fields["i_blur"]?.toFloatOrNull()?.coerceIn(0f, 25f)
                    ?: defaults.extras.imageBlur,
                surfaceOpacity = fields["i_op"]?.toFloatOrNull()?.coerceIn(0.25f, 1f)
                    ?: defaults.extras.surfaceOpacity
            )
        )
    }

    private fun hex(value: Long): String =
        value.toInt().toUInt().toString(16).uppercase().padStart(8, '0')

    private fun parseColor(value: String): Long? =
        runCatching { value.removePrefix("#").toLong(16) and 0xFFFFFFFFL }.getOrNull()
}
