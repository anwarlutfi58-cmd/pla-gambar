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

        val totalCells = config.cols * config.rows
        val totalCards = raw.size

        // Tentukan berapa sel yang kosong (single) dan berapa sel yang diisi 2-3 kartu (stacked)
        val stacked = config.stackedCount.coerceAtMost(totalCards / 2)

        // Buat daftar slot: setiap slot punya cellKey dan layer
        data class Slot(val cell: Int, val layer: Int)
        val slots = mutableListOf<Slot>()
        val cellFill = IntArray(totalCells) { 0 } // jumlah kartu per cell

        // Isi dulu 1 kartu per sel (sebanyak mungkin)
        val cellsOrder = (0 until totalCells).shuffled(Random(System.currentTimeMillis() + 2))
        var remaining = totalCards
        for (cell in cellsOrder) {
            if (remaining <= 0) break
            slots.add(Slot(cell, 0))
            cellFill[cell] = 1
            remaining--
        }

        // Tambahkan kartu stacked di sel yang sudah ada isinya
        var stackedLeft = stacked
        for (cell in cellsOrder) {
            if (stackedLeft <= 0) break
            if (cellFill[cell] == 0) continue
            val maxLayerHere = if (config.layers >= 3) 2 else 1
            var layer = cellFill[cell]
            while (layer <= maxLayerHere && stackedLeft > 0 && remaining > 0) {
                slots.add(Slot(cell, layer))
                cellFill[cell]++
                layer++
                stackedLeft--
                remaining--
            }
        }

        // Jika masih ada sisa kartu (slot tidak cukup), tambah ke sel baru
        while (remaining > 0) {
            for (cell in cellsOrder) {
                if (remaining <= 0) break
                slots.add(Slot(cell, cellFill[cell]))
                cellFill[cell]++
                remaining--
            }
        }

        // Pastikan jumlah slot = jumlah kartu
        while (slots.size < totalCards) {
            val cell = cellsOrder.random()
            slots.add(Slot(cell, cellFill[cell]))
            cellFill[cell]++
        }
        val finalSlots = slots.take(totalCards)

        // Assign slot ke kartu
        for (i in raw.indices) {
            val s = finalSlots[i]
            val rot = if (config.useRotation) (Random.nextFloat() * 20f - 10f) else 0f
            raw[i] = raw[i].copy(cellKey = s.cell, layer = s.layer, rotation = rot)
        }

        cards.addAll(raw)
        recomputeLocks()
    }

    fun recomputeLocks() {
        // Kartu terkunci jika ada kartu lain (belum matched) di cellKey sama dengan layer lebih tinggi
        for (i in cards.indices) {
            val c = cards[i]
            if (c.matched) continue
            var maxLayerHere = c.layer
            for (j in cards.indices) {
                if (i == j) continue
                val o = cards[j]
                if (o.matched) continue
                if (o.cellKey == c.cellKey && o.layer > maxLayerHere) {
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