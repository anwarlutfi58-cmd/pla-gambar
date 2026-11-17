package com.pla.gambarcocok

data class Card(
    val emoji: String,
    val id: String = System.nanoTime().toString() + Math.random().toString().takeLast(6),
    val layer: Int = 0,
    val col: Int = 0,
    val row: Int = 0,
    val rotation: Float = 0f,
    var locked: Boolean = false,
    var flipped: Boolean = false,
    var matched: Boolean = false
)