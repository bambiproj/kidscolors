package com.bambiproj.monstersnake

/** What the child has to do to finish a level. Kept to four very visual goals. */
object Goal {
    const val EAT = 0      // catch N creatures
    const val SCORE = 1    // reach N points
    const val STARS = 2    // collect N stars
    const val COINS = 3    // collect N coins
}

object Power {
    const val SHIELD = 0
    const val SPEED = 1
    const val MAGNET = 2
    const val SLOW = 3
    const val BONUS = 4
    const val COUNT = 5
}

object Theme {
    const val MEADOW = 0
    const val CANDY = 1
    const val OCEAN = 2
    const val SPACE = 3

    val names = arrayOf("Green Meadow", "Candy Hills", "Coral Sea", "Star Space")
}

class Level(
    val num: Int,
    val name: String,
    val cols: Int,
    val rows: Int,
    val stepMs: Int,
    val wrap: Boolean,
    val layout: Int,
    val movers: Int,
    val portals: Boolean,
    val goal: Int,
    val target: Int,
    val powers: IntArray,
    val theme: Int,
    val star2: Int,
    val star3: Int
) {
    val isEndless: Boolean get() = num > Levels.COUNT

    fun goalText(): String = when (goal) {
        Goal.EAT -> "Catch $target monsters"
        Goal.SCORE -> "Score $target points"
        Goal.STARS -> "Collect $target stars"
        else -> "Collect $target coins"
    }
}

object Levels {
    const val COUNT = 20

    private val NONE = intArrayOf()
    private val P_SH = intArrayOf(Power.SHIELD)
    private val P_SH_BO = intArrayOf(Power.SHIELD, Power.BONUS)
    private val P_SH_SP = intArrayOf(Power.SHIELD, Power.SPEED)
    private val P_SH_MA = intArrayOf(Power.SHIELD, Power.MAGNET)
    private val P_SH_BO_MA = intArrayOf(Power.SHIELD, Power.BONUS, Power.MAGNET)
    private val P_MOST = intArrayOf(Power.SHIELD, Power.BONUS, Power.MAGNET, Power.SLOW)
    private val P_ALL = intArrayOf(Power.SHIELD, Power.BONUS, Power.MAGNET, Power.SLOW, Power.SPEED)

    private val table: Array<Level> = arrayOf(
        //     n  name              c   r  step  wrap lay mov port goal        tgt  powers      theme          s2   s3
        Level(1, "First Wiggle",    10, 13, 300, true, 0, 0, false, Goal.EAT, 5, NONE, Theme.MEADOW, 60, 90),
        Level(2, "Star Picnic",     10, 13, 285, true, 0, 0, false, Goal.STARS, 3, NONE, Theme.MEADOW, 110, 150),
        Level(3, "Rocky Corners",   11, 14, 270, true, 1, 0, false, Goal.EAT, 8, NONE, Theme.MEADOW, 120, 170),
        Level(4, "Coin Meadow",     11, 14, 255, false, 0, 0, false, Goal.COINS, 8, P_SH, Theme.MEADOW, 130, 180),
        Level(5, "The Big Cross",   11, 14, 245, false, 2, 0, false, Goal.SCORE, 120, P_SH_BO, Theme.MEADOW, 180, 240),

        Level(6, "Candy Gates",     11, 15, 240, false, 3, 0, false, Goal.EAT, 10, P_SH_BO, Theme.CANDY, 180, 240),
        Level(7, "Lollipop Pillars", 11, 15, 232, false, 4, 0, false, Goal.SCORE, 170, P_SH_SP, Theme.CANDY, 240, 310),
        Level(8, "Zigzag Sweets",   12, 16, 226, false, 6, 0, false, Goal.STARS, 4, P_SH_MA, Theme.CANDY, 220, 300),
        Level(9, "Sugar Rings",     12, 16, 220, false, 5, 0, false, Goal.EAT, 12, P_SH_BO_MA, Theme.CANDY, 240, 320),
        Level(10, "Candy Diamond",  12, 16, 214, false, 7, 0, false, Goal.SCORE, 220, P_MOST, Theme.CANDY, 300, 390),

        Level(11, "Coral Cross",    12, 16, 212, false, 2, 1, false, Goal.EAT, 12, P_SH_BO, Theme.OCEAN, 250, 330),
        Level(12, "Kelp Gates",     12, 16, 208, false, 3, 1, false, Goal.STARS, 5, P_SH_MA, Theme.OCEAN, 270, 360),
        Level(13, "Bubble Field",   12, 16, 204, false, 8, 2, false, Goal.COINS, 14, P_MOST, Theme.OCEAN, 290, 380),
        Level(14, "Deep Zigzag",    12, 16, 200, false, 6, 2, false, Goal.SCORE, 250, P_MOST, Theme.OCEAN, 330, 420),
        Level(15, "Tunnel Reef",    12, 16, 196, false, 9, 2, false, Goal.EAT, 15, P_ALL, Theme.OCEAN, 320, 420),

        Level(16, "Portal Park",    12, 17, 194, false, 1, 0, true, Goal.STARS, 5, P_SH_BO, Theme.SPACE, 300, 400),
        Level(17, "Ring Station",   12, 17, 190, false, 5, 1, true, Goal.SCORE, 290, P_MOST, Theme.SPACE, 380, 480),
        Level(18, "Comet Diamond",  12, 17, 186, false, 7, 2, true, Goal.EAT, 16, P_MOST, Theme.SPACE, 340, 440),
        Level(19, "Meteor Grid",    12, 17, 182, false, 8, 2, true, Goal.STARS, 6, P_ALL, Theme.SPACE, 400, 520),
        Level(20, "Galaxy Finale",  12, 17, 178, false, 9, 3, true, Goal.SCORE, 340, P_ALL, Theme.SPACE, 440, 560)
    )

    fun get(num: Int): Level {
        if (num in 1..COUNT) return table[num - 1]
        return endless(num)
    }

    /** Level 21 and beyond: never ends, gets a little spicier each round. */
    private fun endless(num: Int): Level {
        val n = num - COUNT
        val step = (190 - n * 4).coerceAtLeast(120)
        val layout = ((n * 3) % 10)
        val movers = (1 + n / 3).coerceAtMost(4)
        return Level(
            num, "Endless " + n, 12, 17, step, false, layout, movers, n >= 2,
            Goal.SCORE, 200 + n * 60, P_ALL, (n - 1) % 4, 0, 0
        )
    }

    /** Static blocks for a layout id. Cells are packed as row * cols + col. */
    fun obstacles(layout: Int, cols: Int, rows: Int): HashSet<Int> {
        val s = HashSet<Int>()
        fun add(c: Int, r: Int) {
            if (c in 0 until cols && r in 0 until rows) s.add(r * cols + c)
        }

        val cx = cols / 2
        val cy = rows / 2
        when (layout) {
            1 -> { // four corner clusters
                val m = 2
                for (d in 0..1) {
                    add(m + d, m); add(m, m + d)
                    add(cols - 1 - m - d, m); add(cols - 1 - m, m + d)
                    add(m + d, rows - 1 - m); add(m, rows - 1 - m - d)
                    add(cols - 1 - m - d, rows - 1 - m); add(cols - 1 - m, rows - 1 - m - d)
                }
            }
            2 -> { // big plus in the middle
                for (d in -2..2) { add(cx + d, cy - 3); add(cx, cy - 3 + d) }
                for (d in -2..2) { add(cx + d, cy + 3); add(cx, cy + 3 + d) }
            }
            3 -> { // two vertical gates, open in the middle and at both ends
                for (r in 0 until rows) {
                    val middleGap = r >= rows / 2 - 2 && r <= rows / 2 + 2
                    val endGap = r <= 2 || r >= rows - 3
                    if (!middleGap && !endGap) { add(3, r); add(cols - 4, r) }
                }
            }
            4 -> { // pillar grid near the edges
                var r = 2
                while (r < rows - 2) {
                    add(2, r); add(cols - 3, r)
                    r += 3
                }
                var c = 2
                while (c < cols - 2) {
                    add(c, 2); add(c, rows - 3)
                    c += 3
                }
            }
            5 -> { // ring
                val rad = 3
                for (d in -rad..rad) {
                    add(cx + d, cy - rad); add(cx + d, cy + rad)
                    add(cx - rad, cy + d); add(cx + rad, cy + d)
                }
                // doorways so it is never a trap
                s.remove((cy - rad) * cols + cx)
                s.remove((cy + rad) * cols + cx)
                s.remove(cy * cols + (cx - rad))
                s.remove(cy * cols + (cx + rad))
            }
            6 -> { // zigzag shelves
                var r = 3
                var left = true
                while (r < rows - 2) {
                    val from = if (left) 1 else cols - 6
                    for (d in 0..4) add(from + d, r)
                    left = !left
                    r += 3
                }
            }
            7 -> { // diamond with a doorway on each side
                val rad = 4
                for (d in 0..rad) {
                    add(cx - rad + d, cy - d); add(cx + rad - d, cy - d)
                    add(cx - rad + d, cy + d); add(cx + rad - d, cy + d)
                }
                s.remove(cy * cols + (cx - rad))
                s.remove(cy * cols + (cx + rad))
                s.remove((cy - rad) * cols + cx)
                s.remove((cy + rad) * cols + cx)
            }
            8 -> { // dotted grid
                var r = 2
                while (r < rows - 2) {
                    var c = 2
                    while (c < cols - 1) {
                        add(c, r)
                        c += 3
                    }
                    r += 3
                }
            }
            9 -> { // side tunnels
                for (r in 2 until rows - 2) {
                    if (r % 5 != 0) { add(2, r); add(cols - 3, r) }
                }
                for (c in 4 until cols - 4) {
                    if (c % 4 != 0) add(c, rows / 2)
                }
            }
        }

        // Never block the snake's starting lane - it always gets a clear run-up.
        val startRow = rows / 2
        val safeTo = cols / 2 + 2
        val iter = s.iterator()
        while (iter.hasNext()) {
            val cell = iter.next()
            val r = cell / cols
            val c = cell % cols
            if (r == startRow && c <= safeTo) iter.remove()
        }
        return s
    }
}
