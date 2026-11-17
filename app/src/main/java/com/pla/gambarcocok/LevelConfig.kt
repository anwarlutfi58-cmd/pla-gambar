package com.pla.gambarcocok

data class LevelConfig(
    val totalCards: Int,
    val timeLimitSec: Int,
    val useRotation: Boolean
)

object LevelConfigs {
    fun forLevel(level: Int): LevelConfig {
        return when {
            level == 1 -> LevelConfig(8, 0, false)  // Awal: 8 kartu
            level == 2 -> LevelConfig(12, 0, false)
            level == 3 -> LevelConfig(16, 0, true)
            level == 4 -> LevelConfig(20, 0, true)
            level == 5 -> LevelConfig(24, 0, true)
            level == 6 -> LevelConfig(28, 0, true)
            level == 7 -> LevelConfig(32, 0, true)
            level == 8 -> LevelConfig(36, 0, true)
            level == 9 -> LevelConfig(40, 0, true)
            level == 10 -> LevelConfig(48, 160, true) // Level 10+ mulai pakai waktu
            else -> LevelConfig(50, 160, true) // Endless
        }
    }
}