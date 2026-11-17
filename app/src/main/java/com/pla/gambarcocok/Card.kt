package com.pla.gambarcocok

data class Card(
    val emoji: String,
    val id: String = System.nanoTime().toString() + Math.random().toString().takeLast(6),
    val layer: Int = 0,          // Urutan tumpukan (0 = paling bawah, makin besar makin atas)
    val xPosition: Float = 0.5f, // Posisi X bebas (0.0 sampai 1.0)
    val yPosition: Float = 0.5f, // Posisi Y bebas (0.0 sampai 1.0)
    val rotation: Float = 0f,    // Rotasi acak
    var locked: Boolean = false,
    var flipped: Boolean = false,
    var matched: Boolean = false
)