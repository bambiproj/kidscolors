package com.bambiproj.monstersnake

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Icon {
    const val NONE = -1
    const val PLAY = 0
    const val GEAR = 1
    const val GRID = 2
    const val SKIN = 3
    const val BACK = 4
    const val PAUSE = 5
    const val HOME = 6
    const val REPLAY = 7
    const val NEXT = 8
    const val MUSIC = 9
    const val SOUND = 10
    const val LOCK = 11
    const val CHECK = 12
    const val CROSS = 13
    const val UP = 14
    const val DOWN = 15
    const val LEFT = 16
    const val RIGHT = 17
    const val COIN = 18
    const val STAR = 19
    const val PAD = 20
    const val INFINITY = 21

    fun draw(c: Canvas, kind: Int, cx: Float, cy: Float, r: Float, color: Int, t: Float = 0f) {
        when (kind) {
            PLAY -> tri(c, cx + r * 0.10f, cy, r, color, 0f)
            PAUSE -> {
                Art.rrect(c, cx - r * 0.62f, cy - r * 0.72f, cx - r * 0.14f, cy + r * 0.72f, r * 0.18f, color)
                Art.rrect(c, cx + r * 0.14f, cy - r * 0.72f, cx + r * 0.62f, cy + r * 0.72f, r * 0.18f, color)
            }
            GEAR -> {
                for (i in 0 until 8) {
                    val a = i * PI.toFloat() / 4f
                    Art.rrect(
                        c, cx + cos(a) * r * 0.78f - r * 0.20f, cy + sin(a) * r * 0.78f - r * 0.20f,
                        cx + cos(a) * r * 0.78f + r * 0.20f, cy + sin(a) * r * 0.78f + r * 0.20f,
                        r * 0.08f, color
                    )
                }
                Art.circle(c, cx, cy, r * 0.62f, color)
                Art.circle(c, cx, cy, r * 0.26f, Col.PANEL)
            }
            GRID -> {
                for (gy in 0 until 2) for (gx in 0 until 2) {
                    val ox = cx + (gx - 0.5f) * r * 0.90f
                    val oy = cy + (gy - 0.5f) * r * 0.90f
                    Art.rrect(c, ox - r * 0.33f, oy - r * 0.33f, ox + r * 0.33f, oy + r * 0.33f, r * 0.12f, color)
                }
            }
            SKIN -> {
                Art.circle(c, cx, cy - r * 0.05f, r * 0.72f, color)
                Art.circle(c, cx - r * 0.26f, cy - r * 0.16f, r * 0.17f, Col.PANEL)
                Art.circle(c, cx + r * 0.26f, cy - r * 0.16f, r * 0.17f, Col.PANEL)
                Art.circle(c, cx - r * 0.26f, cy - r * 0.13f, r * 0.08f, color)
                Art.circle(c, cx + r * 0.26f, cy - r * 0.13f, r * 0.08f, color)
            }
            BACK -> arrow(c, cx, cy, r, color, 180f)
            UP -> arrow(c, cx, cy, r, color, -90f)
            DOWN -> arrow(c, cx, cy, r, color, 90f)
            LEFT -> arrow(c, cx, cy, r, color, 180f)
            RIGHT -> arrow(c, cx, cy, r, color, 0f)
            NEXT -> {
                tri(c, cx - r * 0.18f, cy, r * 0.86f, color, 0f)
                Art.rrect(c, cx + r * 0.46f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f, r * 0.12f, color)
            }
            HOME -> {
                c.save()
                Art.rrect(c, cx - r * 0.52f, cy - r * 0.10f, cx + r * 0.52f, cy + r * 0.72f, r * 0.14f, color)
                c.restore()
                triUp(c, cx, cy - r * 0.68f, r * 0.82f, color)
            }
            REPLAY -> {
                Art.ring(c, cx, cy, r * 0.62f, r * 0.24f, color)
                Art.rrect(c, cx + r * 0.10f, cy - r * 0.95f, cx + r * 0.95f, cy - r * 0.45f, r * 0.1f, Col.PANEL)
                tri(c, cx + r * 0.42f, cy - r * 0.62f, r * 0.5f, color, -90f)
            }
            MUSIC -> {
                Art.circle(c, cx - r * 0.34f, cy + r * 0.44f, r * 0.30f, color)
                Art.circle(c, cx + r * 0.42f, cy + r * 0.24f, r * 0.30f, color)
                Art.rrect(c, cx - r * 0.12f, cy - r * 0.78f, cx + r * 0.06f, cy + r * 0.46f, r * 0.07f, color)
                Art.rrect(c, cx + r * 0.54f, cy - r * 0.94f, cx + r * 0.72f, cy + r * 0.26f, r * 0.07f, color)
                Art.rrect(c, cx - r * 0.12f, cy - r * 0.94f, cx + r * 0.72f, cy - r * 0.62f, r * 0.10f, color)
            }
            SOUND -> {
                Art.rrect(c, cx - r * 0.76f, cy - r * 0.26f, cx - r * 0.30f, cy + r * 0.26f, r * 0.08f, color)
                triRightBig(c, cx - r * 0.30f, cy, r * 0.70f, color)
                for (i in 1..2) Art.ring(c, cx + r * 0.20f, cy, r * (0.20f + i * 0.28f), r * 0.11f, color)
            }
            LOCK -> {
                Art.ring(c, cx, cy - r * 0.34f, r * 0.42f, r * 0.20f, color)
                Art.rrect(c, cx - r * 0.62f, cy - r * 0.16f, cx + r * 0.62f, cy + r * 0.72f, r * 0.18f, color)
                Art.circle(c, cx, cy + r * 0.26f, r * 0.14f, Col.PANEL_EDGE)
            }
            CHECK -> {
                Art.line(c, cx - r * 0.6f, cy + r * 0.04f, cx - r * 0.14f, cy + r * 0.52f, r * 0.30f, color)
                Art.line(c, cx - r * 0.14f, cy + r * 0.52f, cx + r * 0.64f, cy - r * 0.52f, r * 0.30f, color)
            }
            CROSS -> {
                Art.line(c, cx - r * 0.52f, cy - r * 0.52f, cx + r * 0.52f, cy + r * 0.52f, r * 0.28f, color)
                Art.line(c, cx + r * 0.52f, cy - r * 0.52f, cx - r * 0.52f, cy + r * 0.52f, r * 0.28f, color)
            }
            COIN -> Art.coin(c, cx, cy, r, 0.4f)
            STAR -> Art.star(c, cx, cy, r * 0.95f, color)
            PAD -> {
                Art.rrect(c, cx - r * 0.30f, cy - r * 0.92f, cx + r * 0.30f, cy + r * 0.92f, r * 0.16f, color)
                Art.rrect(c, cx - r * 0.92f, cy - r * 0.30f, cx + r * 0.92f, cy + r * 0.30f, r * 0.16f, color)
            }
            INFINITY -> {
                Art.ring(c, cx - r * 0.44f, cy, r * 0.42f, r * 0.22f, color)
                Art.ring(c, cx + r * 0.44f, cy, r * 0.42f, r * 0.22f, color)
            }
        }
    }

    private fun tri(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, deg: Float) {
        c.save()
        c.rotate(deg, cx, cy)
        val p = android.graphics.Path()
        p.moveTo(cx - r * 0.56f, cy - r * 0.78f)
        p.lineTo(cx + r * 0.78f, cy)
        p.lineTo(cx - r * 0.56f, cy + r * 0.78f)
        p.close()
        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
        pt.color = color
        c.drawPath(p, pt)
        c.restore()
    }

    private fun triRightBig(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = android.graphics.Path()
        p.moveTo(cx, cy - r * 0.5f)
        p.lineTo(cx, cy + r * 0.5f)
        p.lineTo(cx - r * 0.8f, cy + r)
        p.lineTo(cx - r * 0.8f, cy - r)
        p.close()
        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
        pt.color = color
        c.drawPath(p, pt)
    }

    private fun triUp(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = android.graphics.Path()
        p.moveTo(cx, cy - r * 0.42f)
        p.lineTo(cx + r * 0.86f, cy + r * 0.42f)
        p.lineTo(cx - r * 0.86f, cy + r * 0.42f)
        p.close()
        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
        pt.color = color
        c.drawPath(p, pt)
    }

    private fun arrow(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, deg: Float) {
        c.save()
        c.rotate(deg, cx, cy)
        Art.line(c, cx - r * 0.55f, cy, cx + r * 0.45f, cy, r * 0.30f, color)
        Art.line(c, cx - r * 0.55f, cy, cx + r * 0.02f, cy - r * 0.56f, r * 0.30f, color)
        Art.line(c, cx - r * 0.55f, cy, cx + r * 0.02f, cy + r * 0.56f, r * 0.30f, color)
        c.restore()
    }
}

/** Big, chunky, kid-proof button with a squishy press animation. */
class Btn(
    var label: String = "",
    var icon: Int = Icon.NONE,
    var color: Int = Col.GREEN,
    var textSize: Float = 0f
) {
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f
    var enabled = true
    var visible = true
    var pressAmt = 0f
    private var down = false
    var tag = 0

    fun set(x: Float, y: Float, w: Float, h: Float): Btn {
        this.x = x; this.y = y; this.w = w; this.h = h
        return this
    }

    fun contains(px: Float, py: Float): Boolean {
        val pad = h * 0.12f
        return visible && enabled && px >= x - pad && px <= x + w + pad && py >= y - pad && py <= y + h + pad
    }

    fun onDown(px: Float, py: Float): Boolean {
        if (!contains(px, py)) return false
        down = true
        return true
    }

    /** Returns true when the finger lifts inside the button. */
    fun onUp(px: Float, py: Float): Boolean {
        val was = down
        down = false
        return was && contains(px, py)
    }

    fun cancel() {
        down = false
    }

    fun update(dt: Float) {
        val target = if (down) 1f else 0f
        pressAmt += (target - pressAmt) * (1f - Math.pow(0.0001, dt.toDouble()).toFloat())
        if (pressAmt < 0.002f) pressAmt = 0f
    }

    fun draw(c: Canvas, t: Float) {
        if (!visible) return
        val squish = 1f - pressAmt * 0.06f
        val cx = x + w / 2f
        val cy = y + h / 2f
        val hw = w / 2f * squish
        val hh = h / 2f * squish
        val rad = hh * 0.52f
        val lift = h * 0.10f * (1f - pressAmt)
        val col = if (enabled) color else 0xFFB9C4CC.toInt()

        // drop shadow / 3d base
        Art.rrect(c, cx - hw, cy - hh + lift * 0.4f, cx + hw, cy + hh + lift, rad, darken(col, 0.62f))
        Art.rrect(c, cx - hw, cy - hh, cx + hw, cy + hh, rad, col)
        // glossy top
        Art.rrect(c, cx - hw * 0.90f, cy - hh * 0.82f, cx + hw * 0.90f, cy - hh * 0.10f, rad * 0.7f, Col.WHITE, 46)

        val ts = if (textSize > 0f) textSize * squish else h * 0.40f * squish
        if (icon != Icon.NONE && label.isEmpty()) {
            Icon.draw(c, icon, cx, cy, hh * 0.58f, Col.WHITE, t)
        } else if (icon != Icon.NONE) {
            val iconR = hh * 0.46f
            val tw = Art.textWidth(label, ts)
            val total = iconR * 2.4f + tw
            Icon.draw(c, icon, cx - total / 2f + iconR, cy, iconR, Col.WHITE, t)
            Art.textMid(c, label, cx - total / 2f + iconR * 2.4f + tw / 2f, cy, ts, Col.WHITE, darken(col, 0.5f), ts * 0.14f)
        } else {
            Art.textMid(c, label, cx, cy, ts, Col.WHITE, darken(col, 0.5f), ts * 0.14f)
        }
    }

    companion object {
        fun darken(c: Int, f: Float): Int {
            val a = (c ushr 24) and 0xFF
            val r = (((c shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
            val g = (((c shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
            val b = ((c and 0xFF) * f).toInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun lighten(c: Int, f: Float): Int {
            val a = (c ushr 24) and 0xFF
            val r = (((c shr 16) and 0xFF) + (255 - ((c shr 16) and 0xFF)) * f).toInt().coerceIn(0, 255)
            val g = (((c shr 8) and 0xFF) + (255 - ((c shr 8) and 0xFF)) * f).toInt().coerceIn(0, 255)
            val b = ((c and 0xFF) + (255 - (c and 0xFF)) * f).toInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

object Ui {
    /** Soft card used by every dialog. */
    fun panel(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int = Col.PANEL) {
        Art.rrect(c, l + rad * 0.12f, t + rad * 0.22f, r + rad * 0.12f, b + rad * 0.30f, rad, 0x44000000)
        Art.rrect(c, l, t, r, b, rad, Btn.darken(color, 0.80f))
        Art.rrect(c, l, t, r, b - rad * 0.22f, rad, color)
    }

    fun dim(c: Canvas, w: Float, h: Float, alpha: Int) {
        Art.rrect(c, 0f, 0f, w, h, 0f, 0xFF10202C.toInt(), alpha)
    }

    /** Header ribbon with a title, used on every screen. */
    fun ribbon(c: Canvas, cx: Float, cy: Float, w: Float, h: Float, title: String, color: Int = Col.ORANGE) {
        Art.rrect(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f + h * 0.14f, h * 0.42f, Btn.darken(color, 0.7f))
        Art.rrect(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h * 0.42f, color)
        Art.textMid(c, title, cx, cy, h * 0.52f, Col.WHITE, Btn.darken(color, 0.45f), h * 0.075f)
    }

    /** Little rounded pill showing an icon and a number (coins, stars...). */
    fun chip(c: Canvas, cx: Float, cy: Float, w: Float, h: Float, icon: Int, value: String, color: Int) {
        Art.rrect(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h * 0.5f, 0x55000000)
        Art.rrect(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f - h * 0.08f, h * 0.5f, color)
        val r = h * 0.34f
        Icon.draw(c, icon, cx - w / 2f + h * 0.46f, cy, r, Col.WHITE)
        Art.textMid(c, value, cx + h * 0.16f, cy, h * 0.46f, Col.WHITE, Btn.darken(color, 0.45f), h * 0.07f)
    }

    fun starRow(c: Canvas, cx: Float, cy: Float, r: Float, earned: Int, total: Int = 3, t: Float = 0f, pop: Float = 1f) {
        val gap = r * 2.3f
        val start = cx - gap * (total - 1) / 2f
        for (i in 0 until total) {
            val x = start + i * gap
            if (i < earned) {
                val s = if (pop < 1f) Fx.bounce(((pop * total) - i).coerceIn(0f, 1f)) else 1f
                Art.star(c, x, cy, r * 1.18f * s, Col.GOLD_DK, sin(t * 2f + i) * 0.08f)
                Art.star(c, x, cy, r * s, Col.STAR, sin(t * 2f + i) * 0.08f)
            } else {
                Art.star(c, x, cy, r, 0x55000000, 0f)
            }
        }
    }
}
