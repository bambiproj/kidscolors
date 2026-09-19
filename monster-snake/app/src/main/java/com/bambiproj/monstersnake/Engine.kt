package com.bambiproj.monstersnake

import kotlin.math.abs
import kotlin.random.Random

object Kind {
    const val FOOD = 0
    const val STAR = 1
    const val COIN = 2
    const val CHEST = 3
    const val POWER = 4
}

object Evt {
    const val FOOD = 0
    const val STAR = 1
    const val COIN = 2
    const val CHEST = 3
    const val POWER = 4
    const val CRASH = 5
    const val SAVED = 6
    const val WIN = 7
    const val PORTAL = 8
    const val NEW_CRITTER = 9
}

class Seg(var x: Int, var y: Int)

class Item(var x: Int, var y: Int, val kind: Int, val sub: Int) {
    var age = 0f
    var ttl = Float.MAX_VALUE
}

class Mover(var x: Int, var y: Int, var dx: Int, var dy: Int) {
    var px = x
    var py = y
}

class Event(val type: Int, val x: Int, val y: Int, val sub: Int)

/**
 * The whole game rule-set. Deliberately forgiving: shields bounce you away
 * instead of killing you, early levels wrap around the walls, and the board
 * never runs out of things to collect.
 */
class Engine(val level: Level) {

    val cols = level.cols
    val rows = level.rows
    val walls: HashSet<Int> = Levels.obstacles(level.layout, cols, rows)

    val body = ArrayList<Seg>()
    val items = ArrayList<Item>()
    val movers = ArrayList<Mover>()
    val portals = ArrayList<Seg>()
    val events = ArrayList<Event>()

    var dirX = 1
    var dirY = 0
    private var wantX = 1
    private var wantY = 0

    var tailPrevX = 0
    var tailPrevY = 0
    var headJumped = false          // teleported this step: don't interpolate

    var score = 0
    var eaten = 0
    var starsGot = 0
    var coinsGot = 0
    var coinsEarned = 0
    var alive = true
    var won = false
    var started = false             // waits for the first swipe

    var shield = false
    val timers = FloatArray(Power.COUNT)

    private var grow = 0
    private var stepAcc = 0f
    var stepT = 0f
    private var tick = 0
    private var foodSinceChest = 0
    private var powerCooldown = 6f
    private val rnd = Random(level.num * 7919L + 13)

    init {
        val sy = rows / 2
        val sx = 3
        body.add(Seg(sx, sy))
        body.add(Seg(sx - 1, sy))
        body.add(Seg(sx - 2, sy))
        tailPrevX = sx - 3
        tailPrevY = sy

        if (level.portals) {
            // Portals win over blocks: carve the square out so a pair always exists.
            addPortalPair(Seg(2, 2), Seg(cols - 3, rows - 3))
            addPortalPair(Seg(cols - 3, 2), Seg(2, rows - 3))
        }

        for (i in 0 until level.movers) {
            val c = freeCell(true) ?: continue
            val horiz = i % 2 == 0
            movers.add(Mover(c % cols, c / cols, if (horiz) 1 else 0, if (horiz) 0 else 1))
        }

        spawnFood()
        if (level.goal == Goal.STARS) spawnStar()
        if (level.goal == Goal.COINS) { spawnCoin(); spawnCoin() }
    }

    private fun addPortalPair(a: Seg, b: Seg) {
        val startRow = rows / 2
        if (a.y == startRow || b.y == startRow) return
        walls.remove(a.y * cols + a.x)
        walls.remove(b.y * cols + b.x)
        portals.add(a)
        portals.add(b)
    }

    // ------------------------------------------------------------- helpers

    private fun cell(x: Int, y: Int) = y * cols + x

    private fun blockedStatic(x: Int, y: Int): Boolean =
        x < 0 || y < 0 || x >= cols || y >= rows || walls.contains(cell(x, y))

    private fun onSnake(x: Int, y: Int): Boolean {
        for (s in body) if (s.x == x && s.y == y) return true
        return false
    }

    private fun onItem(x: Int, y: Int): Boolean {
        for (s in items) if (s.x == x && s.y == y) return true
        return false
    }

    private fun onMover(x: Int, y: Int): Boolean {
        for (s in movers) if (s.x == x && s.y == y) return true
        return false
    }

    private fun onPortal(x: Int, y: Int): Boolean {
        for (s in portals) if (s.x == x && s.y == y) return true
        return false
    }

    /** A random empty square, or null if the board is packed. */
    private fun freeCell(awayFromStart: Boolean = false): Int? {
        val free = ArrayList<Int>()
        val hx = body[0].x
        val hy = body[0].y
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (blockedStatic(x, y) || onSnake(x, y) || onItem(x, y) || onMover(x, y) || onPortal(x, y)) continue
                if (awayFromStart && abs(x - hx) + abs(y - hy) < 5) continue
                // never right in front of the head
                if (!awayFromStart && abs(x - hx) + abs(y - hy) < 2) continue
                free.add(cell(x, y))
            }
        }
        if (free.isEmpty()) return null
        return free[rnd.nextInt(free.size)]
    }

    private fun spawnAt(kind: Int, sub: Int, ttl: Float = Float.MAX_VALUE) {
        val c = freeCell() ?: return
        val it = Item(c % cols, c / cols, kind, sub)
        it.ttl = ttl
        items.add(it)
    }

    private fun spawnFood() = spawnAt(Kind.FOOD, rnd.nextInt(Critters.all.size))
    private fun spawnStar() = spawnAt(Kind.STAR, 0)
    private fun spawnCoin() = spawnAt(Kind.COIN, 0)

    private fun countKind(kind: Int): Int {
        var n = 0
        for (s in items) if (s.kind == kind) n++
        return n
    }

    fun powerActive(kind: Int): Boolean = timers[kind] > 0f

    fun stepSeconds(): Float {
        var s = level.stepMs / 1000f
        if (powerActive(Power.SPEED)) s *= 0.62f
        if (powerActive(Power.SLOW)) s *= 1.55f
        return s
    }

    fun turn(nx: Int, ny: Int) {
        if (nx == 0 && ny == 0) return
        // no instant 180 turns into your own neck
        if (nx == -dirX && ny == -dirY && body.size > 1) return
        wantX = nx
        wantY = ny
        started = true
    }

    // ---------------------------------------------------------------- loop

    fun update(dt: Float) {
        if (!alive || won) return

        for (i in timers.indices) if (timers[i] > 0f) timers[i] -= dt
        if (shield && timers[Power.SHIELD] <= 0f) shield = false
        for (s in items) {
            s.age += dt
            s.ttl -= dt
        }
        var i = items.size - 1
        while (i >= 0) {
            if (items[i].ttl <= 0f) items.removeAt(i)
            i--
        }

        if (!started) return

        val step = stepSeconds()
        stepAcc += dt
        var guard = 0
        while (stepAcc >= step && alive && !won && guard < 4) {
            stepAcc -= step
            doStep()
            guard++
        }
        if (stepAcc > step) stepAcc = step
        stepT = if (step > 0f) (stepAcc / step).coerceIn(0f, 1f) else 0f

        powerCooldown -= dt
        if (level.powers.isNotEmpty() && powerCooldown <= 0f && countKind(Kind.POWER) == 0) {
            spawnAt(Kind.POWER, level.powers[rnd.nextInt(level.powers.size)], 11f)
            powerCooldown = 9f + rnd.nextFloat() * 5f
        }
    }

    private fun doStep() {
        tick++
        headJumped = false

        if (wantX != -dirX || wantY != -dirY || body.size <= 1) {
            dirX = wantX
            dirY = wantY
        }

        val head = body[0]
        var nx = head.x + dirX
        var ny = head.y + dirY

        if (level.wrap) {
            if (nx < 0) { nx = cols - 1; headJumped = true }
            if (nx >= cols) { nx = 0; headJumped = true }
            if (ny < 0) { ny = rows - 1; headJumped = true }
            if (ny >= rows) { ny = 0; headJumped = true }
        } else if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) {
            crash(head.x, head.y)
            return
        }

        // portals - always spit the snake out somewhere it can survive
        for (k in portals.indices) {
            val pp = portals[k]
            if (pp.x == nx && pp.y == ny) {
                val other = portals[if (k % 2 == 0) k + 1 else k - 1]
                val exit = portalExit(other) ?: break
                nx = exit[0]
                ny = exit[1]
                dirX = exit[2]
                dirY = exit[3]
                wantX = dirX
                wantY = dirY
                headJumped = true
                events.add(Event(Evt.PORTAL, other.x, other.y, 0))
                break
            }
        }

        if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) {
            crash(head.x, head.y)
            return
        }
        if (walls.contains(cell(nx, ny))) {
            crash(head.x, head.y)
            return
        }
        if (onMover(nx, ny)) {
            crash(nx, ny)
            return
        }
        // own body - the very last segment moves away this step, so it is safe
        val last = body[body.size - 1]
        for (si in 0 until body.size) {
            val s = body[si]
            if (s.x == nx && s.y == ny) {
                if (grow == 0 && s === last) break
                crash(nx, ny)
                return
            }
        }

        tailPrevX = last.x
        tailPrevY = last.y
        body.add(0, Seg(nx, ny))
        if (grow > 0) grow-- else body.removeAt(body.size - 1)

        pickUp(nx, ny)

        if (movers.isNotEmpty() && tick % 2 == 0) stepMovers()
        if (powerActive(Power.MAGNET)) pullItems(nx, ny)

        checkGoal()
    }

    /**
     * Where a portal drops you: straight on if it is clear, otherwise sideways
     * or back the way you came. Returns x, y and the new heading, or null when
     * the far side is completely boxed in (then the jump simply does not
     * happen, instead of killing the player).
     */
    private fun portalExit(other: Seg): IntArray? {
        val opts = arrayOf(
            intArrayOf(dirX, dirY),
            intArrayOf(dirY, dirX),
            intArrayOf(-dirY, -dirX),
            intArrayOf(-dirX, -dirY)
        )
        for (o in opts) {
            var ex = other.x + o[0]
            var ey = other.y + o[1]
            if (level.wrap) {
                ex = (ex + cols) % cols
                ey = (ey + rows) % rows
            }
            if (ex < 0 || ey < 0 || ex >= cols || ey >= rows) continue
            if (walls.contains(cell(ex, ey))) continue
            if (onSnake(ex, ey)) continue
            if (onMover(ex, ey)) continue
            if (onPortal(ex, ey)) continue
            return intArrayOf(ex, ey, o[0], o[1])
        }
        return null
    }

    private fun pickUp(x: Int, y: Int) {
        var i = items.size - 1
        while (i >= 0) {
            val it = items[i]
            if (it.x != x || it.y != y) { i--; continue }
            items.removeAt(i)
            val mult = if (powerActive(Power.SPEED)) 2 else 1
            when (it.kind) {
                Kind.FOOD -> {
                    eaten++
                    grow += 1
                    score += 10 * mult
                    coinsEarned += 1
                    foodSinceChest++
                    events.add(Event(Evt.FOOD, x, y, it.sub))
                    if (Prefs.meetCreature(it.sub)) events.add(Event(Evt.NEW_CRITTER, x, y, it.sub))
                    spawnFood()
                    if (level.goal == Goal.STARS && countKind(Kind.STAR) == 0) spawnStar()
                    else if (rnd.nextFloat() < 0.28f && countKind(Kind.STAR) == 0) spawnStar()
                    if (level.goal == Goal.COINS) {
                        var guard = 0
                        while (countKind(Kind.COIN) < 2 && guard < 4) { spawnCoin(); guard++ }
                    } else if (rnd.nextFloat() < 0.42f && countKind(Kind.COIN) < 2) spawnCoin()
                    if (level.num >= 4 && foodSinceChest >= 6 && countKind(Kind.CHEST) == 0) {
                        foodSinceChest = 0
                        spawnAt(Kind.CHEST, 0, 14f)
                    }
                }
                Kind.STAR -> {
                    starsGot++
                    score += 25 * mult
                    coinsEarned += 2
                    events.add(Event(Evt.STAR, x, y, 0))
                    if (level.goal == Goal.STARS) spawnStar()
                }
                Kind.COIN -> {
                    coinsGot++
                    score += 5 * mult
                    coinsEarned += 5
                    events.add(Event(Evt.COIN, x, y, 0))
                    if (level.goal == Goal.COINS) spawnCoin()
                }
                Kind.CHEST -> {
                    score += 50 * mult
                    coinsEarned += 15
                    coinsGot += 3
                    events.add(Event(Evt.CHEST, x, y, 0))
                }
                Kind.POWER -> {
                    applyPower(it.sub)
                    events.add(Event(Evt.POWER, x, y, it.sub))
                    powerCooldown = 9f + rnd.nextFloat() * 5f
                }
            }
            i--
        }
    }

    private fun applyPower(kind: Int) {
        when (kind) {
            Power.SHIELD -> { shield = true; timers[Power.SHIELD] = 12f }
            Power.SPEED -> timers[Power.SPEED] = 7f
            Power.MAGNET -> timers[Power.MAGNET] = 9f
            Power.SLOW -> timers[Power.SLOW] = 7f
            else -> { score += 30; coinsEarned += 3 }
        }
    }

    /** Magnet: nearby goodies shuffle one square towards the snake each step. */
    private fun pullItems(hx: Int, hy: Int) {
        for (it in items) {
            if (it.kind == Kind.POWER) continue
            val dx = hx - it.x
            val dy = hy - it.y
            if (abs(dx) + abs(dy) > 5) continue
            var nx = it.x
            var ny = it.y
            if (abs(dx) >= abs(dy)) nx += if (dx > 0) 1 else -1 else ny += if (dy > 0) 1 else -1
            if (!blockedStatic(nx, ny) && !onSnake(nx, ny) && !onItem(nx, ny) && !onMover(nx, ny)) {
                it.x = nx
                it.y = ny
            }
        }
    }

    /**
     * Roaming monsters patrol back and forth. They never walk into the snake -
     * they turn around instead - so the only way to lose one is to drive into
     * it yourself. Being hit from behind by something you cannot see coming is
     * not a fair way for a small player to lose.
     */
    private fun stepMovers() {
        for (m in movers) {
            m.px = m.x
            m.py = m.y
            var nx = m.x + m.dx
            var ny = m.y + m.dy
            if (moverBlocked(nx, ny)) {
                m.dx = -m.dx
                m.dy = -m.dy
                nx = m.x + m.dx
                ny = m.y + m.dy
                if (moverBlocked(nx, ny)) { nx = m.x; ny = m.y }
            }
            m.x = nx
            m.y = ny
        }
    }

    private fun moverBlocked(x: Int, y: Int): Boolean =
        blockedStatic(x, y) || onPortal(x, y) || onSnake(x, y)

    private fun crash(x: Int, y: Int) {
        if (shield) {
            shield = false
            timers[Power.SHIELD] = 0f
            events.add(Event(Evt.SAVED, x, y, 0))
            bounceBack()
            return
        }
        alive = false
        events.add(Event(Evt.CRASH, x, y, 0))
    }

    /** Shield save: flip the snake around and send it somewhere safe. */
    private fun bounceBack() {
        body.reverse()
        if (body.size > 1) {
            dirX = (body[0].x - body[1].x).coerceIn(-1, 1)
            dirY = (body[0].y - body[1].y).coerceIn(-1, 1)
        }
        if (dirX == 0 && dirY == 0) { dirX = 1; dirY = 0 }
        if (!canGo(dirX, dirY)) {
            val opts = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
            for (o in opts) {
                if (canGo(o[0], o[1])) { dirX = o[0]; dirY = o[1]; break }
            }
        }
        wantX = dirX
        wantY = dirY
        stepAcc = 0f
    }

    private fun canGo(dx: Int, dy: Int): Boolean {
        var nx = body[0].x + dx
        var ny = body[0].y + dy
        if (level.wrap) {
            nx = (nx + cols) % cols
            ny = (ny + rows) % rows
        }
        if (blockedStatic(nx, ny)) return false
        if (onMover(nx, ny)) return false
        for (i in 0 until body.size - 1) if (body[i].x == nx && body[i].y == ny) return false
        return true
    }

    private fun checkGoal() {
        if (level.isEndless) return
        val done = when (level.goal) {
            Goal.EAT -> eaten >= level.target
            Goal.SCORE -> score >= level.target
            Goal.STARS -> starsGot >= level.target
            else -> coinsGot >= level.target
        }
        if (done) {
            won = true
            events.add(Event(Evt.WIN, body[0].x, body[0].y, 0))
        }
    }

    fun progress(): Int = when (level.goal) {
        Goal.EAT -> eaten
        Goal.SCORE -> score
        Goal.STARS -> starsGot
        else -> coinsGot
    }

    fun goalIcon(): Int = when (level.goal) {
        Goal.EAT -> Icon.SKIN
        Goal.SCORE -> Icon.STAR
        Goal.STARS -> Icon.STAR
        else -> Icon.COIN
    }

    /** Stars awarded for how well the level went. */
    fun starsAwarded(): Int {
        if (level.isEndless) return 0
        var s = 1
        if (score >= level.star2) s = 2
        if (score >= level.star3) s = 3
        return s
    }
}
