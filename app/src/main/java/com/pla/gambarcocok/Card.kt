package com.pla.gambarcocok

data class Card(
    val emoji: String,
    val id: String = System.nanoTime().toString() + Math.random().toString().takeLast(6),
    val layer: Int = 0,
    val cellKey: Int = -1,          // posisi sel grid (0..n-1), sama untuk kartu yang ditumpuk
    val rotation: Float = 0f,
    var locked: Boolean = false,
    var flipped: Boolean = false,
    var matched: Boolean = false
)