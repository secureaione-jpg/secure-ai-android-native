package one.secureai.app.data

import androidx.compose.ui.graphics.Color

enum class ChatBackground(val key: String, val label: String) {
    SYSTEM("system", "Default"),
    WHITE("white", "White"),
    OCEAN("ocean", "Ocean"),
    SUNSET("sunset", "Sunset"),
    FOREST("forest", "Forest"),
    LAVENDER("lavender", "Lavender"),
    MIDNIGHT("midnight", "Midnight"),
    ROSE("rose", "Rose"),
    AURORA("aurora", "Aurora");

    val gradient: List<Color>
        get() = when (this) {
            SYSTEM -> emptyList()
            WHITE -> listOf(Color.White, Color.White)
            OCEAN -> listOf(
                Color(0xFF9EB8D1),
                Color(0xFF85ADD1),
                Color(0xFF6B9ECC),
                Color(0xFF5994C7)
            )
            SUNSET -> listOf(
                Color(0xFFB89EC7),
                Color(0xFFD199A6),
                Color(0xFFE0A68C),
                Color(0xFFEBB885)
            )
            FOREST -> listOf(
                Color(0xFF8CAD94),
                Color(0xFF85B399),
                Color(0xFF7AB894),
                Color(0xFF73AD8C)
            )
            LAVENDER -> listOf(
                Color(0xFFB8ADD1),
                Color(0xFFB3A6D1),
                Color(0xFFAD9ECC),
                Color(0xFFB8ADD9)
            )
            MIDNIGHT -> listOf(
                Color(0xFF2E3352),
                Color(0xFF383D61),
                Color(0xFF40476B),
                Color(0xFF47527A)
            )
            ROSE -> listOf(
                Color(0xFFD1ADB8),
                Color(0xFFD9A6AD),
                Color(0xFFD19EA6),
                Color(0xFFCC99A6)
            )
            AURORA -> listOf(
                Color(0xFF8CADB8),
                Color(0xFF80B8B8),
                Color(0xFF7ABFAD),
                Color(0xFF85C7AD)
            )
        }

    val usesLightText: Boolean
        get() = this != SYSTEM && this != WHITE

    companion object {
        fun fromKey(key: String): ChatBackground =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
