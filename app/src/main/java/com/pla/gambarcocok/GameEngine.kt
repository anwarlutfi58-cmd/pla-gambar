package com.pla.gambarcocok

import kotlin.random.Random

class GameEngine(private val config: LevelConfig) {
    val cards = mutableListOf<Card>()
    var flipped = mutableListOf<Card>()
    var matchedPairs = 0
    var totalPairs = config.totalCards / 2

    fun generate() {
        cards.clear()
        flipped.clear()
        matchedPairs = 0

        val pairCount = config.totalCards / 2
        val selected = EmojiData.all.shuffled(Random(System.currentTimeMillis())).take(pairCount)

        val raw = mutableListOf<Card>()
        for ((idx, fact) in selected.withIndex()) {
            raw.add(Card(emoji = fact.emoji, id = "a_${fact.emoji}_$idx"))
            raw.add(Card(emoji = fact.emoji, id = "b_${fact.emoji}_$idx"))
        }

        raw.shuffle(Random(System.currentTimeMillis() + 1))

        // Assign grid positions
        val cols = config.cols
        val total = raw.size
        raw.forEachIndexed { i, c ->
            val col = i % cols
            val row = i / cols
            val layer = when {
                config.layers == 1 -> 0
                config.layers == 2 -> if (i % 2 == 0) 1 else 0
                else -> when {
                    i % 4 == 0 -> 2
                    i % 4 == 1 -> 1
                    else -> 0
                }
            }
            val rotation = if (config.useRotation) {
                (Random.nextFloat() * 24f - 12f)
            } else 0f
            raw[i] = c.copy(col = col, row = row, layer = layer, rotation = rotation)
        }

        cards.addAll(raw)
        recomputeLocks()
    }

    fun recomputeLocks() {
        // Kartu terkunci jika ada kartu lain (belum matched) di posisi (col,row) sama
        // dengan layer lebih tinggi.
        for (i in cards.indices) {
            val c = cards[i]
            if (c.matched) continue
            var maxLayerHere = c.layer
            for (j in cards.indices) {
                if (i == j) continue
                val o = cards[j]
                if (o.matched) continue
                if (o.col == c.col && o.row == c.row && o.layer > maxLayerHere) {
                    maxLayerHere = o.layer
                }
            }
            cards[i] = c.copy(locked = c.layer < maxLayerHere)
        }
    }

    fun canFlip(card: Card): Boolean =
        !card.matched && !card.locked && !card.flipped && flipped.size < 2

    fun flip(index: Int): Card? {
        if (index !in cards.indices) return null
        val c = cards[index]
        if (!canFlip(c)) return null
        cards[index] = c.copy(flipped = true)
        flipped.add(cards[index])
        return cards[index]
    }

    fun checkMatch(): Pair<Boolean, String?> {
        if (flipped.size < 2) return Pair(false, null)
        val a = flipped[0]
        val b = flipped[1]
        val isMatch = a.emoji == b.emoji
        if (isMatch) {
            for (i in cards.indices) {
                val c = cards[i]
                if (c.emoji == a.emoji && c.flipped) {
                    cards[i] = c.copy(matched = true, flipped = false)
                }
            }
            matchedPairs++
            recomputeLocks()
        } else {
            for (i in cards.indices) {
                val c = cards[i]
                if (c.flipped && !c.matched) {
                    cards[i] = c.copy(flipped = false)
                }
            }
        }
        val matchedEmoji = if (isMatch) a.emoji else null
        flipped.clear()
        return Pair(isMatch, matchedEmoji)
    }

    fun isWin(): Boolean = matchedPairs >= totalPairs
    fun remainingPairs(): Int = totalPairs - matchedPairs
}