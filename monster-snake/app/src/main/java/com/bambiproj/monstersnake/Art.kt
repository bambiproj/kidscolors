package com.bambiproj.monstersnake

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Col {
    val WHITE = 0xFFFFFFFF.toInt()
    val INK = 0xFF2B3A46.toInt()
    val INK_SOFT = 0xFF56687A.toInt()
    val PINK = 0xFFFF8FA6.toInt()
    val GOLD = 0xFFFFC93C.toInt()
    val GOLD_DK = 0xFFE09B10.toInt()
    val STAR = 0xFFFFD84D.toInt()
    val PANEL = 0xFFFFFDF5.toInt()
    val PANEL_EDGE = 0xFF3A4C5C.toInt()
    val GREEN = 0xFF5FD36B.toInt()
    val GREEN_DK = 0xFF33A34A.toInt()
    val RED = 0xFFFF6B6B.toInt()
    val BLUE = 0xFF4FA9FF.toInt()
    val PURPLE = 0xFFB07BFF.toInt()
    val ORANGE = 0xFFFF9F3C.toInt()
    val CYAN = 0xFF63E2F0.toInt()
    val SHADOW = 0x33000000
}

/** Snake looks the player can unlock with coins. Cosmetic only. */
class Skin(
    val id: Int,
    val label: String,
    val price: Int,
    val body: Int,
    val bodyDark: Int,
    val belly: Int,
    val crest: Int,     // 0 antennae, 1 flame, 2 fin, 3 bolt, 4 leaf, 5 ice, 6 star, 7 rainbow
    val accent: Int
)

object Skins {
    val all = arrayOf(
        Skin(0, "Gobbo", 0, 0xFF5FD36B.toInt(), 0xFF33A34A.toInt(), 0xFFC8F5A9.toInt(), 0, 0xFFFFD54A.toInt()),
        Skin(1, "Flambit", 60, 0xFFFF8A3D.toInt(), 0xFFE0521C.toInt(), 0xFFFFD9A8.toInt(), 1, 0xFFFFE066.toInt()),
        Skin(2, "Splashy", 120, 0xFF4FC3F7.toInt(), 0xFF1E88C7.toInt(), 0xFFD3F1FF.toInt(), 2, 0xFF9CE7FF.toInt()),
        Skin(3, "Zappo", 200, 0xFFFFD54A.toInt(), 0xFFDDA400.toInt(), 0xFFFFF3C4.toInt(), 3, 0xFFFFF7D6.toInt()),
        Skin(4, "Sprig", 300, 0xFF7CD858.toInt(), 0xFF3F9E32.toInt(), 0xFFE2FFCC.toInt(), 4, 0xFFB7F27A.toInt()),
        Skin(5, "Frosty", 420, 0xFF9AE7FF.toInt(), 0xFF3FB4D8.toInt(), 0xFFF0FCFF.toInt(), 5, 0xFFFFFFFF.toInt()),
        Skin(6, "Cosmo", 600, 0xFFB07BFF.toInt(), 0xFF7340C9.toInt(), 0xFFE9DBFF.toInt(), 6, 0xFFFFE066.toInt()),
        Skin(7, "Prism", 900, 0xFFFF7BB0.toInt(), 0xFFD1418A.toInt(), 0xFFFFE1F0.toInt(), 7, 0xFFFFFFFF.toInt())
    )

    fun get(id: Int): Skin = all[id.coerceIn(0, all.size - 1)]
}

/** The five original pocket-monsters the snake chases. */
class Critter(val id: Int, val label: String, val body: Int, val dark: Int, val accent: Int, val crest: Int)

object Critters {
    val all = arrayOf(
        Critter(0, "Emberlin", 0xFFFF8A3D.toInt(), 0xFFE0521C.toInt(), 0xFFFFD166.toInt(), 1),
        Critter(1, "Bloop", 0xFF4FC3F7.toInt(), 0xFF1E88C7.toInt(), 0xFFD3F1FF.toInt(), 2),
        Critter(2, "Zibb", 0xFFFFD54A.toInt(), 0xFFDDA400.toInt(), 0xFFFFF3C4.toInt(), 3),
        Critter(3, "Mossy", 0xFF7CD858.toInt(), 0xFF3F9E32.toInt(), 0xFFCFF7B0.toInt(), 4),
        Critter(4, "Nibbit", 0xFF9AE7FF.toInt(), 0xFF3FB4D8.toInt(), 0xFFFFFFFF.toInt(), 5)
    )
}

object Art {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()
    private val path = Path()

    val fontBold: Typeface = try {
        Typeface.create("sans-serif-black", Typeface.BOLD)
    } catch (t: Throwable) {
        Typeface.DEFAULT_BOLD
    }

    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fontBold
        textAlign = Paint.Align.CENTER
    }
    private val tpStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fontBold
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun circle(c: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        c.drawCircle(x, y, r, p)
    }

    fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        rf.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(rf, p)
    }

    fun ring(c: Canvas, x: Float, y: Float, r: Float, w: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        c.drawCircle(x, y, r, p)
    }

    fun rrect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        rf.set(l, t, r, b)
        c.drawRoundRect(rf, rad, rad, p)
    }

    fun rrectStroke(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, w: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        rf.set(l, t, r, b)
        c.drawRoundRect(rf, rad, rad, p)
    }

    fun line(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, w: Float, color: Int, alpha: Int = 255) {
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND
        c.drawLine(x0, y0, x1, y1, p)
    }

    fun star(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, rot: Float = 0f, alpha: Int = 255) {
        path.reset()
        for (i in 0 until 10) {
            val a = -PI.toFloat() / 2f + rot + i * PI.toFloat() / 5f
            val rr = if (i % 2 == 0) r else r * 0.45f
            val x = cx + cos(a) * rr
            val y = cy + sin(a) * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.reset(); p.isAntiAlias = true
        p.color = color; p.alpha = alpha
        c.drawPath(path, p)
    }

    fun heart(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        circle(c, cx - r * 0.45f, cy - r * 0.25f, r * 0.55f, color)
        circle(c, cx + r * 0.45f, cy - r * 0.25f, r * 0.55f, color)
        path.reset()
        path.moveTo(cx - r, cy - r * 0.05f)
        path.lineTo(cx + r, cy - r * 0.05f)
        path.lineTo(cx, cy + r)
        path.close()
        p.reset(); p.isAntiAlias = true
        p.color = color
        c.drawPath(path, p)
    }

    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float,
        color: Int = Col.WHITE, outline: Int = Col.INK, outlineW: Float = 0f,
        align: Paint.Align = Paint.Align.CENTER, alpha: Int = 255
    ) {
        if (outlineW > 0f) {
            tpStroke.textSize = size
            tpStroke.textAlign = align
            tpStroke.strokeWidth = outlineW
            tpStroke.color = outline
            tpStroke.alpha = alpha
            c.drawText(s, x, y, tpStroke)
        }
        tp.textSize = size
        tp.textAlign = align
        tp.color = color
        tp.alpha = alpha
        c.drawText(s, x, y, tp)
    }

    fun textWidth(s: String, size: Float): Float {
        tp.textSize = size
        return tp.measureText(s)
    }

    /** Vertically centred text: y is the middle of the line. */
    fun textMid(
        c: Canvas, s: String, x: Float, yMid: Float, size: Float,
        color: Int = Col.WHITE, outline: Int = Col.INK, outlineW: Float = 0f,
        align: Paint.Align = Paint.Align.CENTER, alpha: Int = 255
    ) {
        tp.textSize = size
        val fm = tp.fontMetrics
        val y = yMid - (fm.ascent + fm.descent) / 2f
        text(c, s, x, y, size, color, outline, outlineW, align, alpha)
    }

    // --------------------------------------------------------------- faces

    fun eyes(c: Canvas, cx: Float, cy: Float, r: Float, lookX: Float, lookY: Float, blink: Float = 0f, spread: Float = 0.40f) {
        val ex = r * spread
        val eR = r * 0.30f
        for (s in -1..1 step 2) {
            val x = cx + s * ex
            if (blink > 0.5f) {
                line(c, x - eR * 0.8f, cy, x + eR * 0.8f, cy, r * 0.10f, Col.INK)
            } else {
                circle(c, x, cy, eR, Col.WHITE)
                circle(c, x + lookX * eR * 0.35f, cy + lookY * eR * 0.35f, eR * 0.52f, Col.INK)
                circle(c, x + lookX * eR * 0.35f - eR * 0.18f, cy + lookY * eR * 0.35f - eR * 0.2f, eR * 0.18f, Col.WHITE)
            }
        }
    }

    fun smile(c: Canvas, cx: Float, cy: Float, r: Float, open: Float = 1f) {
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.12f
        p.strokeCap = Paint.Cap.ROUND
        p.color = Col.INK
        rf.set(cx - r * 0.34f, cy - r * 0.28f, cx + r * 0.34f, cy + r * 0.34f * open)
        c.drawArc(rf, 20f, 140f, false, p)
    }

    fun openMouth(c: Canvas, cx: Float, cy: Float, r: Float, amount: Float) {
        val h = r * (0.18f + 0.22f * amount)
        oval(c, cx, cy + r * 0.05f, r * 0.30f, h, Col.INK)
        oval(c, cx, cy + r * 0.05f + h * 0.38f, r * 0.17f, h * 0.45f, Col.PINK)
    }

    fun blush(c: Canvas, cx: Float, cy: Float, r: Float) {
        oval(c, cx - r * 0.62f, cy + r * 0.28f, r * 0.20f, r * 0.12f, Col.PINK, 200)
        oval(c, cx + r * 0.62f, cy + r * 0.28f, r * 0.20f, r * 0.12f, Col.PINK, 200)
    }

    /** Head decoration shared by snake skins and critters. */
    fun crest(c: Canvas, cx: Float, cy: Float, r: Float, kind: Int, accent: Int, dark: Int, t: Float) {
        val wob = sin(t * 4f) * r * 0.06f
        when (kind) {
            0 -> { // antennae
                for (s in -1..1 step 2) {
                    line(c, cx + s * r * 0.30f, cy - r * 0.72f, cx + s * r * 0.44f, cy - r * 1.18f + wob, r * 0.11f, dark)
                    circle(c, cx + s * r * 0.44f, cy - r * 1.20f + wob, r * 0.17f, accent)
                }
            }
            1 -> { // flame
                path.reset()
                path.moveTo(cx, cy - r * 1.42f + wob)
                path.cubicTo(cx + r * 0.52f, cy - r * 0.92f, cx + r * 0.34f, cy - r * 0.62f, cx, cy - r * 0.66f)
                path.cubicTo(cx - r * 0.34f, cy - r * 0.62f, cx - r * 0.52f, cy - r * 0.92f, cx, cy - r * 1.42f + wob)
                path.close()
                p.reset(); p.isAntiAlias = true; p.color = 0xFFFF5A2E.toInt()
                c.drawPath(path, p)
                path.reset()
                path.moveTo(cx, cy - r * 1.14f + wob)
                path.cubicTo(cx + r * 0.28f, cy - r * 0.86f, cx + r * 0.18f, cy - r * 0.70f, cx, cy - r * 0.72f)
                path.cubicTo(cx - r * 0.18f, cy - r * 0.70f, cx - r * 0.28f, cy - r * 0.86f, cx, cy - r * 1.14f + wob)
                path.close()
                p.color = accent
                c.drawPath(path, p)
            }
            2 -> { // water fin
                path.reset()
                path.moveTo(cx - r * 0.42f, cy - r * 0.70f)
                path.lineTo(cx - r * 0.06f, cy - r * 1.32f + wob)
                path.lineTo(cx + r * 0.10f, cy - r * 0.86f)
                path.lineTo(cx + r * 0.46f, cy - r * 1.14f + wob)
                path.lineTo(cx + r * 0.44f, cy - r * 0.64f)
                path.close()
                p.reset(); p.isAntiAlias = true; p.color = accent
                c.drawPath(path, p)
            }
            3 -> { // lightning ears
                for (s in -1..1 step 2) {
                    path.reset()
                    val bx = cx + s * r * 0.40f
                    path.moveTo(bx, cy - r * 0.66f)
                    path.lineTo(bx + s * r * 0.30f, cy - r * 1.06f + wob)
                    path.lineTo(bx + s * r * 0.08f, cy - r * 1.02f)
                    path.lineTo(bx + s * r * 0.34f, cy - r * 1.42f + wob)
                    path.lineTo(bx - s * r * 0.04f, cy - r * 0.94f)
                    path.lineTo(bx + s * r * 0.16f, cy - r * 0.92f)
                    path.close()
                    p.reset(); p.isAntiAlias = true; p.color = 0xFFFFE04D.toInt()
                    c.drawPath(path, p)
                }
            }
            4 -> { // leaf
                line(c, cx, cy - r * 0.70f, cx, cy - r * 1.00f, r * 0.10f, dark)
                p.reset(); p.isAntiAlias = true; p.color = accent
                c.save()
                c.rotate(-24f + sin(t * 3f) * 6f, cx, cy - r * 1.0f)
                rf.set(cx - r * 0.12f, cy - r * 1.46f, cx + r * 0.58f, cy - r * 0.92f)
                c.drawOval(rf, p)
                c.restore()
            }
            5 -> { // ice crown
                for (i in -1..1) {
                    val x = cx + i * r * 0.36f
                    val h = if (i == 0) r * 1.46f else r * 1.20f
                    path.reset()
                    path.moveTo(x - r * 0.15f, cy - r * 0.68f)
                    path.lineTo(x, cy - h + wob)
                    path.lineTo(x + r * 0.15f, cy - r * 0.68f)
                    path.close()
                    p.reset(); p.isAntiAlias = true; p.color = accent
                    c.drawPath(path, p)
                }
            }
            6 -> { // star crown
                star(c, cx, cy - r * 1.10f + wob, r * 0.34f, accent)
                star(c, cx - r * 0.52f, cy - r * 0.86f, r * 0.19f, accent, 0.4f)
                star(c, cx + r * 0.52f, cy - r * 0.86f, r * 0.19f, accent, 0.8f)
            }
            7 -> { // rainbow arcs
                val cols = intArrayOf(0xFFFF5D5D.toInt(), 0xFFFFC93C.toInt(), 0xFF5FD36B.toInt(), 0xFF4FA9FF.toInt())
                p.reset(); p.isAntiAlias = true
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                for (i in cols.indices) {
                    p.color = cols[i]
                    p.strokeWidth = r * 0.15f
                    val rr = r * (0.80f + i * 0.17f)
                    rf.set(cx - rr, cy - r * 0.55f - rr, cx + rr, cy - r * 0.55f + rr)
                    c.drawArc(rf, 200f, 140f, false, p)
                }
            }
        }
    }

    // ------------------------------------------------------------- critters

    /** One of the five collectible monsters, drawn with a little idle bounce. */
    fun critter(c: Canvas, cx: Float, cy: Float, r: Float, type: Int, t: Float, alpha: Int = 255) {
        val m = Critters.all[type % Critters.all.size]
        val bob = sin(t * 5f + type) * r * 0.10f
        val y = cy + bob
        val squash = 1f + sin(t * 5f + type) * 0.06f

        circle(c, cx, cy + r * 0.92f, r * 0.62f, Col.SHADOW, (alpha * 0.35f).toInt())
        // feet
        oval(c, cx - r * 0.44f, y + r * 0.74f, r * 0.24f, r * 0.17f, m.dark, alpha)
        oval(c, cx + r * 0.44f, y + r * 0.74f, r * 0.24f, r * 0.17f, m.dark, alpha)
        // body
        oval(c, cx, y, r * 0.82f / squash, r * 0.82f * squash, m.body, alpha)
        oval(c, cx, y + r * 0.22f, r * 0.46f, r * 0.40f, m.accent, (alpha * 0.55f).toInt())
        crest(c, cx, y - r * 0.10f, r * 0.72f, m.crest, m.accent, m.dark, t)
        eyes(c, cx, y - r * 0.10f, r * 0.80f, 0f, 0f, 0f, 0.36f)
        smile(c, cx, y + r * 0.26f, r * 0.72f)
        blush(c, cx, y + r * 0.04f, r * 0.70f)
    }

    fun coin(c: Canvas, cx: Float, cy: Float, r: Float, t: Float, alpha: Int = 255) {
        val spin = abs(cos(t * 3f))
        val rx = r * (0.26f + 0.62f * spin)
        circle(c, cx, cy + r * 0.85f, r * 0.45f, Col.SHADOW, (alpha * 0.3f).toInt())
        oval(c, cx, cy, rx, r * 0.80f, Col.GOLD_DK, alpha)
        oval(c, cx, cy, rx * 0.78f, r * 0.62f, Col.GOLD, alpha)
        if (spin > 0.45f) {
            textMid(c, "$", cx, cy, r * 0.86f, Col.GOLD_DK, Col.GOLD_DK, 0f, Paint.Align.CENTER, alpha)
        }
        circle(c, cx - rx * 0.35f, cy - r * 0.34f, r * 0.13f, Col.WHITE, (alpha * 0.8f).toInt())
    }

    fun starItem(c: Canvas, cx: Float, cy: Float, r: Float, t: Float, alpha: Int = 255) {
        val bob = sin(t * 4f) * r * 0.10f
        val rot = sin(t * 2f) * 0.22f
        circle(c, cx, cy + r * 0.88f, r * 0.45f, Col.SHADOW, (alpha * 0.3f).toInt())
        star(c, cx, cy + bob, r * 0.92f, Col.GOLD_DK, rot, alpha)
        star(c, cx, cy + bob, r * 0.78f, Col.STAR, rot, alpha)
        eyes(c, cx, cy + bob + r * 0.02f, r * 0.62f, 0f, 0f, 0f, 0.34f)
        smile(c, cx, cy + bob + r * 0.26f, r * 0.50f)
    }

    fun chest(c: Canvas, cx: Float, cy: Float, r: Float, t: Float, alpha: Int = 255) {
        val bob = sin(t * 3f) * r * 0.06f
        val y = cy + bob
        circle(c, cx, cy + r * 0.9f, r * 0.55f, Col.SHADOW, (alpha * 0.3f).toInt())
        rrect(c, cx - r * 0.78f, y - r * 0.18f, cx + r * 0.78f, y + r * 0.66f, r * 0.16f, 0xFFA9652F.toInt(), alpha)
        rrect(c, cx - r * 0.82f, y - r * 0.66f, cx + r * 0.82f, y - r * 0.06f, r * 0.22f, 0xFFC97C3A.toInt(), alpha)
        rrect(c, cx - r * 0.82f, y - r * 0.22f, cx + r * 0.82f, y - r * 0.04f, r * 0.06f, Col.GOLD, alpha)
        rrect(c, cx - r * 0.16f, y - r * 0.28f, cx + r * 0.16f, y + r * 0.24f, r * 0.08f, Col.GOLD, alpha)
        circle(c, cx, y + r * 0.02f, r * 0.11f, 0xFF7A4318.toInt(), alpha)
        star(c, cx + r * 0.55f, y - r * 0.80f, r * 0.2f, Col.WHITE, t, (alpha * 0.9f).toInt())
    }

    private val powerColors = intArrayOf(
        0xFF4FA9FF.toInt(), 0xFFFFC93C.toInt(), 0xFFFF6B6B.toInt(), 0xFF63E2F0.toInt(), 0xFFB07BFF.toInt()
    )

    fun powerColor(kind: Int): Int = powerColors[kind % powerColors.size]

    fun powerLabel(kind: Int): String = when (kind) {
        Power.SHIELD -> "SHIELD"
        Power.SPEED -> "SPEED"
        Power.MAGNET -> "MAGNET"
        Power.SLOW -> "SLOW"
        else -> "BONUS"
    }

    /** Big, obvious power-up badge. */
    fun power(c: Canvas, cx: Float, cy: Float, r: Float, kind: Int, t: Float, alpha: Int = 255) {
        val pulse = 1f + sin(t * 6f) * 0.07f
        val rr = r * 0.86f * pulse
        val col = powerColor(kind)
        circle(c, cx, cy, rr * 1.18f, Col.WHITE, (alpha * 0.30f).toInt())
        rrect(c, cx - rr, cy - rr, cx + rr, cy + rr, rr * 0.42f, Col.WHITE, alpha)
        rrect(c, cx - rr * 0.86f, cy - rr * 0.86f, cx + rr * 0.86f, cy + rr * 0.86f, rr * 0.34f, col, alpha)
        powerGlyph(c, cx, cy, rr * 0.62f, kind, t, alpha)
    }

    fun powerGlyph(c: Canvas, cx: Float, cy: Float, r: Float, kind: Int, t: Float, alpha: Int = 255) {
        when (kind) {
            Power.SHIELD -> {
                path.reset()
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r * 0.82f, cy - r * 0.55f)
                path.lineTo(cx + r * 0.66f, cy + r * 0.45f)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - r * 0.66f, cy + r * 0.45f)
                path.lineTo(cx - r * 0.82f, cy - r * 0.55f)
                path.close()
                p.reset(); p.isAntiAlias = true; p.color = Col.WHITE; p.alpha = alpha
                c.drawPath(path, p)
                line(c, cx - r * 0.34f, cy, cx - r * 0.06f, cy + r * 0.32f, r * 0.20f, 0xFF2E7CD6.toInt(), alpha)
                line(c, cx - r * 0.06f, cy + r * 0.32f, cx + r * 0.40f, cy - r * 0.30f, r * 0.20f, 0xFF2E7CD6.toInt(), alpha)
            }
            Power.SPEED -> {
                path.reset()
                path.moveTo(cx + r * 0.28f, cy - r)
                path.lineTo(cx - r * 0.52f, cy + r * 0.14f)
                path.lineTo(cx - r * 0.02f, cy + r * 0.14f)
                path.lineTo(cx - r * 0.22f, cy + r)
                path.lineTo(cx + r * 0.58f, cy - r * 0.18f)
                path.lineTo(cx + r * 0.06f, cy - r * 0.18f)
                path.close()
                p.reset(); p.isAntiAlias = true; p.color = Col.WHITE; p.alpha = alpha
                c.drawPath(path, p)
            }
            Power.MAGNET -> {
                p.reset(); p.isAntiAlias = true
                p.style = Paint.Style.STROKE
                p.strokeWidth = r * 0.44f
                p.color = Col.WHITE; p.alpha = alpha
                rf.set(cx - r * 0.66f, cy - r * 0.76f, cx + r * 0.66f, cy + r * 0.56f)
                c.drawArc(rf, 180f, 180f, false, p)
                p.style = Paint.Style.FILL
                c.drawRect(cx - r * 0.88f, cy + r * 0.10f, cx - r * 0.44f, cy + r * 0.78f, p)
                c.drawRect(cx + r * 0.44f, cy + r * 0.10f, cx + r * 0.88f, cy + r * 0.78f, p)
                p.color = 0xFFD63B3B.toInt(); p.alpha = alpha
                c.drawRect(cx - r * 0.88f, cy + r * 0.46f, cx - r * 0.44f, cy + r * 0.80f, p)
                c.drawRect(cx + r * 0.44f, cy + r * 0.46f, cx + r * 0.88f, cy + r * 0.80f, p)
            }
            Power.SLOW -> {
                p.reset(); p.isAntiAlias = true
                p.color = Col.WHITE; p.alpha = alpha
                p.style = Paint.Style.STROKE
                p.strokeWidth = r * 0.22f
                p.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 3) {
                    val a = i * PI.toFloat() / 3f
                    c.drawLine(cx - cos(a) * r, cy - sin(a) * r, cx + cos(a) * r, cy + sin(a) * r, p)
                }
                p.strokeWidth = r * 0.14f
                for (i in 0 until 6) {
                    val a = i * PI.toFloat() / 3f
                    val bx = cx + cos(a) * r * 0.60f
                    val by = cy + sin(a) * r * 0.60f
                    c.drawLine(bx, by, bx + cos(a + 0.9f) * r * 0.32f, by + sin(a + 0.9f) * r * 0.32f, p)
                    c.drawLine(bx, by, bx + cos(a - 0.9f) * r * 0.32f, by + sin(a - 0.9f) * r * 0.32f, p)
                }
            }
            else -> {
                star(c, cx, cy, r * 1.02f, Col.WHITE, sin(t * 2f) * 0.2f, alpha)
            }
        }
    }

    // ------------------------------------------------------------ obstacles

    fun block(c: Canvas, cx: Float, cy: Float, size: Float, theme: Int, t: Float) {
        val h = size * 0.46f
        when (theme) {
            Theme.CANDY -> {
                rrect(c, cx - h, cy - h, cx + h, cy + h, h * 0.55f, 0xFFF06AA8.toInt())
                rrect(c, cx - h * 0.72f, cy - h * 0.72f, cx + h * 0.72f, cy + h * 0.72f, h * 0.45f, 0xFFFF9DC8.toInt())
                circle(c, cx, cy, h * 0.30f, Col.WHITE, 200)
            }
            Theme.OCEAN -> {
                rrect(c, cx - h, cy - h, cx + h, cy + h, h * 0.62f, 0xFF2E7F9E.toInt())
                circle(c, cx - h * 0.28f, cy - h * 0.28f, h * 0.36f, 0xFF63C6E0.toInt())
                circle(c, cx + h * 0.34f, cy + h * 0.24f, h * 0.26f, 0xFF63C6E0.toInt())
            }
            Theme.SPACE -> {
                rrect(c, cx - h, cy - h, cx + h, cy + h, h * 0.36f, 0xFF5B4B87.toInt())
                rrect(c, cx - h * 0.70f, cy - h * 0.70f, cx + h * 0.70f, cy + h * 0.70f, h * 0.28f, 0xFF8C77C4.toInt())
                star(c, cx, cy, h * 0.34f, 0xFFE3D6FF.toInt(), t * 0.5f)
            }
            else -> {
                rrect(c, cx - h, cy - h, cx + h, cy + h, h * 0.42f, 0xFF7A6A58.toInt())
                rrect(c, cx - h * 0.74f, cy - h * 0.74f, cx + h * 0.74f, cy + h * 0.60f, h * 0.34f, 0xFF9C8B75.toInt())
                circle(c, cx - h * 0.30f, cy - h * 0.26f, h * 0.20f, 0xFFB6A791.toInt())
            }
        }
    }

    /** Roaming grumpy blob. Friendly-looking, but it bites. */
    fun mover(c: Canvas, cx: Float, cy: Float, r: Float, dirX: Float, dirY: Float, t: Float) {
        val wob = sin(t * 7f) * r * 0.08f
        circle(c, cx, cy + r * 0.85f, r * 0.55f, Col.SHADOW, 70)
        oval(c, cx, cy, r * 0.84f + wob, r * 0.84f - wob, 0xFFE0567A.toInt())
        oval(c, cx, cy + r * 0.22f, r * 0.44f, r * 0.34f, 0xFFFF8FAB.toInt(), 150)
        // spikes
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f + t
            val rr = r * 0.84f
            path.reset()
            path.moveTo(cx + cos(a - 0.16f) * rr, cy + sin(a - 0.16f) * rr)
            path.lineTo(cx + cos(a) * rr * 1.24f, cy + sin(a) * rr * 1.24f)
            path.lineTo(cx + cos(a + 0.16f) * rr, cy + sin(a + 0.16f) * rr)
            path.close()
            p.reset(); p.isAntiAlias = true; p.color = 0xFFC33E63.toInt()
            c.drawPath(path, p)
        }
        eyes(c, cx, cy - r * 0.10f, r * 0.72f, dirX, dirY, 0f, 0.36f)
        // frown
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.11f
        p.strokeCap = Paint.Cap.ROUND
        p.color = Col.INK
        rf.set(cx - r * 0.26f, cy + r * 0.26f, cx + r * 0.26f, cy + r * 0.60f)
        c.drawArc(rf, 200f, 140f, false, p)
    }

    fun portal(c: Canvas, cx: Float, cy: Float, r: Float, t: Float, which: Int) {
        val a = t * 2.4f + which * 1.6f
        val col = if (which == 0) 0xFFFF9F3C.toInt() else 0xFFB07BFF.toInt()
        val col2 = if (which == 0) 0xFFFFE066.toInt() else 0xFFE0C6FF.toInt()
        circle(c, cx, cy, r * 0.90f, col, 90)
        ring(c, cx, cy, r * 0.74f, r * 0.20f, col)
        ring(c, cx, cy, r * 0.48f, r * 0.14f, col2)
        for (i in 0 until 3) {
            val ang = a + i * 2.09f
            circle(c, cx + cos(ang) * r * 0.62f, cy + sin(ang) * r * 0.62f, r * 0.12f, Col.WHITE, 220)
        }
        circle(c, cx, cy, r * 0.22f, Col.WHITE, 230)
    }

    // ----------------------------------------------------------- the snake

    fun snakeBody(c: Canvas, cx: Float, cy: Float, r: Float, idx: Int, skin: Skin, t: Float, shield: Boolean) {
        val band = idx % 2 == 0
        val base = if (band) skin.body else skin.bodyDark
        if (shield) circle(c, cx, cy, r * 1.16f, Col.BLUE, 90)
        circle(c, cx, cy, r, base)
        circle(c, cx, cy, r * 0.62f, skin.belly, 110)
        if (skin.crest == 7) {
            val hue = ((idx * 26 + (t * 90f).toInt()) % 360)
            circle(c, cx, cy, r * 0.78f, hsv(hue.toFloat(), 0.62f, 1f), 190)
        }
    }

    fun snakeHead(c: Canvas, cx: Float, cy: Float, r: Float, dirX: Float, dirY: Float, skin: Skin, t: Float, shield: Boolean, mouth: Float) {
        if (shield) {
            circle(c, cx, cy, r * 1.36f, Col.BLUE, 70)
            ring(c, cx, cy, r * 1.30f, r * 0.13f, 0xFF8FD0FF.toInt(), 220)
        }
        circle(c, cx, cy + r * 0.10f, r * 1.02f, skin.bodyDark)
        circle(c, cx, cy, r * 1.02f, skin.body)
        oval(c, cx, cy + r * 0.34f, r * 0.60f, r * 0.42f, skin.belly, 150)
        crest(c, cx, cy, r * 0.94f, skin.crest, skin.accent, skin.bodyDark, t)
        eyes(c, cx + dirX * r * 0.10f, cy - r * 0.16f + dirY * r * 0.08f, r * 1.02f, dirX, dirY, 0f, 0.40f)
        if (mouth > 0.02f) openMouth(c, cx + dirX * r * 0.12f, cy + r * 0.34f, r * 0.96f, mouth)
        else smile(c, cx, cy + r * 0.30f, r * 0.86f)
        blush(c, cx, cy + r * 0.10f, r * 0.92f)
    }

    fun hsv(h: Float, s: Float, v: Float): Int {
        val i = ((h / 60f).toInt()) % 6
        val f = h / 60f - (h / 60f).toInt()
        val pv = v * (1 - s)
        val q = v * (1 - f * s)
        val tt = v * (1 - (1 - f) * s)
        val r: Float; val g: Float; val b: Float
        when (i) {
            0 -> { r = v; g = tt; b = pv }
            1 -> { r = q; g = v; b = pv }
            2 -> { r = pv; g = v; b = tt }
            3 -> { r = pv; g = q; b = v }
            4 -> { r = tt; g = pv; b = v }
            else -> { r = v; g = pv; b = q }
        }
        return (0xFF shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    // --------------------------------------------------------- backgrounds

    private var bgShader: Shader? = null
    private var bgKey = -1
    private var bgW = 0f
    private var bgH = 0f

    private fun themeSky(theme: Int): Pair<Int, Int> = when (theme) {
        Theme.CANDY -> Pair(0xFFFFC7E8.toInt(), 0xFFFFE9C9.toInt())
        Theme.OCEAN -> Pair(0xFF1E5F8C.toInt(), 0xFF6FD3E0.toInt())
        Theme.SPACE -> Pair(0xFF1B1140.toInt(), 0xFF4B2A7A.toInt())
        else -> Pair(0xFF7FD6FF.toInt(), 0xFFD9F5C8.toInt())
    }

    fun background(c: Canvas, w: Float, h: Float, theme: Int, t: Float) {
        if (bgKey != theme || bgW != w || bgH != h) {
            val sky = themeSky(theme)
            bgShader = LinearGradient(0f, 0f, 0f, h, sky.first, sky.second, Shader.TileMode.CLAMP)
            bgKey = theme; bgW = w; bgH = h
        }
        p.reset(); p.isAntiAlias = true
        p.shader = bgShader
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        when (theme) {
            Theme.MEADOW -> {
                circle(c, w * 0.82f, h * 0.10f, w * 0.11f, 0xFFFFE066.toInt(), 220)
                cloud(c, w * 0.20f + sin(t * 0.22f) * w * 0.05f, h * 0.10f, w * 0.13f, 235)
                cloud(c, w * 0.66f + cos(t * 0.17f) * w * 0.04f, h * 0.19f, w * 0.10f, 200)
                hill(c, w * 0.18f, h * 1.02f, w * 0.52f, 0xFF77C55A.toInt())
                hill(c, w * 0.80f, h * 1.05f, w * 0.46f, 0xFF64B44B.toInt())
                for (i in 0 until 6) {
                    val fx = w * (0.07f + i * 0.17f)
                    flower(c, fx, h * (0.955f + 0.012f * (i % 3)), w * 0.020f, i)
                }
            }
            Theme.CANDY -> {
                cloud(c, w * 0.24f + sin(t * 0.2f) * w * 0.04f, h * 0.12f, w * 0.13f, 220)
                cloud(c, w * 0.76f + cos(t * 0.15f) * w * 0.03f, h * 0.20f, w * 0.10f, 190)
                for (i in 0 until 14) {
                    val fx = ((i * 137) % 100) / 100f * w
                    val fy = ((i * 61) % 100) / 100f * h
                    val rr = w * 0.012f
                    val col = intArrayOf(0xFFFF7BB0.toInt(), 0xFF7BD3FF.toInt(), 0xFFFFE066.toInt(), 0xFFA5F58C.toInt())[i % 4]
                    rrect(c, fx - rr, fy - rr * 2.6f, fx + rr, fy + rr * 2.6f, rr, col, 170)
                }
                hill(c, w * 0.22f, h * 1.04f, w * 0.50f, 0xFFF08CC0.toInt())
                hill(c, w * 0.82f, h * 1.06f, w * 0.44f, 0xFFE070AC.toInt())
            }
            Theme.OCEAN -> {
                for (i in 0 until 16) {
                    val bx = ((i * 97) % 100) / 100f * w
                    val speed = 0.06f + (i % 5) * 0.02f
                    val by = h - ((t * speed * h + i * h * 0.13f) % (h * 1.05f))
                    val rr = w * (0.010f + (i % 4) * 0.006f)
                    circle(c, bx, by, rr, Col.WHITE, 90)
                    circle(c, bx - rr * 0.3f, by - rr * 0.3f, rr * 0.3f, Col.WHITE, 150)
                }
                p.reset(); p.isAntiAlias = true
                p.color = 0x22FFFFFF
                for (i in 0 until 4) {
                    path.reset()
                    val x0 = w * (0.1f + i * 0.26f)
                    path.moveTo(x0, 0f)
                    path.lineTo(x0 + w * 0.10f, 0f)
                    path.lineTo(x0 + w * 0.30f, h)
                    path.lineTo(x0 + w * 0.06f, h)
                    path.close()
                    c.drawPath(path, p)
                }
                hill(c, w * 0.16f, h * 1.05f, w * 0.44f, 0xFF15506E.toInt())
                hill(c, w * 0.86f, h * 1.05f, w * 0.40f, 0xFF10455F.toInt())
            }
            else -> {
                for (i in 0 until 40) {
                    val sx = ((i * 73) % 100) / 100f * w
                    val sy = ((i * 151) % 100) / 100f * h
                    val tw = 0.5f + 0.5f * sin(t * 2.2f + i)
                    circle(c, sx, sy, w * (0.0035f + 0.003f * tw), Col.WHITE, (120 + 120 * tw).toInt())
                }
                circle(c, w * 0.80f, h * 0.13f, w * 0.10f, 0xFFFFB35C.toInt(), 235)
                p.reset(); p.isAntiAlias = true
                p.style = Paint.Style.STROKE
                p.strokeWidth = w * 0.014f
                p.color = 0xAAFFD9A0.toInt()
                rf.set(w * 0.80f - w * 0.17f, h * 0.13f - w * 0.06f, w * 0.80f + w * 0.17f, h * 0.13f + w * 0.06f)
                c.drawOval(rf, p)
                for (i in 0 until 5) star(c, w * (0.08f + i * 0.21f), h * (0.06f + 0.03f * (i % 3)), w * 0.013f, Col.WHITE, t + i, 200)
            }
        }
    }

    private fun cloud(c: Canvas, x: Float, y: Float, r: Float, alpha: Int) {
        circle(c, x, y, r * 0.62f, Col.WHITE, alpha)
        circle(c, x - r * 0.58f, y + r * 0.16f, r * 0.44f, Col.WHITE, alpha)
        circle(c, x + r * 0.58f, y + r * 0.14f, r * 0.48f, Col.WHITE, alpha)
        oval(c, x, y + r * 0.34f, r * 1.05f, r * 0.34f, Col.WHITE, alpha)
    }

    private fun hill(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        circle(c, x, y, r, color)
    }

    private fun flower(c: Canvas, x: Float, y: Float, r: Float, i: Int) {
        val col = intArrayOf(0xFFFF7BB0.toInt(), 0xFFFFE066.toInt(), 0xFFFFFFFF.toInt(), 0xFFB07BFF.toInt())[i % 4]
        for (k in 0 until 5) {
            val a = k * 2f * PI.toFloat() / 5f
            circle(c, x + cos(a) * r, y + sin(a) * r, r * 0.8f, col)
        }
        circle(c, x, y, r * 0.7f, Col.GOLD)
    }
}
