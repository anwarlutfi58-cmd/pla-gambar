package com.pla.gambarcocok

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class GameActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper
    private lateinit var engine: GameEngine
    private lateinit var boardContainer: FrameLayout
    private lateinit var txtGameTitle: TextView
    private lateinit var txtPairs: TextView
    private lateinit var txtTimer: TextView
    private lateinit var aiBox: TextView

    private var mode = "offline"
    private var currentLevel = 1
    private var maxLevel = 1
    private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())

    private val cardViews = mutableMapOf<String, View>()

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

        mode = intent.getStringExtra("mode") ?: "offline"
        maxLevel = prefs.getInt("maxLevel", 1)
        currentLevel = maxLevel

        boardContainer = findViewById(R.id.boardContainer)
        txtGameTitle = findViewById(R.id.txtGameTitle)
        txtPairs = findViewById(R.id.txtPairs)
        txtTimer = findViewById(R.id.txtTimer)
        aiBox = findViewById(R.id.aiBox)

        findViewById<Button>(R.id.btnHome).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { if (currentLevel > 1) loadLevel(currentLevel - 1) }
        findViewById<Button>(R.id.btnReplay).setOnClickListener { loadLevel(currentLevel) }
        findViewById<Button>(R.id.btnNext).setOnClickListener { if (currentLevel < maxLevel) loadLevel(currentLevel + 1) }

        loadLevel(currentLevel)
    }

    private fun loadLevel(level: Int) {
        currentLevel = level
        isProcessing = false
        cardViews.clear()
        boardContainer.removeAllViews()
        aiBox.visibility = View.GONE

        val config = LevelConfigs.forLevel(level)
        engine = GameEngine(config)
        engine.generate()

        txtGameTitle.text = "Level $level"
        updatePairsLeft()

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

        boardContainer.post {
            renderBoard(config)
        }
        speech.speak("Level $level siap.")
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
        txtPairs.text = "Sisa pasangan: ${engine.remainingPairs()}"
    }

    private fun renderBoard(config: LevelConfig) {
        val boardW = boardContainer.width.toFloat()
        val boardH = boardContainer.height.toFloat()
        if (boardW == 0f || boardH == 0f) return

        // Ukuran kartu disesuaikan dengan layar
        val cardSize = (boardW / 4.5f).coerceAtMost(boardH / 5f).toInt()

        // Urutkan berdasarkan layer (dari bawah ke atas) agar z-order benar
        val sortedCards = engine.cards.filter { !it.matched }.sortedBy { it.layer }

        for (card in sortedCards) {
            val view = layoutInflater.inflate(R.layout.item_card, boardContainer, false)
            val tv = view.findViewById<TextView>(R.id.cardText)
            renderCardFace(tv, card)

            val lp = FrameLayout.LayoutParams(cardSize, cardSize)
            // Konversi posisi float (0..1) ke pixel absolut
            lp.leftMargin = (card.xPosition * (boardW - cardSize)).toInt()
            lp.topMargin = (card.yPosition * (boardH - cardSize)).toInt()
            
            view.layoutParams = lp
            view.rotation = card.rotation
            view.elevation = card.layer * 4f // Makin tinggi layer, makin naik bayangannya
            
            // Efek visual untuk kartu yang terkunci
            view.alpha = if (card.locked) 0.7f else 1f
            if (card.locked) {
                view.setBackgroundColor(0x44000000) // Gelap sedikit
            } else {
                view.setBackgroundColor(0x00000000)
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
                tv.alpha = 0.8f
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
        speech.speak(EmojiData.all.firstOrNull { it.emoji == flippedCard.emoji }?.name ?: "")

        val view = cardViews[card.id] ?: return
        
        // Animasi balik
        ObjectAnimator.ofFloat(view, "rotationY", 0f, 180f).setDuration(200).start()
        
        handler.postDelayed({
            val tv = view.findViewById<TextView>(R.id.cardText)
            renderCardFace(tv, engine.cards[idx])
        }, 100)

        if (engine.flipped.size == 2) {
            isProcessing = true
            handler.postDelayed({ checkMatch() }, 800)
        }
    }

    private fun checkMatch() {
        val (isMatch, emoji) = engine.checkMatch()
        
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
                view.alpha = 0.7f
            }
        }

        if (isMatch && emoji != null) {
            updatePairsLeft()
            speech.speak("Cocok!")
            if (engine.isWin()) {
                handler.postDelayed({ winLevel() }, 600)
            }
        }
        isProcessing = false
    }

    private fun winLevel() {
        if (currentLevel >= maxLevel) {
            maxLevel = currentLevel + 1
            prefs.edit().putInt("maxLevel", maxLevel).apply()
        }
        val view = layoutInflater.inflate(R.layout.dialog_win, null)
        view.findViewById<TextView>(R.id.winText).text = "Hebat! Kamu menyelesaikan Level $currentLevel."
        val dlg = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnNextLevel).setOnClickListener {
            dlg.dismiss()
            loadLevel(currentLevel + 1)
        }
        view.findViewById<Button>(R.id.btnMainMenu).setOnClickListener {
            dlg.dismiss(); finish()
        }
        dlg.show()
        speech.speak("Selamat! Level selesai.")
    }

    private fun showGameOver() {
        val view = layoutInflater.inflate(R.layout.dialog_gameover, null)
        view.findViewById<TextView>(R.id.goText).text = "Waktu habis di Level $currentLevel."
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

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speech.shutdown()
        super.onDestroy()
    }
}