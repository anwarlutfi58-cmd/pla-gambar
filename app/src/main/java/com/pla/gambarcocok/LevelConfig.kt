package com.pla.gambarcocok

data class LevelConfig(
    val totalCards: Int,
    val cols: Int,
    val rows: Int,
    val layers: Int,
    val useRotation: Boolean
)

object LevelConfigs {
    fun forLevel(level: Int): LevelConfig {
        return when {
            level == 1 -> LevelConfig(6, 3, 2, 1, false)
            level == 2 -> LevelConfig(8, 4, 2, 1, false)
            level == 3 -> LevelConfig(10, 5, 2, 1, false)
            level == 4 -> LevelConfig(12, 4, 3, 2, false)
            level == 5 -> LevelConfig(14, 5, 3, 2, false)
            level == 6 -> LevelConfig(16, 4, 4, 2, false)
            level == 7 -> LevelConfig(18, 6, 3, 2, true)
            level == 8 -> LevelConfig(20, 5, 4, 3, true)
            level == 9 -> LevelConfig(22, 6, 4, 3, true)
            else -> LevelConfig(24, 6, 4, 3, true)
        }
    }
}