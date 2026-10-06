package ua.edu.cunl.lyceummobile.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ua.edu.cunl.lyceummobile.R

class SilenceMetronome(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleId = soundPool.load(context.applicationContext, R.raw.metronome_tick, 1)
    private var loaded = false
    private var running = false
    private var volume = 0.25f
    private var nextBeat = 0L
    private var streamId = 0

    private val beat = object : Runnable {
        override fun run() {
            if (!running) return
            if (loaded && volume > 0f) {
                if (streamId != 0) soundPool.stop(streamId)
                streamId = soundPool.play(sampleId, volume, volume, 1, 0, 1f)
            }
            nextBeat += 1_000L
            handler.postAtTime(this, nextBeat)
        }
    }

    init {
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (id == sampleId && status == 0) loaded = true
        }
    }

    fun setActive(active: Boolean, volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        if (!active || this.volume <= 0f) {
            stop()
            return
        }
        if (running) return
        running = true
        nextBeat = SystemClock.uptimeMillis()
        handler.postAtTime(beat, nextBeat)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(beat)
        if (streamId != 0) {
            soundPool.stop(streamId)
            streamId = 0
        }
    }

    fun release() {
        stop()
        soundPool.release()
    }
}
