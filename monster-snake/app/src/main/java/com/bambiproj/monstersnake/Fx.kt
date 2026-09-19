package com.bambiproj.monstersnake

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var color: Int,
    var life: Float, var maxLife: Float,
    var kind: Int,           // 0 dot, 1 confetti rect, 2 star, 3 ring
    var rot: Float, var vrot: Float,
    var gravity: Float
)

private class Popup(
    var text: String, var x: Float, var y: Float,
    var color: Int, var size: Float,
    var life: Float, var maxLife: Float
)

/** Confetti, sparkles and the bouncy "GREAT!" words. */
object Fx {
    private val parts = ArrayList<Particle>()
    private val pops = ArrayList<Popup>()
    private val rnd = Random(1234)

    val cheers = arrayOf("GREAT!", "NICE!", "SUPER!", "YUM!", "WOW!", "YAY!", "COOL!")

    fun clear() {
        parts.clear()
        pops.clear()
    }

    fun burst(x: Float, y: Float, color: Int, count: Int, speed: Float, size: Float) {
        for (i in 0 until count) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val sp = speed * (0.4f + rnd.nextFloat() * 0.8f)
            val life = 0.45f + rnd.nextFloat() * 0.35f
            parts.add(
                Particle(
                    x, y, cos(a) * sp, sin(a) * sp,
                    size * (0.5f + rnd.nextFloat() * 0.8f), color,
                    life, life, 0, 0f, 0f, speed * 1.5f
                )
            )
        }
    }

    fun sparkle(x: Float, y: Float, color: Int, count: Int, spread: Float, size: Float) {
        for (i in 0 until count) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val d = rnd.nextFloat() * spread
            val life = 0.4f + rnd.nextFloat() * 0.3f
            parts.add(
                Particle(
                    x + cos(a) * d, y + sin(a) * d, 0f, -spread * 0.5f,
                    size * (0.5f + rnd.nextFloat() * 0.7f), color,
                    life, life, 2, rnd.nextFloat() * 3f, (rnd.nextFloat() - 0.5f) * 6f, 0f
                )
            )
        }
    }

    fun ring(x: Float, y: Float, color: Int, size: Float) {
        parts.add(Particle(x, y, 0f, 0f, size, color, 0.4f, 0.4f, 3, 0f, 0f, 0f))
    }

    /** Party! Falls from the top of the screen. */
    fun confetti(w: Float, h: Float, count: Int) {
        val cols = intArrayOf(
            0xFFFF5D5D.toInt(), 0xFFFFC93C.toInt(), 0xFF5FD36B.toInt(),
            0xFF4FA9FF.toInt(), 0xFFB07BFF.toInt(), 0xFFFF7BB0.toInt()
        )
        for (i in 0 until count) {
            val life = 1.8f + rnd.nextFloat() * 1.6f
            parts.add(
                Particle(
                    rnd.nextFloat() * w, -h * 0.05f - rnd.nextFloat() * h * 0.45f,
                    (rnd.nextFloat() - 0.5f) * w * 0.25f, h * (0.25f + rnd.nextFloat() * 0.35f),
                    w * (0.014f + rnd.nextFloat() * 0.016f), cols[i % cols.size],
                    life, life, 1, rnd.nextFloat() * 6f, (rnd.nextFloat() - 0.5f) * 10f,
                    h * 0.10f
                )
            )
        }
    }

    fun popup(text: String, x: Float, y: Float, color: Int, size: Float, life: Float = 1.0f) {
        pops.add(Popup(text, x, y, color, size, life, life))
    }

    fun cheer(x: Float, y: Float, size: Float) {
        popup(cheers[rnd.nextInt(cheers.size)], x, y, Col.GOLD, size)
    }

    fun update(dt: Float) {
        var i = 0
        while (i < parts.size) {
            val q = parts[i]
            q.life -= dt
            if (q.life <= 0f) {
                parts.removeAt(i)
                continue
            }
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.vy += q.gravity * dt
            q.rot += q.vrot * dt
            i++
        }
        var j = 0
        while (j < pops.size) {
            val q = pops[j]
            q.life -= dt
            if (q.life <= 0f) {
                pops.removeAt(j)
                continue
            }
            q.y -= dt * q.size * 1.1f
            j++
        }
    }

    fun draw(c: Canvas) {
        for (q in parts) {
            val t = q.life / q.maxLife
            val alpha = (255 * (if (t > 0.6f) 1f else t / 0.6f)).toInt().coerceIn(0, 255)
            when (q.kind) {
                1 -> {
                    c.save()
                    c.rotate(q.rot * 57.3f, q.x, q.y)
                    Art.rrect(c, q.x - q.size, q.y - q.size * 0.55f, q.x + q.size, q.y + q.size * 0.55f, q.size * 0.3f, q.color, alpha)
                    c.restore()
                }
                2 -> Art.star(c, q.x, q.y, q.size * (0.6f + 0.6f * t), q.color, q.rot, alpha)
                3 -> {
                    val rr = q.size * (1.6f - t)
                    Art.ring(c, q.x, q.y, rr, q.size * 0.25f * t, q.color, alpha)
                }
                else -> Art.circle(c, q.x, q.y, q.size * (0.4f + 0.7f * t), q.color, alpha)
            }
        }
        for (q in pops) {
            val t = 1f - q.life / q.maxLife          // 0 -> 1
            val pop = if (t < 0.25f) bounce(t / 0.25f) else 1f
            val alpha = if (t > 0.7f) ((1f - (t - 0.7f) / 0.3f) * 255).toInt().coerceIn(0, 255) else 255
            val size = q.size * pop
            Art.textMid(c, q.text, q.x, q.y, size, q.color, Col.INK, size * 0.18f, Paint.Align.CENTER, alpha)
        }
    }

    /** Overshoot easing so pop-ups feel springy. */
    fun bounce(t: Float): Float {
        val s = 1.9f
        val x = t - 1f
        return 1f + x * x * ((s + 1f) * x + s)
    }

    fun busy(): Boolean = parts.size > 0
}
