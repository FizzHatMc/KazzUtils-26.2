package de.kazz.config.ui.theme

/**
 * Theme definition for the KazzUtils config UI.
 *
 * All colors are ARGB packed ints (0xAARRGGBB).
 * Implementations should provide a cohesive color scheme.
 *
 * To add a new theme, implement this interface and register it
 * with [ConfigThemeManager].
 */
interface ConfigTheme {
    /** Display name of the theme (e.g., "Dark", "Light"). */
    val name: String

    // ── General ──────────────────────────────────────────

    /** Background color of the entire screen. */
    val backgroundColor: Int

    /** Color of borders / separators. */
    val borderColor: Int

    // ── Sidebar ──────────────────────────────────────────

    /** Background color of the left sidebar panel. */
    val sidebarBackground: Int

    /** Width of the sidebar in pixels. */
    val sidebarWidth: Int

    /** Background color of the currently selected category button. */
    val selectedCategoryBg: Int

    /** Text color of category buttons. */
    val categoryTextColor: Int

    /** Background color when hovering over a category button. */
    val categoryHoverBg: Int

    // ── Content Panel ────────────────────────────────────

    /** Background color of the right content area. */
    val contentBackground: Int

    // ── Banners (Sub-Category Headers) ───────────────────

    /** Background color of sub-category banners. */
    val bannerBackground: Int

    /** Text color of sub-category banner text. */
    val bannerTextColor: Int

    /** Height of the banner in pixels. */
    val bannerHeight: Int

    // ── Property Labels ──────────────────────────────────

    /** Color of property name labels. */
    val propertyLabelColor: Int

    /** Color of property value text (e.g., slider value, text content). */
    val propertyValueColor: Int

    /** Height of a single property row in pixels. */
    val propertyHeight: Int

    // ── Group Headers (Collapsible) ──────────────────────

    /** Background color of a collapsed group header. */
    val groupHeaderBg: Int

    /** Text color of a group header. */
    val groupHeaderTextColor: Int

    /** Background color of an expanded group header. */
    val groupExpandedBg: Int

    /** Height of a group header in pixels. */
    val groupHeaderHeight: Int

    // ── Scrollbar ────────────────────────────────────────

    /** Color of the scrollbar track. */
    val scrollbarTrack: Int

    /** Color of the scrollbar thumb (draggable part). */
    val scrollbarThumb: Int

    /** Width of the scrollbar in pixels. */
    val scrollbarWidth: Int

    // ── Toggle Switch ────────────────────────────────────

    /** Color of the toggle when in the ON state. */
    val toggleOnColor: Int

    /** Color of the toggle when in the OFF state. */
    val toggleOffColor: Int

    /** Color of the toggle knob. */
    val toggleKnobColor: Int

    // ── Slider ───────────────────────────────────────────

    /** Color of the slider track (background bar). */
    val sliderTrack: Int

    /** Color of the slider thumb (draggable knob). */
    val sliderThumb: Int

    // ── Text Input ───────────────────────────────────────

    /** Background color of the text input field. */
    val inputFieldBg: Int

    /** Border color of the text input field. */
    val inputFieldBorder: Int

    /** Border color when the text input field is focused. */
    val inputFieldFocusedBorder: Int

    // ── Dropdown ─────────────────────────────────────────

    /** Background color of the dropdown list. */
    val dropdownBg: Int

    /** Background color of a hovered dropdown item. */
    val dropdownHoverBg: Int

    /** Text color of dropdown items. */
    val dropdownTextColor: Int

    // ── Color Preset Picker ──────────────────────────────

    /** Border color of a color preset swatch. */
    val colorPresetBorder: Int

    /** Border color of the currently selected color preset. */
    val colorPresetSelectedBorder: Int

    /** Size of each color preset swatch in pixels. */
    val colorPresetSize: Int

    // ── Action Button ────────────────────────────────────

    /** Background color of an action button. */
    val actionButtonBg: Int

    /** Background color of an action button on hover. */
    val actionButtonHoverBg: Int

    /** Text color of an action button. */
    val actionButtonTextColor: Int

    // ── Padding / Spacing ────────────────────────────────

    /** Standard padding (e.g., around content, between elements). */
    val padding: Int

    /** Small padding (e.g., inside widgets, between label and widget). */
    val smallPadding: Int
}