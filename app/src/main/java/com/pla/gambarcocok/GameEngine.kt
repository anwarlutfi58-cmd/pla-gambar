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

        val totalCards = raw.size
        
        // KITA AKAN MEMBUAT POSISI ACAK (TUMPUKAN BEBAS)
        // Kita bayangkan ini seperti meja. Kita buat titik-titik koordinat.
        val random = Random(System.currentTimeMillis())
        
        // Tentukan titik tengah sebagai pusat tumpukan
        val centerX = 0.5f
        val centerY = 0.5f

        raw.forEachIndexed { i, card ->
            // Posisi acak di sekitar tengah (X dan Y), dengan sebaran yang bikin tumpang tindih
            val rx = centerX + (random.nextFloat() - 0.5f) * 0.6f
            val ry = centerY + (random.nextFloat() - 0.5f) * 0.6f
            
            // Rotasi acak biar berantakan
            val rot = if (config.useRotation) (random.nextFloat() * 30f - 15f) else 0f
            
            // Urutan Z (layer) dari bawah ke atas (0 sampai totalCards)
            val layer = i

            raw[i] = card.copy(
                xPosition = rx,  // Posisi X (0.0 - 1.0)
                yPosition = ry,  // Posisi Y (0.0 - 1.0)
                layer = layer,
                rotation = rot
            )
        }

        cards.addAll(raw)
        recomputeLocks()
    }

    fun recomputeLocks() {
        // Logika kunci: 
        // Kartu A terkunci jika ada Kartu B yang posisinya menimpa Kartu A
        // DAN layer Kartu B lebih tinggi dari layer Kartu A.
        
        for (i in cards.indices) {
            val c = cards[i]
            if (c.matched) continue
            
            var isLocked = false
            
            for (j in cards.indices) {
                if (i == j) continue
                val o = cards[j]
                if (o.matched) continue
                
                // Jika o lebih tinggi layernya
                if (o.layer > c.layer) {
                    // Cek apakah posisinya bertumpukan (jarak < 0.15f)
                    val dx = o.xPosition - c.xPosition
                    val dy = o.yPosition - c.yPosition
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    
                    if (distance < 0.15f) {
                        isLocked = true
                        break
                    }
                }
            }
            cards[i] = c.copy(locked = isLocked)
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