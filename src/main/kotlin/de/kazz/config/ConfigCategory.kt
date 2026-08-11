package de.kazz.config

/**
 * A category page in the config (e.g., "Combat", "Farming").
 *
 * @param name          Display name of the category
 * @param key           Cleaned key used in dot-paths and JSON (e.g., "combat")
 * @param subCategories List of sub-category pages under this category
 */
class ConfigCategory(
    val name: String,
    val key: String,
    val subCategories: List<ConfigSubCategory>
)

/**
 * A sub-category page within a category (e.g., "Dungeon", "Garden").
 *
 * @param name             Display name of the sub-category
 * @param key              Dot-separated path prefix (e.g., "combat.dungeon")
 * @param directProperties Config properties directly in this sub-category (no group)
 * @param groups           Collapsible groups (hiders) within this sub-category
 */
class ConfigSubCategory(
    val name: String,
    val key: String,
    val directProperties: List<ConfigProperty<*>>,
    val groups: List<ConfigGroup>
)

/**
 * A collapsible group (hider) within a sub-category (e.g., "Diana Features").
 *
 * @param name        Display name of the group
 * @param description Tooltip / help text for the group
 * @param key         Dot-separated path prefix (e.g., "combat.dungeon.dianaFeatures")
 * @param properties  Config properties within this group
 */
class ConfigGroup(
    val name: String,
    val description: String,
    val key: String,
    val properties: List<ConfigProperty<*>>
)