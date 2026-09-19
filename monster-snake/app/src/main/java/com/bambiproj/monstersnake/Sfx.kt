package com.bambiproj.monstersnake

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * All audio is synthesised on the device at first launch, so the app ships with
 * no audio files, needs no network and stays tiny.
 */
object Sfx {
    const val COLLECT = 0
    const val COIN = 1
    const val STAR = 2
    const val POWER = 3
    const val CLICK = 4
    const val WIN = 5
    const val LOSE = 6
    const val UNLOCK = 7
    private const val COUNT = 8

    private const val RATE = 22050

    private var pool: SoundPool? = null
    private val ids = IntArray(COUNT) { 0 }
    private var music: MediaPlayer? = null
    private var musicFile: File? = null

    @Volatile private var loaded = false
    @Volatile private var wantMusic = false

    fun init(ctx: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()

        val app = ctx.applicationContext
        Thread {
            try {
                buildAll(app)
            } catch (t: Throwable) {
                // Audio is a nice-to-have; the game must never fail because of it.
            }
        }.apply { priority = Thread.MIN_PRIORITY }.start()
    }

    private fun buildAll(ctx: Context) {
        val dir = File(ctx.cacheDir, "snd")
        if (!dir.exists()) dir.mkdirs()

        val clips = arrayOf(
            "collect" to ::collectClip,
            "coin" to ::coinClip,
            "star" to ::starClip,
            "power" to ::powerClip,
            "click" to ::clickClip,
            "win" to ::winClip,
            "lose" to ::loseClip,
            "unlock" to ::unlockClip
        )
        for (i in clips.indices) {
            val f = File(dir, clips[i].first + ".wav")
            if (!f.exists() || f.length() < 64) writeWav(f, clips[i].second.invoke())
            ids[i] = pool?.load(f.absolutePath, 1) ?: 0
        }

        val mf = File(dir, "theme.wav")
        if (!mf.exists() || mf.length() < 1024) writeWav(mf, musicClip())
        musicFile = mf
        loaded = true
        if (wantMusic) startMusic()
    }

    // ---------------------------------------------------------------- playback

    fun play(id: Int, rate: Float = 1f, vol: Float = 1f) {
        if (!Prefs.soundOn || !loaded) return
        if (id < 0 || id >= COUNT) return
        val sid = ids[id]
        if (sid != 0) pool?.play(sid, vol, vol, 1, 0, rate)
    }

    fun startMusic() {
        wantMusic = true
        if (!Prefs.musicOn || !loaded) return
        synchronized(this) {
            if (music != null) {
                if (!music!!.isPlaying) music!!.start()
                return
            }
            val f = musicFile ?: return
            try {
                val mp = MediaPlayer()
                mp.setDataSource(f.absolutePath)
                mp.isLooping = true
                mp.setVolume(0.55f, 0.55f)
                mp.prepare()
                mp.start()
                music = mp
            } catch (t: Throwable) {
                music = null
            }
        }
    }

    fun pauseMusic() {
        synchronized(this) {
            try {
                if (music?.isPlaying == true) music?.pause()
            } catch (t: Throwable) {
            }
        }
    }

    fun stopMusic() {
        wantMusic = false
        synchronized(this) {
            try {
                music?.stop()
                music?.release()
            } catch (t: Throwable) {
            }
            music = null
        }
    }

    /** Called when the music switch is flipped in Settings. */
    fun applyMusicSetting() {
        if (Prefs.musicOn) startMusic() else stopMusic()
    }

    fun release() {
        stopMusic()
        try {
            pool?.release()
        } catch (t: Throwable) {
        }
        pool = null
        loaded = false
    }

    // ------------------------------------------------------------- synthesis

    private fun sec(s: Float) = (s * RATE).toInt()

    /** Adds one enveloped tone to the buffer. wave: 0 square, 1 triangle, 2 sine, 3 noise. */
    private fun tone(
        buf: FloatArray, start: Int, dur: Int,
        f0: Float, f1: Float, amp: Float, wave: Int,
        attack: Float = 0.008f, decay: Float = 6f
    ) {
        var phase = 0.0
        val rnd = Random(start * 31 + dur)
        for (i in 0 until dur) {
            val p = start + i
            if (p < 0 || p >= buf.size) continue
            val t = i.toFloat() / dur
            val freq = f0 + (f1 - f0) * t
            phase += freq / RATE
            if (phase > 1.0) phase -= 1.0
            val raw = when (wave) {
                0 -> if (phase < 0.5) 1f else -1f
                1 -> (if (phase < 0.5) phase * 4 - 1 else 3 - phase * 4).toFloat()
                2 -> sin(phase * 2 * PI).toFloat()
                else -> rnd.nextFloat() * 2f - 1f
            }
            val secs = i.toFloat() / RATE
            var env = exp(-decay * secs)
            val atk = attack * RATE
            if (i < atk) env *= i / atk
            // gentle fade at the very end to avoid clicks
            if (t > 0.92f) env *= (1f - t) / 0.08f
            buf[p] += raw * amp * env
        }
    }

    private fun collectClip(): FloatArray {
        val b = FloatArray(sec(0.16f))
        tone(b, 0, sec(0.14f), 700f, 1400f, 0.42f, 1, decay = 9f)
        tone(b, sec(0.02f), sec(0.12f), 1400f, 2100f, 0.16f, 2, decay = 12f)
        return b
    }

    private fun coinClip(): FloatArray {
        val b = FloatArray(sec(0.2f))
        tone(b, 0, sec(0.06f), 988f, 988f, 0.36f, 0, decay = 8f)
        tone(b, sec(0.06f), sec(0.13f), 1319f, 1319f, 0.34f, 0, decay = 7f)
        return b
    }

    private fun starClip(): FloatArray {
        val b = FloatArray(sec(0.4f))
        val notes = floatArrayOf(784f, 988f, 1175f, 1568f)
        for (i in notes.indices) {
            tone(b, sec(0.055f * i), sec(0.2f), notes[i], notes[i], 0.3f, 1, decay = 10f)
        }
        return b
    }

    private fun powerClip(): FloatArray {
        val b = FloatArray(sec(0.45f))
        tone(b, 0, sec(0.26f), 380f, 1250f, 0.34f, 0, decay = 5f)
        val notes = floatArrayOf(1046f, 1318f, 1568f)
        for (i in notes.indices) {
            tone(b, sec(0.16f + 0.06f * i), sec(0.22f), notes[i], notes[i], 0.24f, 1, decay = 9f)
        }
        return b
    }

    private fun clickClip(): FloatArray {
        val b = FloatArray(sec(0.08f))
        tone(b, 0, sec(0.05f), 880f, 660f, 0.3f, 1, decay = 22f)
        return b
    }

    private fun winClip(): FloatArray {
        val b = FloatArray(sec(1.5f))
        val notes = floatArrayOf(523f, 659f, 784f, 1047f)
        for (i in notes.indices) {
            tone(b, sec(0.12f * i), sec(0.3f), notes[i], notes[i], 0.3f, 1, decay = 6f)
            tone(b, sec(0.12f * i), sec(0.3f), notes[i] * 2f, notes[i] * 2f, 0.1f, 2, decay = 8f)
        }
        // final happy chord
        val chord = floatArrayOf(523f, 659f, 784f, 1047f, 1319f)
        for (f in chord) tone(b, sec(0.6f), sec(0.85f), f, f, 0.15f, 1, decay = 2.6f)
        return b
    }

    private fun loseClip(): FloatArray {
        // Soft and silly rather than scary.
        val b = FloatArray(sec(0.8f))
        val notes = floatArrayOf(523f, 466f, 392f, 330f)
        for (i in notes.indices) {
            tone(b, sec(0.11f * i), sec(0.3f), notes[i], notes[i] * 0.98f, 0.26f, 1, decay = 6f)
        }
        tone(b, sec(0.45f), sec(0.32f), 300f, 180f, 0.2f, 2, decay = 5f)
        return b
    }

    private fun unlockClip(): FloatArray {
        val b = FloatArray(sec(1.0f))
        val notes = floatArrayOf(784f, 1046f, 1318f, 1568f, 2093f)
        for (i in notes.indices) {
            tone(b, sec(0.07f * i), sec(0.35f), notes[i], notes[i], 0.25f, 1, decay = 5.5f)
        }
        return b
    }

    /** Cheerful 8-bar chiptune loop: C - G - Am - F. */
    private fun musicClip(): FloatArray {
        val beat = 0.5f                       // 120 bpm
        val bars = 8
        val total = sec(beat * 4 * bars)
        val b = FloatArray(total)

        fun hz(semi: Int): Float = (440.0 * Math.pow(2.0, (semi - 9) / 12.0)).toFloat()

        // root semitone (C4 = 0 here, we offset to real pitch below)
        val chordRoots = intArrayOf(0, 0, 7, 7, 9, 9, 5, 5)
        // four quarter notes per bar, semitones above C5
        val melody = arrayOf(
            intArrayOf(12, 16, 19, 16), intArrayOf(21, 19, 16, -1),
            intArrayOf(19, 23, 26, 23), intArrayOf(24, 23, 19, -1),
            intArrayOf(21, 24, 28, 24), intArrayOf(26, 24, 21, -1),
            intArrayOf(17, 21, 24, 21), intArrayOf(23, 21, 17, -1)
        )

        for (bar in 0 until bars) {
            val barStart = sec(beat * 4 * bar)
            val root = chordRoots[bar]
            // bass on beats 1 and 3
            for (k in 0..1) {
                val f = hz(root - 24 + 12)
                tone(b, barStart + sec(beat * 2 * k), sec(beat * 1.6f), f, f, 0.20f, 0, decay = 2.2f)
            }
            // soft chord pad on beat 1
            for (iv in intArrayOf(0, 4, 7)) {
                val f = hz(root + iv)
                tone(b, barStart, sec(beat * 3.6f), f, f, 0.055f, 2, decay = 0.9f)
            }
            // melody
            val line = melody[bar]
            for (q in line.indices) {
                val n = line[q]
                if (n < 0) continue
                val f = hz(n)
                tone(b, barStart + sec(beat * q), sec(beat * 0.92f), f, f, 0.15f, 1, decay = 3.2f)
            }
            // light shaker
            for (e in 0 until 8) {
                tone(b, barStart + sec(beat * 0.5f * e), sec(0.045f), 6000f, 6000f, 0.035f, 3, decay = 45f)
            }
        }
        return b
    }

    // --------------------------------------------------------------- wav file

    private fun writeWav(f: File, data: FloatArray) {
        val n = data.size
        val bytes = ByteArray(44 + n * 2)
        var p = 0
        fun str(s: String) { for (c in s) bytes[p++] = c.code.toByte() }
        fun i32(v: Int) {
            bytes[p++] = (v and 0xff).toByte()
            bytes[p++] = ((v shr 8) and 0xff).toByte()
            bytes[p++] = ((v shr 16) and 0xff).toByte()
            bytes[p++] = ((v shr 24) and 0xff).toByte()
        }
        fun i16(v: Int) {
            bytes[p++] = (v and 0xff).toByte()
            bytes[p++] = ((v shr 8) and 0xff).toByte()
        }

        str("RIFF"); i32(36 + n * 2); str("WAVE")
        str("fmt "); i32(16); i16(1); i16(1); i32(RATE); i32(RATE * 2); i16(2); i16(16)
        str("data"); i32(n * 2)
        for (i in 0 until n) {
            var v = data[i]
            if (v > 1f) v = 1f
            if (v < -1f) v = -1f
            i16((v * 32000f).toInt())
        }
        FileOutputStream(f).use { it.write(bytes) }
    }
}
