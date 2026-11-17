package com.pla.gambarcocok

data class LevelConfig(
    val totalCards: Int,
    val cols: Int,
    val rows: Int,
    val layers: Int,
    val stackedCount: Int,      // berapa kartu yang ditumpuk di sel yang sudah dihuni
    val useRotation: Boolean,
    val timeLimitSec: Int       // 0 = tanpa timer
)

object LevelConfigs {
    fun forLevel(level: Int): LevelConfig {
        return when {
            level == 1 -> LevelConfig(6, 3, 2, 1, 0, false, 0)
            level == 2 -> LevelConfig(8, 4, 2, 1, 0, false, 0)
            level == 3 -> LevelConfig(10, 5, 2, 1, 0, false, 0)
            level == 4 -> LevelConfig(12, 4, 3, 2, 4, false, 0)
            level == 5 -> LevelConfig(14, 5, 3, 2, 6, false, 0)
            level == 6 -> LevelConfig(16, 4, 4, 2, 8, true, 0)
            level == 7 -> LevelConfig(18, 6, 3, 2, 10, true, 0)
            level == 8 -> LevelConfig(20, 5, 4, 3, 12, true, 0)
            level == 9 -> LevelConfig(22, 6, 4, 3, 14, true, 0)
            level == 10 -> LevelConfig(24, 6, 4, 3, 16, true, 160)
            else -> LevelConfig(24, 6, 4, 3, 16, true, 160)
        }
    }
}