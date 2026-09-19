package com.bambiproj.monstersnake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless play-through of every level. A bot steers the snake with a
 * breadth-first search plus the usual "don't wall yourself in" check, which
 * is roughly how a careful child plays. The test fails if a level is
 * unwinnable, kills the player instantly, or throws.
 */
class GameplayTest {

    private val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

    private fun blockedGrid(e: Engine): Array<BooleanArray> {
        val cols = e.cols
        val rows = e.rows
        val g = Array(rows) { BooleanArray(cols) }
        for (cell in e.walls) g[cell / cols][cell % cols] = true
        for (i in 0 until e.body.size - 1) g[e.body[i].y][e.body[i].x] = true
        for (m in e.movers) {
            g[m.y][m.x] = true
            val nx = m.x + m.dx
            val ny = m.y + m.dy
            if (nx in 0 until cols && ny in 0 until rows) g[ny][nx] = true
        }
        return g
    }

    private fun stepTo(e: Engine, x: Int, y: Int, d: IntArray): IntArray? {
        var nx = x + d[0]
        var ny = y + d[1]
        if (e.level.wrap) {
            nx = (nx + e.cols) % e.cols
            ny = (ny + e.rows) % e.rows
        }
        if (nx < 0 || ny < 0 || nx >= e.cols || ny >= e.rows) return null
        return intArrayOf(nx, ny)
    }

    /** How much room is left after a move - keeps the bot from boxing itself in. */
    private fun roomAfter(e: Engine, d: IntArray): Int {
        val blocked = blockedGrid(e)
        val start = stepTo(e, e.body[0].x, e.body[0].y, d) ?: return 0
        if (blocked[start[1]][start[0]]) return 0
        val seen = Array(e.rows) { BooleanArray(e.cols) }
        val q = ArrayDeque<Int>()
        seen[start[1]][start[0]] = true
        q.add(start[1] * e.cols + start[0])
        var n = 0
        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            n++
            val cx = cur % e.cols
            val cy = cur / e.cols
            for (dd in dirs) {
                val p = stepTo(e, cx, cy, dd) ?: continue
                if (blocked[p[1]][p[0]] || seen[p[1]][p[0]]) continue
                seen[p[1]][p[0]] = true
                q.add(p[1] * e.cols + p[0])
            }
        }
        return n
    }

    /** Breadth-first step towards the nearest collectible worth having. */
    private fun chooseMove(e: Engine): IntArray? {
        val cols = e.cols
        val rows = e.rows
        val head = e.body[0]
        val blocked = blockedGrid(e)

        val targets = HashSet<Int>()
        for (it in e.items) {
            val wanted = when (e.level.goal) {
                Goal.STARS -> it.kind == Kind.STAR || it.kind == Kind.FOOD
                Goal.COINS -> it.kind == Kind.COIN || it.kind == Kind.FOOD
                else -> it.kind != Kind.POWER
            }
            if (wanted) targets.add(it.y * cols + it.x)
        }

        var bestDir: IntArray? = null
        if (targets.isNotEmpty()) {
            val dist = Array(rows) { IntArray(cols) { -1 } }
            val first = Array(rows) { arrayOfNulls<IntArray>(cols) }
            val queue = ArrayDeque<Int>()
            dist[head.y][head.x] = 0
            queue.add(head.y * cols + head.x)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                val cx = cur % cols
                val cy = cur / cols
                if (targets.contains(cur) && dist[cy][cx] > 0) {
                    bestDir = first[cy][cx]
                    break
                }
                for (d in dirs) {
                    // portals hop you elsewhere; the bot simply avoids them
                    var portal = false
                    for (pp in e.portals) if (pp.x == cx + d[0] && pp.y == cy + d[1]) portal = true
                    if (portal) continue
                    val p = stepTo(e, cx, cy, d) ?: continue
                    if (blocked[p[1]][p[0]] || dist[p[1]][p[0]] != -1) continue
                    dist[p[1]][p[0]] = dist[cy][cx] + 1
                    first[p[1]][p[0]] = if (dist[cy][cx] == 0) d else first[cy][cx]
                    queue.add(p[1] * cols + p[0])
                }
            }
        }

        // Weigh "go get it" against "don't wall yourself in", the way a
        // careful player does.
        val need = e.body.size + 2
        var best: IntArray? = null
        var bestScore = Int.MIN_VALUE
        for (d in dirs) {
            if (d[0] == -e.dirX && d[1] == -e.dirY) continue
            val room = roomAfter(e, d)
            if (room <= 0) continue
            var score = room
            if (bestDir != null && d[0] == bestDir[0] && d[1] == bestDir[1]) score += 1000
            if (room < need) score -= 3000
            val p = stepTo(e, e.body[0].x, e.body[0].y, d)
            if (p != null) score -= nearestTarget(e, p[0], p[1])
            if (score > bestScore) { bestScore = score; best = d }
        }
        return best ?: bestDir
    }

    private fun nearestTarget(e: Engine, x: Int, y: Int): Int {
        var best = e.cols + e.rows
        for (it in e.items) {
            if (it.kind == Kind.POWER) continue
            val d = Math.abs(it.x - x) + Math.abs(it.y - y)
            if (d < best) best = d
        }
        return best
    }

    private fun playLevel(num: Int, maxSteps: Int): Engine {
        val level = Levels.get(num)
        val e = Engine(level)
        val step = level.stepMs / 1000f
        var steps = 0
        while (steps < maxSteps && e.alive && !e.won) {
            val d = chooseMove(e)
            if (d != null) e.turn(d[0], d[1])
            else e.turn(e.dirX, e.dirY)
            e.update(step)
            steps++
        }
        return e
    }

    @Test
    fun everyLevelIsWinnable() {
        for (num in 1..Levels.COUNT) {
            val e = playLevel(num, 6000)
            assertTrue(
                "level $num unfinished alive=${e.alive} progress=${e.progress()}" +
                    "/${e.level.target} score=${e.score} len=${e.body.size} items=${e.items.size}",
                e.won
            )
        }
    }

    @Test
    fun theSnakeSurvivesTheOpeningMoves() {
        // Straight ahead from the start must be safe for a few squares on
        // every level, so a child is never killed before they can react.
        for (num in 1..Levels.COUNT) {
            val level = Levels.get(num)
            val e = Engine(level)
            e.turn(1, 0)
            val step = level.stepMs / 1000f
            for (i in 0 until 3) e.update(step)
            assertTrue("level $num kills the player in the first 3 steps", e.alive)
        }
    }

    @Test
    fun layoutsLeaveTheBoardUsable() {
        for (num in 1..Levels.COUNT) {
            val level = Levels.get(num)
            val e = Engine(level)
            val total = level.cols * level.rows
            assertTrue("level $num is more than a third walls", e.walls.size < total / 3)
            assertEquals("level $num has unpaired portals", 0, e.portals.size % 2)
            if (level.portals) assertTrue("level $num should have portals", e.portals.size >= 2)
            assertEquals("level $num mover count", level.movers, e.movers.size)
        }
    }

    @Test
    fun levelTableIsSane() {
        var prevStep = Int.MAX_VALUE
        for (num in 1..Levels.COUNT) {
            val l = Levels.get(num)
            assertTrue("level $num has no goal", l.target > 0)
            assertTrue("level $num star2 must be positive", l.star2 > 0)
            assertTrue("level $num star3 must beat star2", l.star3 > l.star2)
            if (l.goal == Goal.SCORE) {
                assertTrue("level $num star2 must be at least the goal", l.star2 >= l.target)
            }
            assertTrue("level $num got faster than it should", l.stepMs <= prevStep)
            assertTrue("level $num is too fast for a 6 year old", l.stepMs >= 150)
            prevStep = l.stepMs
        }
    }

    @Test
    fun endlessModeKeepsGoing() {
        for (num in Levels.COUNT + 1..Levels.COUNT + 6) {
            val e = playLevel(num, 600)
            assertTrue("endless level $num ended instantly", e.score > 0)
        }
    }

    @Test
    fun shieldSavesInsteadOfKilling() {
        val level = Levels.get(4)       // solid walls, no wrap
        val e = Engine(level)
        val step = level.stepMs / 1000f
        // grant a shield the same way a power-up would
        e.items.add(Item(e.body[0].x, e.body[0].y - 1, Kind.POWER, Power.SHIELD))
        e.turn(0, -1)                   // then head straight for the top wall
        var rescued = false
        for (i in 0 until 40) {
            e.update(step)
            if (e.events.any { it.type == Evt.SAVED }) { rescued = true; break }
            if (!e.alive) break
        }
        assertTrue("the shield should have caught the wall", rescued)
        assertTrue("a shielded snake should survive its first crash", e.alive)
        assertTrue("the shield should be used up", !e.shield)
    }

    @Test
    fun withoutAShieldAWallEndsTheRun() {
        val level = Levels.get(4)
        val e = Engine(level)
        val step = level.stepMs / 1000f
        e.turn(0, -1)
        for (i in 0 until 40) {
            e.update(step)
            if (!e.alive) break
        }
        assertTrue("hitting a wall unprotected should end the run", !e.alive)
    }

    @Test
    fun progressUnlocksAndSavesLocally() {
        Prefs.unlocked = 1
        Prefs.unlockLevel(2)
        assertEquals(2, Prefs.unlocked)
        Prefs.unlockLevel(1)
        assertEquals("unlocking must never go backwards", 2, Prefs.unlocked)
        Prefs.unlockLevel(999)
        assertEquals("endless is the last unlock", Levels.COUNT + 1, Prefs.unlocked)

        Prefs.setStars(3, 2)
        Prefs.setStars(3, 1)
        assertEquals("stars must never go down", 2, Prefs.stars(3))

        assertTrue("the starter skin is always owned", Prefs.ownsSkin(0))
        assertTrue("other skins start locked", !Prefs.ownsSkin(5))
        Prefs.unlockSkin(5)
        assertTrue(Prefs.ownsSkin(5))
    }
}
