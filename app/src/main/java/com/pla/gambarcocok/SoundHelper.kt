package com.pla.gambarcocok

import android.media.AudioManager
import android.media.ToneGenerator

class SoundHelper {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    var enabled = true

    fun click() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60) }
    fun match() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_ACK, 150) }
    fun win() { if (enabled) tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300) }
    fun error() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_NACK, 150) }
    fun release() { tone.release() }
}