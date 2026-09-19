package com.bambiproj.monstersnake

import android.content.Context
import android.content.SharedPreferences

/** Local save file. Everything the child earns lives here - no network, no accounts. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("monster_snake", Context.MODE_PRIVATE)
    }

    private fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    private fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()

    var coins: Int
        get() = sp.getInt("coins", 0)
        set(v) = putInt("coins", if (v < 0) 0 else v)

    var musicOn: Boolean
        get() = sp.getBoolean("music", true)
        set(v) = putBool("music", v)

    var soundOn: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = putBool("sound", v)

    /** Big arrow buttons under the board (on by default - easiest for small hands). */
    var padOn: Boolean
        get() = sp.getBoolean("pad", true)
        set(v) = putBool("pad", v)

    var skin: Int
        get() = sp.getInt("skin", 0)
        set(v) = putInt("skin", v)

    /** Highest level the player may enter (1..20, 21 = endless). */
    var unlocked: Int
        get() = sp.getInt("unlocked", 1)
        set(v) = putInt("unlocked", v)

    var endlessBest: Int
        get() = sp.getInt("endless_best", 0)
        set(v) = putInt("endless_best", v)

    fun stars(level: Int): Int = sp.getInt("stars_$level", 0)

    fun setStars(level: Int, s: Int) {
        if (s > stars(level)) putInt("stars_$level", s)
    }

    fun totalStars(): Int {
        var t = 0
        for (i in 1..Levels.COUNT) t += stars(i)
        return t
    }

    fun ownsSkin(id: Int): Boolean = id == 0 || sp.getBoolean("skin_$id", false)

    fun unlockSkin(id: Int) = putBool("skin_$id", true)

    /** Creature "album": which food creatures have been met at least once. */
    fun metCreature(id: Int): Boolean = sp.getBoolean("met_$id", false)

    fun meetCreature(id: Int): Boolean {
        if (metCreature(id)) return false
        putBool("met_$id", true)
        return true
    }

    fun unlockLevel(level: Int) {
        if (level > unlocked) unlocked = if (level > Levels.COUNT + 1) Levels.COUNT + 1 else level
    }
}
