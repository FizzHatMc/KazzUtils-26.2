package de.kazz.config.ui.theme

/**
 * Default dark theme for the KazzUtils config UI.
 *
 * A modern, dark color scheme with blue accent tones.
 */
object DarkTheme : ConfigTheme {

    override val name: String = "Dark"

    // ── General ──────────────────────────────────────────
    override val backgroundColor: Int = 0xFF1A1A2E.toInt()
    override val borderColor: Int = 0xFF2D2D44.toInt()

    // ── Sidebar ──────────────────────────────────────────
    override val sidebarBackground: Int = 0xFF16213E.toInt()
    override val sidebarWidth: Int = 180
    override val selectedCategoryBg: Int = 0xFF0F3460.toInt()
    override val categoryTextColor: Int = 0xFFE0E0E0.toInt()
    override val categoryHoverBg: Int = 0xFF1A2D5A.toInt()

    // ── Content Panel ────────────────────────────────────
    override val contentBackground: Int = 0xFF1A1A2E.toInt()

    // ── Banners ──────────────────────────────────────────
    override val bannerBackground: Int = 0xFF0F3460.toInt()
    override val bannerTextColor: Int = 0xFFFFFFFF.toInt()
    override val bannerHeight: Int = 36

    // ── Property Labels ──────────────────────────────────
    override val propertyLabelColor: Int = 0xFFCCCCCC.toInt()
    override val propertyValueColor: Int = 0xFFFFFFFF.toInt()
    override val propertyHeight: Int = 28

    // ── Group Headers ────────────────────────────────────
    override val groupHeaderBg: Int = 0xFF1E2A4A.toInt()
    override val groupHeaderTextColor: Int = 0xFFAABBCC.toInt()
    override val groupExpandedBg: Int = 0xFF25355A.toInt()
    override val groupHeaderHeight: Int = 30

    // ── Scrollbar ────────────────────────────────────────
    override val scrollbarTrack: Int = 0xFF2A2A3E.toInt()
    override val scrollbarThumb: Int = 0xFF4A4A6A.toInt()
    override val scrollbarWidth: Int = 6

    // ── Toggle Switch ────────────────────────────────────
    override val toggleOnColor: Int = 0xFF4CAF50.toInt()
    override val toggleOffColor: Int = 0xFF555555.toInt()
    override val toggleKnobColor: Int = 0xFFFFFFFF.toInt()

    // ── Slider ───────────────────────────────────────────
    override val sliderTrack: Int = 0xFF3A3A5A.toInt()
    override val sliderThumb: Int = 0xFF6A6AFF.toInt()

    // ── Text Input ───────────────────────────────────────
    override val inputFieldBg: Int = 0xFF2A2A3E.toInt()
    override val inputFieldBorder: Int = 0xFF3A3A5A.toInt()
    override val inputFieldFocusedBorder: Int = 0xFF6A6AFF.toInt()

    // ── Dropdown ─────────────────────────────────────────
    override val dropdownBg: Int = 0xFF2A2A3E.toInt()
    override val dropdownHoverBg: Int = 0xFF3A3A5A.toInt()
    override val dropdownTextColor: Int = 0xFFE0E0E0.toInt()

    // ── Color Preset Picker ──────────────────────────────
    override val colorPresetBorder: Int = 0xFF555555.toInt()
    override val colorPresetSelectedBorder: Int = 0xFFFFFFFF.toInt()
    override val colorPresetSize: Int = 20

    // ── Action Button ────────────────────────────────────
    override val actionButtonBg: Int = 0xFF3A3A5A.toInt()
    override val actionButtonHoverBg: Int = 0xFF4A4A6A.toInt()
    override val actionButtonTextColor: Int = 0xFFE0E0E0.toInt()

    // ── Padding / Spacing ────────────────────────────────
    override val padding: Int = 12
    override val smallPadding: Int = 6
}
