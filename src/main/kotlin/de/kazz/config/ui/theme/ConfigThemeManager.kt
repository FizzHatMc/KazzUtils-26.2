package de.kazz.config.ui.theme

/**
 * Manages the active UI theme for the config screen.
 *
 * Themes can be registered and switched at runtime.
 * By default, [DarkTheme] is active.
 */
object ConfigThemeManager {

    private val themes = mutableMapOf<String, ConfigTheme>()
    private var activeTheme: ConfigTheme = DarkTheme

    init {
        register(DarkTheme)
    }

    /**
     * Register a theme so it can be selected by the user.
     */
    fun register(theme: ConfigTheme) {
        themes[theme.name] = theme
    }

    /**
     * Get the currently active theme.
     */
    fun getActive(): ConfigTheme = activeTheme

    /**
     * Set the active theme by name.
     * Returns true if the theme was found and applied.
     */
    fun setActive(name: String): Boolean {
        val theme = themes[name] ?: return false
        activeTheme = theme
        return true
    }

    /**
     * Get all registered themes.
     */
    fun getAll(): Collection<ConfigTheme> = themes.values

    /**
     * Get a theme by name.
     */
    fun get(name: String): ConfigTheme? = themes[name]
}