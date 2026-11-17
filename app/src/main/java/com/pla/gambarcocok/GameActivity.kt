package com.pla.gambarcocok

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.min

class GameActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper
    private lateinit var sound: SoundHelper
    private lateinit var engine: GameEngine
    private lateinit var boardContainer: FrameLayout
    private lateinit var txtGameTitle: TextView
    private lateinit var txtPairs: TextView
    private lateinit var aiBox: TextView

    private var mode = "offline"
    private var currentLevel = 1
    private var maxLevel = 1
    private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())

    private val cardViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = getSharedPreferences("gambarCocok", MODE_PRIVATE)
        speech = SpeechHelper(this)
        sound = SoundHelper()

        mode = intent.getStringExtra("mode") ?: "offline"
        maxLevel = prefs.getInt("maxLevel", 1)
        currentLevel = maxLevel

        boardContainer = findViewById(R.id.boardContainer)
        txtGameTitle = findViewById(R.id.txtGameTitle)
        txtPairs = findViewById(R.id.txtPairs)
        aiBox = findViewById(R.id.aiBox)

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            speech.speak("Kembali ke beranda")
            finish()
        }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { prevLevel() }
        findViewById<Button>(R.id.btnReplay).setOnClickListener { loadLevel(currentLevel) }
        findViewById<Button>(R.id.btnNext).setOnClickListener { nextLevel() }

        loadLevel(currentLevel)
    }

    private fun loadLevel(level: Int) {
        currentLevel = level
        isProcessing = false
        cardViews.clear()
        boardContainer.removeAllViews()
        aiBox.visibility = View.GONE
        findViewById<Button>(R.id.btnPrev).isEnabled = level > 1
        findViewById<Button>(R.id.btnNext).isEnabled = false

        val config = LevelConfigs.forLevel(level)
        engine = GameEngine(config)
        engine.generate()

        txtGameTitle.text = if (mode == "online") getString(R.string.level_online, level)
            else getString(R.string.level, level)
        updatePairsLeft()

        renderBoard(config)
        speech.speak("Level $level siap. ${engine.totalPairs} pasang gambar.")
    }

    private fun updatePairsLeft() {
        txtPairs.text = getString(R.string.pairs_left, engine.remainingPairs())
    }

    private fun renderBoard(config: LevelConfig) {
        val w = boardContainer.width
        val h = boardContainer.height
        // fallback jika belum terukur
        val boardW = if (w > 0) w else resources.displayMetrics.widthPixels - 40
        val boardH = if (h > 0) h else resources.displayMetrics.heightPixels / 2

        val padding = 10
        val cellW = (boardW - padding * 2f) / config.cols
        val cellH = (boardH - padding * 2f) / config.rows
        val baseSize = min(cellW, cellH) * 0.95f

        for (card in engine.cards) {
            if (card.matched) continue
            val view = layoutInflater.inflate(R.layout.item_card, boardContainer, false)
            val tv = view.findViewById<TextView>(R.id.cardText)
            renderCardFace(tv, card)

            val lp = FrameLayout.LayoutParams(baseSize.toInt(), baseSize.toInt())
            val offsetX = if (card.locked) padding else padding + 4
            val offsetY = if (card.locked) padding else padding + 4
            lp.leftMargin = (card.col * cellW + offsetX).toInt()
            lp.topMargin = (card.row * cellH + offsetY).toInt()
            view.layoutParams = lp
            view.rotation = card.rotation
            view.elevation = card.layer * 6f
            view.alpha = if (card.locked) 0.6f else 1f

            if (!card.locked) {
                view.setOnClickListener { onCardClick(card) }
            }
            cardViews[card.id] = view
            boardContainer.addView(view)
        }
    }

    private fun renderCardFace(tv: TextView, card: Card) {
        when {
            card.locked -> {
                tv.text = "🔒"
                tv.setBackgroundResource(R.drawable.bg_card_locked)
                tv.setTextColor(0xFF3D8EC9.toInt())
                tv.alpha = 0.6f
            }
            card.flipped || card.matched -> {
                tv.text = card.emoji
                tv.setBackgroundResource(R.drawable.bg_card_face)
                tv.alpha = 1f
            }
            else -> {
                tv.text = "❓"
                tv.setBackgroundResource(R.drawable.bg_card_back)
                tv.alpha = 1f
            }
        }
    }

    private fun onCardClick(card: Card) {
        if (isProcessing) return
        val idx = engine.cards.indexOfFirst { it.id == card.id }
        val flippedCard = engine.flip(idx) ?: return
        sound.click()
        speech.speak(EmojiData.all.firstOrNull { it.emoji == flippedCard.emoji }?.name ?: "")

        val view = cardViews[card.id] ?: return
        val tv = view.findViewById<TextView>(R.id.cardText)
        renderCardFace(tv, engine.cards[idx])

        if (engine.flipped.size == 2) {
            isProcessing = true
            handler.postDelayed({ checkMatch() }, 700)
        }
    }

    private fun checkMatch() {
        val (isMatch, emoji) = engine.checkMatch()
        // refresh board
        refreshBoard()
        if (isMatch && emoji != null) {
            sound.match()
            updatePairsLeft()
            speech.speak("Cocok!")
            showAiFact(emoji)
            if (engine.isWin()) {
                handler.postDelayed({ winLevel() }, 600)
            }
        } else {
            speech.speak("Belum cocok")
        }
        isProcessing = false
    }

    private fun refreshBoard() {
        for ((id, view) in cardViews) {
            val card = engine.cards.firstOrNull { it.id == id }
            if (card == null || card.matched) {
                view.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(250)
                    .withEndAction { view.visibility = View.GONE }.start()
                continue
            }
            val tv = view.findViewById<TextView>(R.id.cardText)
            renderCardFace(tv, card)
            if (!card.locked && view.hasOnClickListeners().not()) {
                view.setOnClickListener { onCardClick(card) }
            }
            if (card.locked) {
                view.setOnClickListener(null)
                view.alpha = 0.6f
            } else {
                view.alpha = 1f
            }
        }
    }

    private fun showAiFact(emoji: String) {
        aiBox.visibility = View.VISIBLE
        if (mode == "online") {
            val key = pickApiKey()
            if (key.isNullOrBlank()) {
                aiBox.text = "🤖 ${EmojiData.factOf(emoji)}"
                speech.speak("Fakta: ${EmojiData.factOf(emoji)}")
                return
            }
            aiBox.text = "🤖 Mencari fakta tentang $emoji..."
            GeminiApi.fetchFact(key, emoji) { fact ->
                runOnUiThread {
                    val result = fact ?: EmojiData.factOf(emoji)
                    aiBox.text = "🤖 $result"
                    speech.speak("Fakta: $result")
                }
            }
        } else {
            val fact = EmojiData.factOf(emoji)
            aiBox.text = "💡 $fact"
            speech.speak(fact)
        }
        aiBox.animate().translationY(0f).alpha(1f).setDuration(250)
            .setInterpolator(OvershootInterpolator()).start()
    }

    private fun pickApiKey(): String? {
        for (i in 1..4) {
            val k = prefs.getString("api$i", "").orEmpty()
            if (k.isNotBlank()) return k
        }
        return null
    }

    private fun winLevel() {
        sound.win()
        if (currentLevel >= maxLevel) {
            maxLevel = currentLevel + 1
            prefs.edit().putInt("maxLevel", maxLevel).apply()
        }
        val view = layoutInflater.inflate(R.layout.dialog_win, null)
        view.findViewById<TextView>(R.id.winText).text =
            getString(R.string.win_text, currentLevel)
        val dlg = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnNextLevel).setOnClickListener {
            dlg.dismiss()
            loadLevel(currentLevel + 1)
        }
        view.findViewById<Button>(R.id.btnMainMenu).setOnClickListener {
            dlg.dismiss(); finish()
        }
        dlg.show()
        speech.speak("Selamat! Level $currentLevel selesai.")
    }

    private fun prevLevel() {
        if (currentLevel > 1) loadLevel(currentLevel - 1)
    }

    private fun nextLevel() {
        if (currentLevel < maxLevel) loadLevel(currentLevel + 1)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && cardViews.isEmpty()) {
            // re-render setelah ukuran board diketahui
            renderBoard(LevelConfigs.forLevel(currentLevel))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speech.shutdown()
        sound.release()
        super.onDestroy()
    }
}