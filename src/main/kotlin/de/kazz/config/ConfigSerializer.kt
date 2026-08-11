package de.kazz.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Handles serialization and deserialization of the config tree to/from JSON.
 *
 * The JSON structure mirrors the config hierarchy:
 * ```json
 * {
 *   "combat": {
 *     "dungeon": {
 *       "bossTimer": true,
 *       "dianaFeatures": {
 *         "dianaWaypoint": false,
 *         "waypointRange": 50
 *       }
 *     },
 *     "general": {
 *       "autoHeal": true
 *     }
 *   }
 * }
 * ```
 */
object ConfigSerializer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serialize all categories and their properties into a JSON string.
     */
    fun serialize(categories: List<ConfigCategory>): String {
        val root = JsonObject()
        for (category in categories) {
            val categoryObj = JsonObject()
            for (subCategory in category.subCategories) {
                val subCategoryObj = JsonObject()
                serializeProperties(subCategoryObj, subCategory.directProperties)
                for (group in subCategory.groups) {
                    val groupObj = JsonObject()
                    serializeProperties(groupObj, group.properties)
                    subCategoryObj.add(group.keyName(), groupObj)
                }
                categoryObj.add(subCategory.keyName(), subCategoryObj)
            }
            root.add(category.keyName(), categoryObj)
        }
        return gson.toJson(root)
    }

    /**
     * Deserialize a JSON string and apply the stored values to the config tree.
     */
    fun deserialize(json: String, categories: List<ConfigCategory>) {
        val root = gson.fromJson(json, JsonObject::class.java) ?: return
        for (category in categories) {
            val categoryObj = root.getAsJsonObject(category.keyName()) ?: continue
            for (subCategory in category.subCategories) {
                val subCategoryObj = categoryObj.getAsJsonObject(subCategory.keyName()) ?: continue
                deserializeProperties(subCategoryObj, subCategory.directProperties)
                for (group in subCategory.groups) {
                    val groupObj = subCategoryObj.getAsJsonObject(group.keyName()) ?: continue
                    deserializeProperties(groupObj, group.properties)
                }
            }
        }
    }

    // ── Private helpers ──────────────────────────────────

    /**
     * Write each property's value to the JSON object using its [key-only] (last segment of the dot path).
     */
    private fun serializeProperties(obj: JsonObject, properties: List<ConfigProperty<*>>) {
        for (prop in properties) {
            if (prop.type == ConfigType.ACTION_BUTTON) continue // skip action buttons
            val value = prop.serializeValue()
            obj.add(prop.keyName(), toJsonElement(value, prop.type))
        }
    }

    /**
     * Read each property's value from the JSON object and apply it.
     */
    @Suppress("UNCHECKED_CAST")
    private fun deserializeProperties(obj: JsonObject, properties: List<ConfigProperty<*>>) {
        for (prop in properties) {
            if (prop.type == ConfigType.ACTION_BUTTON) continue
            val element = obj.get(prop.keyName()) ?: continue
            val stringValue = fromJsonElement(element)
            val typedProp = prop as ConfigProperty<Any>
            typedProp.currentValue = typedProp.deserializeValue(stringValue)
        }
    }

    /**
     * Convert a string value to the appropriate [JsonElement] based on the type.
     */
    private fun toJsonElement(value: String, type: ConfigType): JsonElement = when (type) {
        ConfigType.BOOLEAN -> JsonPrimitive(value.toBoolean())
        ConfigType.INT -> JsonPrimitive(value.toInt())
        ConfigType.DOUBLE -> JsonPrimitive(value.toDouble())
        ConfigType.STRING -> JsonPrimitive(value)
        ConfigType.ENUM -> JsonPrimitive(value)
        ConfigType.COLOR -> JsonPrimitive(value)
        ConfigType.ACTION_BUTTON -> JsonPrimitive("")
    }

    /**
     * Convert a [JsonElement] back to a string value.
     */
    private fun fromJsonElement(element: JsonElement): String = when {
        element.isJsonPrimitive -> element.asString
        else -> element.toString()
    }

    private fun ConfigCategory.keyName(): String = key

    private fun ConfigSubCategory.keyName(): String = key.substringAfterLast('.')

    private fun ConfigGroup.keyName(): String = key.substringAfterLast('.')

    private fun ConfigProperty<*>.keyName(): String = key.substringAfterLast('.')
}