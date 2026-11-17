package com.pla.gambarcocok

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var txtTimer: TextView

    private var mode = "offline"
    private var currentLevel = 1
    private var maxLevel = 1
    private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())

    private val cardViews = mutableMapOf<String, View>()

    // Timer
    private var timerRunning = false
    private var timeLeft = 0
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!timerRunning) return
            timeLeft--
            updateTimerUI()
            if (timeLeft <= 0) {
                timerRunning = false
                showGameOver()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

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
        txtTimer = findViewById(R.id.txtTimer)

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            timerRunning = false
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

        // Timer
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
        if (config.timeLimitSec > 0) {
            txtTimer.visibility = View.VISIBLE
            timeLeft = config.timeLimitSec
            updateTimerUI()
            timerRunning = true
            handler.postDelayed(timerRunnable, 1000)
        } else {
            txtTimer.visibility = View.GONE
        }

        renderBoard(config)
        speech.speak("Level $level siap. ${engine.totalPairs} pasang gambar.")
    }

    private fun updateTimerUI() {
        val m = timeLeft / 60
        val s = timeLeft % 60
        txtTimer.text = "⏱ %d:%02d".format(m, s)
        val color = when {
            timeLeft > 60 -> ContextCompat.getColor(this, R.color.timer_green)
            timeLeft > 20 -> ContextCompat.getColor(this, R.color.timer_yellow)
            else -> ContextCompat.getColor(this, R.color.timer_red)
        }
        txtTimer.setTextColor(color)
    }

    private fun updatePairsLeft() {
        txtPairs.text = getString(R.string.pairs_left, engine.remainingPairs())
    }

    private fun renderBoard(config: LevelConfig) {
        val w = boardContainer.width
        val h = boardContainer.height
        val boardW = if (w > 0) w else resources.displayMetrics.widthPixels - 40
        val boardH = if (h > 0) h else resources.displayMetrics.heightPixels / 2

        val padding = 10
        val cellW = (boardW - padding * 2f) / config.cols
        val cellH = (boardH - padding * 2f) / config.rows
        val baseSize = min(cellW, cellH) * 0.92f

        // Kelompokkan kartu per cellKey
        val byCell = engine.cards.filter { !it.matched }.groupBy { it.cellKey }

        for ((cellKey, group) in byCell) {
            val col = cellKey % config.cols
            val row = cellKey / config.cols

            // Urutkan: layer rendah dulu (belakang), layer tinggi di atas
            val sorted = group.sortedBy { it.layer }

            for (card in sorted) {
                val view = layoutInflater.inflate(R.layout.item_card, boardContainer, false)
                val tv = view.findViewById<TextView>(R.id.cardText)
                renderCardFace(tv, card)

                val lp = FrameLayout.LayoutParams(baseSize.toInt(), baseSize.toInt())
                // Offset tumpukan: makin tinggi layer, makin tergeser
                val offset = card.layer * 10
                lp.leftMargin = (col * cellW + padding + offset).toInt()
                lp.topMargin = (row * cellH + padding + offset).toInt()
                view.layoutParams = lp
                view.rotation = card.rotation
                view.elevation = card.layer * 8f
                view.alpha = if (card.locked) 0.65f else 1f

                if (!card.locked) {
                    view.setOnClickListener { onCardClick(card) }
                }
                cardViews[card.id] = view
                boardContainer.addView(view)
            }
        }
    }

    private fun renderCardFace(tv: TextView, card: Card) {
        when {
            card.locked -> {
                tv.text = "🔒"
                tv.setBackgroundResource(R.drawable.bg_card_locked)
                tv.alpha = 0.65f
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
        refreshBoard()
        if (isMatch && emoji != null) {
            sound.match()
            updatePairsLeft()
            speech.speak("Cocok!")
            showAiFact(emoji)
            if (engine.isWin()) {
                timerRunning = false
                handler.removeCallbacks(timerRunnable)
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
            if (!card.locked) {
                view.setOnClickListener { onCardClick(card) }
                view.alpha = 1f
            } else {
                view.setOnClickListener(null)
                view.alpha = 0.65f
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

    private fun showGameOver() {
        sound.error()
        val view = layoutInflater.inflate(R.layout.dialog_gameover, null)
        view.findViewById<TextView>(R.id.goText).text =
            "Kamu kehabisan waktu di Level $currentLevel.\nSisa pasangan: ${engine.remainingPairs()}"
        val dlg = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            dlg.dismiss()
            loadLevel(currentLevel)
        }
        view.findViewById<Button>(R.id.btnGoMenu).setOnClickListener {
            dlg.dismiss(); finish()
        }
        dlg.show()
        speech.speak("Waktu habis. Coba lagi.")
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
            renderBoard(LevelConfigs.forLevel(currentLevel))
        }
    }

    override fun onPause() {
        super.onPause()
        timerRunning = false
    }

    override fun onResume() {
        super.onResume()
        // hanya lanjut jika timer masih punya waktu dan level >= 10
        val cfg = LevelConfigs.forLevel(currentLevel)
        if (cfg.timeLimitSec > 0 && timeLeft > 0 && engine.cards.isNotEmpty() && !engine.isWin()) {
            timerRunning = true
            handler.postDelayed(timerRunnable, 1000)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speech.shutdown()
        sound.release()
        super.onDestroy()
    }
}