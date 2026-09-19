package com.bambiproj.monstersnake

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.sin

class PlayScreen(private val levelNum: Int) : Screen() {

    private val level = Levels.get(levelNum)
    private var eng = Engine(level)

    private var state = READY
    private var stateT = 0f

    // board geometry
    private var cell = 0f
    private var bx = 0f
    private var by = 0f
    private var bw = 0f
    private var bh = 0f
    private var padCy = 0f
    private var padSize = 0f

    // swipe tracking
    private var downX = 0f
    private var downY = 0f
    private var swiped = false

    // results
    private var gotStars = 0
    private var gotCoins = 0
    private var saved = false

    private val pause = Btn("", Icon.PAUSE, Col.ORANGE)
    private val up = Btn("", Icon.UP, Col.BLUE)
    private val down = Btn("", Icon.DOWN, Col.BLUE)
    private val left = Btn("", Icon.LEFT, Col.BLUE)
    private val right = Btn("", Icon.RIGHT, Col.BLUE)

    private val resume = Btn("PLAY", Icon.PLAY, Col.GREEN)
    private val replay = Btn("", Icon.REPLAY, Col.ORANGE)
    private val home = Btn("", Icon.HOME, Col.BLUE)
    private val next = Btn("NEXT", Icon.NEXT, Col.GREEN)
    private val again = Btn("TRY AGAIN", Icon.REPLAY, Col.GREEN)
    private val toLevels = Btn("LEVELS", Icon.GRID, Col.BLUE)

    companion object {
        const val READY = 0
        const val PLAY = 1
        const val PAUSED = 2
        const val WON = 3
        const val LOST = 4
    }

    // ------------------------------------------------------------- layout

    override fun layout() {
        val hudH = 280f * G.s
        val padH = if (Prefs.padOn) 430f * G.s else 120f * G.s
        val margin = 26f * G.s
        val top = G.top + hudH
        val bottom = G.bottom - padH
        val availW = G.w - margin * 2
        val availH = (bottom - top).coerceAtLeast(100f * G.s)
        cell = minOf(availW / level.cols, availH / level.rows)
        bw = cell * level.cols
        bh = cell * level.rows
        bx = (G.w - bw) / 2f
        by = top + (availH - bh) / 2f

        val ps = 108f * G.s
        pause.set(G.w - ps - G.w * 0.05f, G.top + 40f * G.s, ps, ps)

        padCy = bottom + padH * 0.48f
        padSize = minOf(168f * G.s, padH * 0.30f)
        val g = padSize * 1.12f
        val cx = G.w / 2f
        up.set(cx - padSize / 2f, padCy - g - padSize / 2f, padSize, padSize)
        down.set(cx - padSize / 2f, padCy + g - padSize / 2f, padSize, padSize)
        left.set(cx - g - padSize / 2f, padCy - padSize / 2f, padSize, padSize)
        right.set(cx + g - padSize / 2f, padCy - padSize / 2f, padSize, padSize)

        val bwd = G.w * 0.52f
        val bhd = 150f * G.s
        val panelCx = G.w / 2f
        resume.textSize = bhd * 0.40f
        next.textSize = bhd * 0.40f
        again.textSize = bhd * 0.36f
        toLevels.textSize = bhd * 0.36f
        resume.set(panelCx - bwd / 2f, G.h * 0.52f, bwd, bhd)
        val small = 132f * G.s
        replay.set(panelCx - small * 1.35f, G.h * 0.52f + bhd + 40f * G.s, small, small)
        home.set(panelCx + small * 0.35f, G.h * 0.52f + bhd + 40f * G.s, small, small)
        next.set(panelCx - bwd / 2f, G.h * 0.60f, bwd, bhd)
        again.set(panelCx - bwd / 2f, G.h * 0.58f, bwd, bhd)
        toLevels.set(panelCx - bwd / 2f, G.h * 0.58f + bhd + 34f * G.s, bwd, bhd * 0.86f)
    }

    override fun onShow() {
        Fx.clear()
        if (Prefs.musicOn) Sfx.startMusic()
    }

    fun forcePause() {
        if (state == PLAY || state == READY) {
            state = PAUSED
            stateT = 0f
        }
    }

    // ------------------------------------------------------------- update

    override fun update(dt: Float) {
        stateT += dt
        pause.update(dt)
        for (b in arrayOf(up, down, left, right)) b.update(dt)
        for (b in arrayOf(resume, replay, home, next, again, toLevels)) b.update(dt)

        when (state) {
            READY -> {
                eng.update(dt)
                if (eng.started) state = PLAY
            }
            PLAY -> {
                eng.update(dt)
            }
        }
        drainEvents()
    }

    private fun boardX(gx: Float) = bx + (gx + 0.5f) * cell
    private fun boardY(gy: Float) = by + (gy + 0.5f) * cell

    private fun drainEvents() {
        if (eng.events.isEmpty()) return
        for (e in eng.events) {
            val px = boardX(e.x.toFloat())
            val py = boardY(e.y.toFloat())
            when (e.type) {
                Evt.FOOD -> {
                    Sfx.play(Sfx.COLLECT, 0.95f + (eng.eaten % 5) * 0.05f)
                    Fx.burst(px, py, Critters.all[e.sub % Critters.all.size].body, 14, cell * 5f, cell * 0.12f)
                    Fx.ring(px, py, Col.WHITE, cell * 0.5f)
                    if (eng.eaten % 3 == 0) Fx.cheer(px, py - cell, 74f * G.s)
                }
                Evt.STAR -> {
                    Sfx.play(Sfx.STAR)
                    Fx.sparkle(px, py, Col.STAR, 18, cell * 0.8f, cell * 0.16f)
                    Fx.popup("+25", px, py - cell * 0.6f, Col.STAR, 62f * G.s, 0.9f)
                }
                Evt.COIN -> {
                    Sfx.play(Sfx.COIN)
                    Fx.burst(px, py, Col.GOLD, 10, cell * 4f, cell * 0.10f)
                }
                Evt.CHEST -> {
                    Sfx.play(Sfx.UNLOCK)
                    Fx.confetti(G.w, G.h * 0.4f, 26)
                    Fx.popup("TREASURE!", G.w / 2f, py - cell, Col.GOLD, 80f * G.s, 1.2f)
                }
                Evt.POWER -> {
                    Sfx.play(Sfx.POWER)
                    Fx.ring(px, py, Art.powerColor(e.sub), cell * 0.8f)
                    Fx.sparkle(px, py, Art.powerColor(e.sub), 16, cell, cell * 0.14f)
                    Fx.popup(Art.powerLabel(e.sub) + "!", G.w / 2f, G.h * 0.34f, Art.powerColor(e.sub), 86f * G.s, 1.2f)
                }
                Evt.NEW_CRITTER -> {
                    Fx.popup("NEW MONSTER!", G.w / 2f, G.h * 0.28f, Col.PURPLE, 78f * G.s, 1.6f)
                    Fx.confetti(G.w, G.h * 0.3f, 24)
                }
                Evt.SAVED -> {
                    Sfx.play(Sfx.POWER, 1.3f)
                    Fx.ring(px, py, Col.BLUE, cell * 1.2f)
                    Fx.popup("SAVED!", px, py - cell, Col.BLUE, 74f * G.s, 1.1f)
                }
                Evt.PORTAL -> {
                    Sfx.play(Sfx.COLLECT, 1.5f)
                    Fx.sparkle(px, py, Col.PURPLE, 12, cell * 0.8f, cell * 0.12f)
                }
                Evt.WIN -> finishWin()
                Evt.CRASH -> finishLose()
            }
        }
        eng.events.clear()
    }

    private fun finishWin() {
        if (state == WON) return
        state = WON
        stateT = 0f
        gotStars = eng.starsAwarded()
        gotCoins = eng.coinsEarned + 10 + gotStars * 8
        if (!saved) {
            saved = true
            Prefs.coins += gotCoins
            Prefs.setStars(levelNum, gotStars)
            Prefs.unlockLevel(levelNum + 1)
        }
        Sfx.play(Sfx.WIN)
        Fx.confetti(G.w, G.h, 130)
        Fx.popup("LEVEL COMPLETE!", G.w / 2f, G.h * 0.30f, Col.STAR, 84f * G.s, 1.8f)
    }

    private fun finishLose() {
        if (state == LOST) return
        state = LOST
        stateT = 0f
        gotCoins = eng.coinsEarned
        if (!saved) {
            saved = true
            Prefs.coins += gotCoins
            if (level.isEndless && eng.score > Prefs.endlessBest) Prefs.endlessBest = eng.score
        }
        Sfx.play(Sfx.LOSE)
        Fx.burst(boardX(eng.body[0].x.toFloat()), boardY(eng.body[0].y.toFloat()), Col.RED, 18, cell * 5f, cell * 0.14f)
    }

    // --------------------------------------------------------------- draw

    override fun draw(c: Canvas) {
        Art.background(c, G.w, G.h, level.theme, G.time)
        drawBoard(c)
        drawHud(c)
        if (Prefs.padOn) {
            up.draw(c, G.time); down.draw(c, G.time); left.draw(c, G.time); right.draw(c, G.time)
        }
        when (state) {
            READY -> drawReady(c)
            PAUSED -> drawPaused(c)
            WON -> drawWon(c)
            LOST -> drawLost(c)
        }
    }

    private fun drawBoard(c: Canvas) {
        val rad = cell * 0.5f
        Art.rrect(c, bx - cell * 0.18f, by - cell * 0.18f, bx + bw + cell * 0.18f, by + bh + cell * 0.18f, rad, 0x55000000)
        Art.rrect(c, bx - cell * 0.12f, by - cell * 0.12f, bx + bw + cell * 0.12f, by + bh + cell * 0.12f, rad, boardFrame())
        Art.rrect(c, bx, by, bx + bw, by + bh, rad * 0.7f, boardBase())
        for (y in 0 until level.rows) {
            for (x in 0 until level.cols) {
                if ((x + y) % 2 == 0) continue
                Art.rrect(c, bx + x * cell, by + y * cell, bx + (x + 1) * cell, by + (y + 1) * cell, 0f, Col.WHITE, 26)
            }
        }

        // static blocks
        for (cellIdx in eng.walls) {
            val x = cellIdx % level.cols
            val y = cellIdx / level.cols
            Art.block(c, boardX(x.toFloat()), boardY(y.toFloat()), cell, level.theme, G.time)
        }

        // portals
        for (i in eng.portals.indices) {
            val p = eng.portals[i]
            Art.portal(c, boardX(p.x.toFloat()), boardY(p.y.toFloat()), cell * 0.5f, G.time, i % 2)
        }

        // collectibles
        for (it in eng.items) {
            val px = boardX(it.x.toFloat())
            val py = boardY(it.y.toFloat())
            val r = cell * 0.48f
            val fade = if (it.ttl < 2.5f) {
                val blink = sin(it.ttl * 14f) * 0.5f + 0.5f
                (120 + 135 * blink).toInt().coerceIn(0, 255)
            } else 255
            when (it.kind) {
                Kind.FOOD -> Art.critter(c, px, py, r, it.sub, G.time + it.age)
                Kind.STAR -> Art.starItem(c, px, py, r, G.time + it.age)
                Kind.COIN -> Art.coin(c, px, py, r * 0.9f, G.time + it.age)
                Kind.CHEST -> Art.chest(c, px, py, r, G.time + it.age, fade)
                Kind.POWER -> Art.power(c, px, py, r, it.sub, G.time + it.age, fade)
            }
        }

        // roaming monsters
        for (m in eng.movers) {
            val t = if (state == PLAY) eng.stepT else 0f
            val ix = lerpCell(m.px.toFloat(), m.x.toFloat(), t)
            val iy = lerpCell(m.py.toFloat(), m.y.toFloat(), t)
            Art.mover(c, boardX(ix), boardY(iy), cell * 0.46f, (m.x - m.px).toFloat(), (m.y - m.py).toFloat(), G.time)
        }

        drawSnake(c)
    }

    private fun lerpCell(a: Float, b: Float, t: Float): Float {
        if (abs(a - b) > 1.01f) return b       // wrapped or teleported: snap
        return a + (b - a) * t
    }

    private fun drawSnake(c: Canvas) {
        val skin = Skins.get(Prefs.skin)
        val n = eng.body.size
        val t = if (state == PLAY || state == READY) eng.stepT else 0f
        val dieT = if (state == LOST) (stateT * 2.2f).coerceAtMost(1f) else 0f

        for (i in n - 1 downTo 1) {
            val cur = eng.body[i]
            val prevX: Float
            val prevY: Float
            if (i + 1 < n) {
                prevX = eng.body[i + 1].x.toFloat(); prevY = eng.body[i + 1].y.toFloat()
            } else {
                prevX = eng.tailPrevX.toFloat(); prevY = eng.tailPrevY.toFloat()
            }
            val x = boardX(lerpCell(prevX, cur.x.toFloat(), t))
            val y = boardY(lerpCell(prevY, cur.y.toFloat(), t))
            val taper = 1f - (i.toFloat() / n) * 0.34f
            Art.snakeBody(c, x, y, cell * 0.42f * taper * (1f - dieT * 0.5f), i, skin, G.time, eng.shield)
        }

        val head = eng.body[0]
        val px: Float
        val py: Float
        if (n > 1) { px = eng.body[1].x.toFloat(); py = eng.body[1].y.toFloat() }
        else { px = eng.tailPrevX.toFloat(); py = eng.tailPrevY.toFloat() }
        val hx = boardX(lerpCell(px, head.x.toFloat(), t))
        val hy = boardY(lerpCell(py, head.y.toFloat(), t))

        var mouth = 0f
        for (it in eng.items) {
            if (abs(it.x - head.x) + abs(it.y - head.y) <= 2) { mouth = 1f; break }
        }
        val wob = if (state == LOST) sin(stateT * 26f) * cell * 0.12f * (1f - dieT) else 0f
        Art.snakeHead(
            c, hx + wob, hy, cell * 0.50f * (1f - dieT * 0.25f),
            eng.dirX.toFloat(), eng.dirY.toFloat(), skin, G.time, eng.shield, mouth
        )
    }

    private fun boardFrame(): Int = when (level.theme) {
        Theme.CANDY -> 0xFFB65A8E.toInt()
        Theme.OCEAN -> 0xFF124A66.toInt()
        Theme.SPACE -> 0xFF3B2A63.toInt()
        else -> 0xFF5E4A33.toInt()
    }

    private fun boardBase(): Int = when (level.theme) {
        Theme.CANDY -> 0xFFFFE3F1.toInt()
        Theme.OCEAN -> 0xFF2E86A8.toInt()
        Theme.SPACE -> 0xFF2A2050.toInt()
        else -> 0xFF8ECB6B.toInt()
    }

    private fun drawHud(c: Canvas) {
        val y1 = G.top + 88f * G.s
        pause.draw(c, G.time)

        val title = if (level.isEndless) "ENDLESS ${levelNum - Levels.COUNT}" else "LEVEL $levelNum"
        Ui.ribbon(c, G.w * 0.40f, y1, G.w * 0.52f, 92f * G.s, title, Col.ORANGE)

        val y2 = G.top + 195f * G.s
        val goalStr = if (level.isEndless) "${eng.score}" else "${eng.progress().coerceAtMost(level.target)}/${level.target}"
        val goalIcon = if (level.isEndless) Icon.STAR else eng.goalIcon()
        Ui.chip(c, G.w * 0.30f, y2, G.w * 0.40f, 82f * G.s, goalIcon, goalStr, Col.GREEN)
        Ui.chip(c, G.w * 0.74f, y2, G.w * 0.36f, 82f * G.s, Icon.COIN, eng.coinsEarned.toString(), Col.GOLD_DK)

        if (!level.isEndless) {
            Art.textMid(
                c, "Score ${eng.score}", G.w * 0.40f, G.top + 268f * G.s,
                36f * G.s, Col.WHITE, Col.INK, 6f * G.s
            )
        }

        // active power-up timers down the left edge of the board
        var py = by + cell * 0.7f
        for (k in 0 until Power.COUNT) {
            if (eng.timers[k] <= 0f) continue
            val r = cell * 0.36f
            val px = bx - cell * 0.62f
            Art.power(c, px, py, r, k, G.time)
            val frac = (eng.timers[k] / 12f).coerceIn(0f, 1f)
            Art.rrect(c, px - r * 0.8f, py + r * 1.05f, px + r * 0.8f, py + r * 1.28f, r * 0.12f, 0x66000000)
            Art.rrect(c, px - r * 0.8f, py + r * 1.05f, px - r * 0.8f + r * 1.6f * frac, py + r * 1.28f, r * 0.12f, Art.powerColor(k))
            py += cell * 1.15f
        }
    }

    private fun drawReady(c: Canvas) {
        val a = (0.55f + 0.45f * sin(G.time * 4f))
        val y = by + bh + 74f * G.s
        val hint = if (Prefs.padOn) "Swipe or tap an arrow!" else "Swipe to move!"
        Art.textMid(c, hint, G.w / 2f, y, 52f * G.s, Col.WHITE, Col.INK, 10f * G.s, Paint.Align.CENTER, (255 * a).toInt())
        Art.textMid(c, level.goalText(), G.w / 2f, by - 36f * G.s, 44f * G.s, Col.STAR, Col.INK, 9f * G.s)
    }

    private fun drawPaused(c: Canvas) {
        Ui.dim(c, G.w, G.h, 170)
        val pw = G.w * 0.78f
        val ph = G.h * 0.34f
        val l = (G.w - pw) / 2f
        val t = G.h * 0.36f
        Ui.panel(c, l, t, l + pw, t + ph, 54f * G.s)
        Art.textMid(c, "PAUSED", G.w / 2f, t + 78f * G.s, 78f * G.s, Col.ORANGE, Col.INK, 12f * G.s)
        resume.draw(c, G.time)
        replay.draw(c, G.time)
        home.draw(c, G.time)
    }

    private fun drawWon(c: Canvas) {
        Ui.dim(c, G.w, G.h, (150 * (stateT * 2f).coerceAtMost(1f)).toInt())
        val pw = G.w * 0.84f
        val ph = G.h * 0.40f
        val l = (G.w - pw) / 2f
        val t = G.h * 0.20f
        val pop = Fx.bounce((stateT * 2.4f).coerceIn(0f, 1f))
        c.save()
        c.scale(pop, pop, G.w / 2f, t + ph / 2f)
        Ui.panel(c, l, t, l + pw, t + ph, 54f * G.s)
        Art.textMid(c, "WELL DONE!", G.w / 2f, t + 80f * G.s, 76f * G.s, Col.GREEN, Col.INK, 12f * G.s)
        Ui.starRow(c, G.w / 2f, t + ph * 0.46f, 62f * G.s, gotStars, 3, G.time, (stateT * 0.9f).coerceIn(0f, 1f))
        Ui.chip(c, G.w / 2f, t + ph * 0.78f, pw * 0.56f, 86f * G.s, Icon.COIN, "+$gotCoins", Col.GOLD_DK)
        c.restore()

        if (levelNum >= Levels.COUNT) {
            next.label = "ENDLESS"
        }
        next.draw(c, G.time)
        replay.draw(c, G.time)
        home.draw(c, G.time)
    }

    private fun drawLost(c: Canvas) {
        Ui.dim(c, G.w, G.h, (150 * (stateT * 2f).coerceAtMost(1f)).toInt())
        val pw = G.w * 0.82f
        val ph = G.h * 0.30f
        val l = (G.w - pw) / 2f
        val t = G.h * 0.22f
        val pop = Fx.bounce((stateT * 2.4f).coerceIn(0f, 1f))
        c.save()
        c.scale(pop, pop, G.w / 2f, t + ph / 2f)
        Ui.panel(c, l, t, l + pw, t + ph, 54f * G.s)
        Art.textMid(c, "OOPS!", G.w / 2f, t + 82f * G.s, 80f * G.s, Col.ORANGE, Col.INK, 12f * G.s)
        val r = 58f * G.s
        val skin = Skins.get(Prefs.skin)
        Art.snakeHead(c, G.w / 2f, t + ph * 0.60f, r, 0f, 1f, skin, G.time, false, 0.2f)
        Art.textMid(
            c, if (level.isEndless) "Score ${eng.score}" else "Try again - you can do it!",
            G.w / 2f, t + ph * 0.90f, 42f * G.s, Col.INK_SOFT, Col.INK, 0f
        )
        c.restore()

        again.draw(c, G.time)
        toLevels.draw(c, G.time)
    }

    // -------------------------------------------------------------- input

    override fun down(x: Float, y: Float) {
        when (state) {
            PAUSED -> {
                if (resume.onDown(x, y) || replay.onDown(x, y) || home.onDown(x, y)) Sfx.play(Sfx.CLICK)
                return
            }
            WON -> {
                if (next.onDown(x, y) || replay.onDown(x, y) || home.onDown(x, y)) Sfx.play(Sfx.CLICK)
                return
            }
            LOST -> {
                if (again.onDown(x, y) || toLevels.onDown(x, y)) Sfx.play(Sfx.CLICK)
                return
            }
        }
        if (pause.onDown(x, y)) { Sfx.play(Sfx.CLICK); return }
        if (Prefs.padOn) {
            if (up.onDown(x, y)) { eng.turn(0, -1); return }
            if (down.onDown(x, y)) { eng.turn(0, 1); return }
            if (left.onDown(x, y)) { eng.turn(-1, 0); return }
            if (right.onDown(x, y)) { eng.turn(1, 0); return }
        }
        downX = x
        downY = y
        swiped = false
    }

    override fun move(x: Float, y: Float) {
        if (state != PLAY && state != READY) return
        if (swiped) return
        val dx = x - downX
        val dy = y - downY
        val thr = 42f * G.s
        if (abs(dx) < thr && abs(dy) < thr) return
        if (abs(dx) > abs(dy)) eng.turn(if (dx > 0) 1 else -1, 0)
        else eng.turn(0, if (dy > 0) 1 else -1)
        // allow a second flick without lifting the finger
        downX = x
        downY = y
    }

    override fun up(x: Float, y: Float) {
        when (state) {
            PAUSED -> {
                if (resume.onUp(x, y)) { state = PLAY; return }
                if (replay.onUp(x, y)) { restart(); return }
                if (home.onUp(x, y)) { Nav.go(MenuScreen()); return }
                resume.cancel(); replay.cancel(); home.cancel()
                return
            }
            WON -> {
                if (next.onUp(x, y)) {
                    val n = if (levelNum >= Levels.COUNT) Levels.COUNT + 1 else levelNum + 1
                    Nav.go(PlayScreen(n))
                    return
                }
                if (replay.onUp(x, y)) { restart(); return }
                if (home.onUp(x, y)) { Nav.go(LevelScreen()); return }
                next.cancel(); replay.cancel(); home.cancel()
                return
            }
            LOST -> {
                if (again.onUp(x, y)) { restart(); return }
                if (toLevels.onUp(x, y)) { Nav.go(LevelScreen()); return }
                again.cancel(); toLevels.cancel()
                return
            }
        }
        if (pause.onUp(x, y)) {
            state = PAUSED
            stateT = 0f
            return
        }
        pause.cancel()
        up.cancel(); down.cancel(); left.cancel(); right.cancel()
    }

    private fun restart() {
        Fx.clear()
        eng = Engine(level)
        state = READY
        stateT = 0f
        saved = false
        gotStars = 0
        gotCoins = 0
    }

    override fun back(): Boolean {
        when (state) {
            PLAY, READY -> { state = PAUSED; stateT = 0f }
            PAUSED -> Nav.go(MenuScreen())
            else -> Nav.go(LevelScreen())
        }
        return true
    }
}
