package com.bambiproj.monstersnake

import android.content.Context
import android.content.SharedPreferences

/**
 * Local save file. Everything the child earns lives here - no network, no
 * accounts. Falls back to an in-memory store when there is no Context
 * (headless tests), so game code never has to care.
 */
object Prefs {
    private var sp: SharedPreferences? = null
    private val mem = HashMap<String, Any>()

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("monster_snake", Context.MODE_PRIVATE)
    }

    private fun getInt(k: String, d: Int): Int {
        val s = sp ?: return (mem[k] as? Int) ?: d
        return s.getInt(k, d)
    }

    private fun putInt(k: String, v: Int) {
        val s = sp
        if (s == null) mem[k] = v else s.edit().putInt(k, v).apply()
    }

    private fun getBool(k: String, d: Boolean): Boolean {
        val s = sp ?: return (mem[k] as? Boolean) ?: d
        return s.getBoolean(k, d)
    }

    private fun putBool(k: String, v: Boolean) {
        val s = sp
        if (s == null) mem[k] = v else s.edit().putBoolean(k, v).apply()
    }

    var coins: Int
        get() = getInt("coins", 0)
        set(v) = putInt("coins", if (v < 0) 0 else v)

    var musicOn: Boolean
        get() = getBool("music", true)
        set(v) = putBool("music", v)

    var soundOn: Boolean
        get() = getBool("sound", true)
        set(v) = putBool("sound", v)

    /** Big arrow buttons under the board (on by default - easiest for small hands). */
    var padOn: Boolean
        get() = getBool("pad", true)
        set(v) = putBool("pad", v)

    var skin: Int
        get() = getInt("skin", 0)
        set(v) = putInt("skin", v)

    /** Highest level the player may enter (1..20, 21 = endless). */
    var unlocked: Int
        get() = getInt("unlocked", 1)
        set(v) = putInt("unlocked", v)

    var endlessBest: Int
        get() = getInt("endless_best", 0)
        set(v) = putInt("endless_best", v)

    fun stars(level: Int): Int = getInt("stars_$level", 0)

    fun setStars(level: Int, s: Int) {
        if (s > stars(level)) putInt("stars_$level", s)
    }

    fun totalStars(): Int {
        var t = 0
        for (i in 1..Levels.COUNT) t += stars(i)
        return t
    }

    fun ownsSkin(id: Int): Boolean = id == 0 || getBool("skin_$id", false)

    fun unlockSkin(id: Int) = putBool("skin_$id", true)

    /** Creature album: which monsters have been met at least once. */
    fun metCreature(id: Int): Boolean = getBool("met_$id", false)

    /** Returns true the first time a given monster is caught. */
    fun meetCreature(id: Int): Boolean {
        if (metCreature(id)) return false
        putBool("met_$id", true)
        return true
    }

    fun unlockLevel(level: Int) {
        val capped = if (level > Levels.COUNT + 1) Levels.COUNT + 1 else level
        if (capped > unlocked) unlocked = capped
    }
}
