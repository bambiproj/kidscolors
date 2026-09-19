package com.bambiproj.monstersnake

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin

private fun themeForProgress(): Int {
    val lv = Prefs.unlocked.coerceIn(1, Levels.COUNT)
    return Levels.get(lv).theme
}

/** Coin + star counters drawn at the top of the menus. */
private fun drawWallet(c: Canvas) {
    val h = 82f * G.s
    val y = G.top + h * 0.75f
    Ui.chip(c, G.w * 0.26f, y, G.w * 0.40f, h, Icon.COIN, Prefs.coins.toString(), Col.GOLD_DK)
    Ui.chip(c, G.w * 0.74f, y, G.w * 0.40f, h, Icon.STAR, "${Prefs.totalStars()}/${Levels.COUNT * 3}", Col.PURPLE)
}

// ---------------------------------------------------------------- main menu

class MenuScreen : Screen() {

    private val play = Btn("PLAY", Icon.PLAY, Col.GREEN)
    private val levels = Btn("", Icon.GRID, Col.BLUE)
    private val skins = Btn("", Icon.SKIN, Col.PURPLE)
    private val settings = Btn("", Icon.GEAR, Col.ORANGE)
    private val all = arrayOf(play, levels, skins, settings)
    private var theme = 0

    override fun onShow() {
        theme = themeForProgress()
        if (Prefs.musicOn) Sfx.startMusic()
    }

    override fun layout() {
        val bw = G.w * 0.66f
        val bh = 168f * G.s
        play.set((G.w - bw) / 2f, G.h * 0.635f, bw, bh)
        play.textSize = bh * 0.44f

        val sw = G.w * 0.22f
        val sh = sw
        val gap = G.w * 0.05f
        val total = sw * 3 + gap * 2
        val sx = (G.w - total) / 2f
        val sy = G.h * 0.635f + bh + G.h * 0.035f
        levels.set(sx, sy, sw, sh)
        skins.set(sx + sw + gap, sy, sw, sh)
        settings.set(sx + (sw + gap) * 2, sy, sw, sh)
    }

    override fun update(dt: Float) {
        for (b in all) b.update(dt)
    }

    override fun draw(c: Canvas) {
        Art.background(c, G.w, G.h, theme, G.time)
        drawWallet(c)

        val t = G.time
        val cx = G.w / 2f
        val titleY = G.top + G.h * 0.115f
        val wob = sin(t * 1.6f) * 3f
        c.save()
        c.rotate(wob, cx, titleY)
        Art.textMid(c, "MONSTER", cx, titleY - G.h * 0.032f, G.w * 0.155f, Col.STAR, Col.INK, G.w * 0.022f)
        Art.textMid(c, "SNAKE", cx, titleY + G.h * 0.038f, G.w * 0.175f, Col.GREEN, Col.INK, G.w * 0.024f)
        c.restore()

        drawMascot(c, cx, G.h * 0.40f, G.w * 0.115f, t)

        for (b in all) b.draw(c, t)

        Art.textMid(c, "LEVELS", levels.x + levels.w / 2f, levels.y + levels.h + 34f * G.s, 40f * G.s, Col.WHITE, Col.INK, 8f * G.s)
        Art.textMid(c, "MONSTERS", skins.x + skins.w / 2f, skins.y + skins.h + 34f * G.s, 40f * G.s, Col.WHITE, Col.INK, 8f * G.s)
        Art.textMid(c, "SETTINGS", settings.x + settings.w / 2f, settings.y + settings.h + 34f * G.s, 40f * G.s, Col.WHITE, Col.INK, 8f * G.s)
    }

    /** A friendly wiggling snake to greet the player. */
    private fun drawMascot(c: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val skin = Skins.get(Prefs.skin)
        val n = 9
        for (i in n - 1 downTo 1) {
            val f = i / n.toFloat()
            val x = cx - r * 1.35f * i + sin(t * 2f + i * 0.7f) * r * 0.20f
            val y = cy + sin(t * 2.2f + i * 0.55f) * r * 0.42f + f * r * 0.25f
            Art.snakeBody(c, x, y, r * (0.92f - f * 0.30f), i, skin, t, false)
        }
        val hx = cx + sin(t * 2f) * r * 0.18f
        val hy = cy + sin(t * 2.2f) * r * 0.30f
        Art.snakeHead(c, hx, hy, r, 1f, 0f, skin, t, false, 0f)
        // a critter bouncing just in front of the snake
        val ci = ((t / 3f).toInt()) % Critters.all.size
        Art.critter(c, hx + r * 2.5f, hy + sin(t * 3f) * r * 0.2f, r * 0.85f, ci, t)
    }

    override fun down(x: Float, y: Float) {
        for (b in all) if (b.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
    }

    override fun up(x: Float, y: Float) {
        if (play.onUp(x, y)) {
            Nav.go(PlayScreen(Prefs.unlocked.coerceIn(1, Levels.COUNT + 1)))
            return
        }
        if (levels.onUp(x, y)) { Nav.go(LevelScreen()); return }
        if (skins.onUp(x, y)) { Nav.go(SkinScreen()); return }
        if (settings.onUp(x, y)) { Nav.go(SettingsScreen()); return }
        for (b in all) b.cancel()
    }
}

// ------------------------------------------------------------ level select

class LevelScreen : Screen() {

    private val back = Btn("", Icon.BACK, Col.RED)
    private val endless = Btn("ENDLESS", Icon.INFINITY, Col.PURPLE)
    private val cells = ArrayList<Btn>()
    private var theme = 0
    private var bumpIdx = -1
    private var bump = 0f

    override fun onShow() {
        theme = themeForProgress()
    }

    override fun layout() {
        cells.clear()
        val cols = 4
        val rows = 5
        val pad = G.w * 0.06f
        val gap = G.w * 0.035f
        val size = (G.w - pad * 2 - gap * (cols - 1)) / cols
        val top = G.top + G.h * 0.175f
        for (i in 0 until Levels.COUNT) {
            val r = i / cols
            val cc = i % cols
            val b = Btn("${i + 1}", Icon.NONE, Col.GREEN)
            b.tag = i + 1
            b.textSize = size * 0.42f
            b.set(pad + cc * (size + gap), top + r * (size + gap), size, size)
            cells.add(b)
        }
        val ew = G.w * 0.62f
        val eh = 140f * G.s
        endless.textSize = eh * 0.38f
        endless.set((G.w - ew) / 2f, top + rows * (size + gap) + G.h * 0.02f, ew, eh)

        val bs = 108f * G.s
        back.set(G.w * 0.05f, G.top + 96f * G.s, bs, bs)
    }

    override fun update(dt: Float) {
        back.update(dt)
        endless.update(dt)
        for (b in cells) b.update(dt)
        if (bump > 0f) bump -= dt * 2.6f
    }

    override fun draw(c: Canvas) {
        Art.background(c, G.w, G.h, theme, G.time)
        Ui.ribbon(c, G.w / 2f, G.top + 96f * G.s, G.w * 0.56f, 96f * G.s, "LEVELS", Col.BLUE)
        back.draw(c, G.time)

        val coinsY = G.top + 96f * G.s
        Ui.chip(c, G.w * 0.86f, coinsY, G.w * 0.24f, 72f * G.s, Icon.COIN, Prefs.coins.toString(), Col.GOLD_DK)

        for (b in cells) {
            val lv = b.tag
            val open = lv <= Prefs.unlocked
            val stars = Prefs.stars(lv)
            val lvl = Levels.get(lv)
            b.color = if (open) themeColor(lvl.theme) else 0xFF8FA3B0.toInt()
            b.enabled = open
            val shake = if (bumpIdx == lv && bump > 0f) sin(bump * 40f) * 8f * G.s else 0f
            c.save()
            c.translate(shake, 0f)
            b.draw(c, G.time)
            if (!open) {
                Icon.draw(c, Icon.LOCK, b.x + b.w / 2f, b.y + b.h * 0.46f, b.h * 0.26f, Col.WHITE)
            } else {
                Ui.starRow(c, b.x + b.w / 2f, b.y + b.h * 0.86f, b.h * 0.115f, stars, 3, G.time)
            }
            c.restore()
        }

        endless.enabled = Prefs.unlocked > Levels.COUNT
        endless.draw(c, G.time)
        if (!endless.enabled) {
            Art.textMid(
                c, "Finish level 20 to unlock", G.w / 2f,
                endless.y + endless.h + 46f * G.s, 38f * G.s, Col.WHITE, Col.INK, 7f * G.s
            )
        } else {
            Art.textMid(
                c, "Best: ${Prefs.endlessBest}", G.w / 2f,
                endless.y + endless.h + 46f * G.s, 40f * G.s, Col.WHITE, Col.INK, 7f * G.s
            )
        }
    }

    private fun themeColor(theme: Int): Int = when (theme) {
        Theme.CANDY -> 0xFFFF7BB0.toInt()
        Theme.OCEAN -> 0xFF35A7C9.toInt()
        Theme.SPACE -> Col.PURPLE
        else -> Col.GREEN
    }

    override fun down(x: Float, y: Float) {
        if (back.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
        if (endless.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
        for (b in cells) {
            if (b.enabled && b.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
            if (!b.enabled && x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h) {
                bumpIdx = b.tag
                bump = 1f
                Sfx.play(Sfx.CLICK, 0.7f)
            }
        }
    }

    override fun up(x: Float, y: Float) {
        if (back.onUp(x, y)) { Nav.go(MenuScreen()); return }
        if (endless.onUp(x, y)) { Nav.go(PlayScreen(Levels.COUNT + 1)); return }
        for (b in cells) if (b.onUp(x, y)) { Nav.go(PlayScreen(b.tag)); return }
        for (b in cells) b.cancel()
    }

    override fun back(): Boolean {
        Nav.go(MenuScreen())
        return true
    }
}

// ------------------------------------------------------------- skins / shop

class SkinScreen : Screen() {

    private val back = Btn("", Icon.BACK, Col.RED)
    private val cards = ArrayList<Btn>()
    private var theme = 0
    private var msg = ""
    private var msgT = 0f

    override fun onShow() {
        theme = themeForProgress()
    }

    override fun layout() {
        cards.clear()
        val cols = 2
        val pad = G.w * 0.07f
        val gap = G.w * 0.05f
        val cw = (G.w - pad * 2 - gap) / cols
        val ch = cw * 1.12f
        val top = G.top + G.h * 0.165f
        for (i in Skins.all.indices) {
            val r = i / cols
            val cc = i % cols
            val b = Btn("", Icon.NONE, Col.PANEL)
            b.tag = i
            b.set(pad + cc * (cw + gap), top + r * (ch + gap * 0.7f), cw, ch)
            cards.add(b)
        }
        val bs = 108f * G.s
        back.set(G.w * 0.05f, G.top + 96f * G.s, bs, bs)
    }

    override fun update(dt: Float) {
        back.update(dt)
        for (b in cards) b.update(dt)
        if (msgT > 0f) msgT -= dt
    }

    override fun draw(c: Canvas) {
        Art.background(c, G.w, G.h, theme, G.time)
        Ui.ribbon(c, G.w / 2f, G.top + 96f * G.s, G.w * 0.60f, 96f * G.s, "MONSTERS", Col.PURPLE)
        back.draw(c, G.time)
        Ui.chip(c, G.w * 0.86f, G.top + 96f * G.s, G.w * 0.24f, 72f * G.s, Icon.COIN, Prefs.coins.toString(), Col.GOLD_DK)

        for (b in cards) {
            val sk = Skins.all[b.tag]
            val owned = Prefs.ownsSkin(sk.id)
            val chosen = Prefs.skin == sk.id
            val squish = 1f - b.pressAmt * 0.05f
            val cx = b.x + b.w / 2f
            val cy = b.y + b.h / 2f
            val hw = b.w / 2f * squish
            val hh = b.h / 2f * squish

            Ui.panel(c, cx - hw, cy - hh, cx + hw, cy + hh, hw * 0.24f, if (chosen) 0xFFFFF3C4.toInt() else Col.PANEL)
            if (chosen) Art.rrectStroke(c, cx - hw, cy - hh, cx + hw, cy + hh, hw * 0.24f, 8f * G.s, Col.GOLD_DK)

            val r = b.w * 0.20f
            if (owned) {
                Art.snakeBody(c, cx - r * 1.5f, cy + r * 0.55f, r * 0.62f, 1, sk, G.time, false)
                Art.snakeBody(c, cx - r * 0.75f, cy + r * 0.40f, r * 0.72f, 0, sk, G.time, false)
                Art.snakeHead(c, cx + r * 0.35f, cy - r * 0.15f, r, 1f, 0f, sk, G.time, false, 0f)
            } else {
                Art.circle(c, cx, cy - r * 0.15f, r * 1.05f, 0xFF9FB0BC.toInt())
                Icon.draw(c, Icon.LOCK, cx, cy - r * 0.15f, r * 0.62f, Col.WHITE)
            }

            Art.textMid(c, sk.label, cx, cy + b.h * 0.26f, b.w * 0.145f, Col.INK, Col.INK, 0f)
            if (owned) {
                val label = if (chosen) "PLAYING" else "TAP TO USE"
                Art.textMid(c, label, cx, cy + b.h * 0.385f, b.w * 0.105f, if (chosen) Col.GOLD_DK else Col.INK_SOFT, Col.INK, 0f)
            } else {
                Ui.chip(c, cx, cy + b.h * 0.385f, b.w * 0.58f, b.h * 0.15f, Icon.COIN, sk.price.toString(), Col.GOLD_DK)
            }
        }

        // the creature album
        val albumY = cards[cards.size - 1].y + cards[cards.size - 1].h + G.h * 0.035f
        Art.textMid(c, "MONSTERS YOU MET", G.w / 2f, albumY, 40f * G.s, Col.WHITE, Col.INK, 7f * G.s)
        val r = G.w * 0.062f
        val gap = G.w * 0.155f
        val startX = G.w / 2f - gap * (Critters.all.size - 1) / 2f
        for (i in Critters.all.indices) {
            val x = startX + i * gap
            val yy = albumY + r * 1.5f
            if (Prefs.metCreature(i)) {
                Art.critter(c, x, yy, r, i, G.time)
            } else {
                Art.circle(c, x, yy, r * 0.82f, 0x66000000)
                Art.textMid(c, "?", x, yy, r * 1.1f, Col.WHITE, Col.INK, 0f)
            }
        }

        if (msgT > 0f) {
            val a = (msgT.coerceAtMost(1f) * 255).toInt()
            Art.textMid(c, msg, G.w / 2f, G.h * 0.93f, 48f * G.s, Col.WHITE, Col.RED, 10f * G.s, Paint.Align.CENTER, a)
        }
    }

    override fun down(x: Float, y: Float) {
        if (back.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
        for (b in cards) if (b.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
    }

    override fun up(x: Float, y: Float) {
        if (back.onUp(x, y)) { Nav.go(MenuScreen()); return }
        for (b in cards) {
            if (!b.onUp(x, y)) continue
            val sk = Skins.all[b.tag]
            if (Prefs.ownsSkin(sk.id)) {
                Prefs.skin = sk.id
                Sfx.play(Sfx.STAR)
                Fx.sparkle(b.x + b.w / 2f, b.y + b.h / 2f, Col.GOLD, 14, b.w * 0.4f, 12f * G.s)
            } else if (Prefs.coins >= sk.price) {
                Prefs.coins -= sk.price
                Prefs.unlockSkin(sk.id)
                Prefs.skin = sk.id
                Sfx.play(Sfx.UNLOCK)
                Fx.confetti(G.w, G.h, 60)
                Fx.popup("NEW SKIN!", G.w / 2f, G.h * 0.5f, Col.STAR, 92f * G.s, 1.4f)
            } else {
                msg = "Need ${sk.price - Prefs.coins} more coins"
                msgT = 2f
                Sfx.play(Sfx.CLICK, 0.6f)
            }
            return
        }
        for (b in cards) b.cancel()
    }

    override fun back(): Boolean {
        Nav.go(MenuScreen())
        return true
    }
}

// ----------------------------------------------------------------- settings

class SettingsScreen : Screen() {

    private val back = Btn("", Icon.BACK, Col.RED)
    private val music = Btn("MUSIC", Icon.MUSIC, Col.GREEN)
    private val sound = Btn("SOUNDS", Icon.SOUND, Col.GREEN)
    private val pad = Btn("BIG ARROWS", Icon.PAD, Col.GREEN)
    private val rows = arrayOf(music, sound, pad)
    private var theme = 0

    override fun onShow() {
        theme = themeForProgress()
    }

    override fun layout() {
        val bw = G.w * 0.78f
        val bh = 150f * G.s
        val gap = 42f * G.s
        val top = G.top + G.h * 0.24f
        for (i in rows.indices) {
            rows[i].set((G.w - bw) / 2f, top + i * (bh + gap), bw, bh)
            rows[i].textSize = bh * 0.34f
        }
        val bs = 108f * G.s
        back.set(G.w * 0.05f, G.top + 96f * G.s, bs, bs)
    }

    override fun update(dt: Float) {
        back.update(dt)
        for (b in rows) b.update(dt)
    }

    override fun draw(c: Canvas) {
        Art.background(c, G.w, G.h, theme, G.time)
        Ui.ribbon(c, G.w / 2f, G.top + 96f * G.s, G.w * 0.60f, 96f * G.s, "SETTINGS", Col.ORANGE)
        back.draw(c, G.time)

        music.color = if (Prefs.musicOn) Col.GREEN else 0xFF9FB0BC.toInt()
        sound.color = if (Prefs.soundOn) Col.GREEN else 0xFF9FB0BC.toInt()
        pad.color = if (Prefs.padOn) Col.GREEN else 0xFF9FB0BC.toInt()

        for (b in rows) {
            b.draw(c, G.time)
            val on = when (b) {
                music -> Prefs.musicOn
                sound -> Prefs.soundOn
                else -> Prefs.padOn
            }
            val kx = b.x + b.w - b.h * 0.60f
            val ky = b.y + b.h / 2f
            Art.circle(c, kx, ky, b.h * 0.33f, Col.WHITE)
            Icon.draw(c, if (on) Icon.CHECK else Icon.CROSS, kx, ky, b.h * 0.20f, if (on) Col.GREEN else Col.RED)
        }

        Art.textMid(
            c, "Big arrows show tap buttons", G.w / 2f,
            pad.y + pad.h + 60f * G.s, 36f * G.s, Col.WHITE, Col.INK, 7f * G.s
        )
        Art.textMid(
            c, "under the board. Swiping always works.", G.w / 2f,
            pad.y + pad.h + 104f * G.s, 36f * G.s, Col.WHITE, Col.INK, 7f * G.s
        )
        Art.textMid(
            c, "No ads  -  No internet  -  Made for kids", G.w / 2f,
            G.bottom - 48f * G.s, 34f * G.s, Col.WHITE, Col.INK, 6f * G.s
        )
    }

    override fun down(x: Float, y: Float) {
        if (back.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
        for (b in rows) if (b.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
    }

    override fun up(x: Float, y: Float) {
        if (back.onUp(x, y)) { Nav.go(MenuScreen()); return }
        if (music.onUp(x, y)) {
            Prefs.musicOn = !Prefs.musicOn
            Sfx.applyMusicSetting()
            return
        }
        if (sound.onUp(x, y)) {
            Prefs.soundOn = !Prefs.soundOn
            Sfx.play(Sfx.CLICK)
            return
        }
        if (pad.onUp(x, y)) {
            Prefs.padOn = !Prefs.padOn
            Sfx.play(Sfx.CLICK)
            return
        }
        for (b in rows) b.cancel()
    }

    override fun back(): Boolean {
        Nav.go(MenuScreen())
        return true
    }
}
