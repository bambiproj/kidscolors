package com.bambiproj.monstersnake

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets

/** Screen-space globals shared by every screen. */
object G {
    var w = 0f
    var h = 0f
    var safeTop = 0f
    var safeBottom = 0f
    var s = 1f            // design scale: 1.0 at 1080px wide
    var time = 0f

    fun dp(v: Float) = v * s

    /** Playfield top / bottom, leaving room for notches and gesture bars. */
    val top: Float get() = safeTop
    val bottom: Float get() = h - safeBottom
}

abstract class Screen {
    open fun layout() {}
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)
    open fun down(x: Float, y: Float) {}
    open fun move(x: Float, y: Float) {}
    open fun up(x: Float, y: Float) {}
    /** Return true when the screen consumed the back press. */
    open fun back(): Boolean = false
    open fun onShow() {}
    open fun onHide() {}
}

object Nav {
    var current: Screen? = null
    private var next: Screen? = null
    var cover = 0f          // 0 = clear, 1 = fully covered
    private var covering = false

    fun reset(s: Screen) {
        current?.onHide()
        current = s
        s.layout()
        s.onShow()
        cover = 0f
        covering = false
        next = null
    }

    fun go(s: Screen) {
        if (next != null) return
        next = s
        covering = true
    }

    fun update(dt: Float) {
        if (covering) {
            cover += dt * 5.5f
            if (cover >= 1f) {
                cover = 1f
                covering = false
                val n = next
                next = null
                if (n != null) {
                    current?.onHide()
                    current = n
                    n.layout()
                    n.onShow()
                }
            }
        } else if (cover > 0f) {
            cover -= dt * 5.5f
            if (cover < 0f) cover = 0f
        }
        current?.update(dt)
    }

    val busy: Boolean get() = next != null || cover > 0.02f
}

class GameView(ctx: Context) : View(ctx), Choreographer.FrameCallback {

    private var lastNs = 0L
    private var running = false

    init {
        isFocusable = true
        setWillNotDraw(false)
    }

    fun start() {
        if (running) return
        running = true
        lastNs = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastNs == 0L) lastNs = frameTimeNanos
        var dt = (frameTimeNanos - lastNs) / 1_000_000_000f
        lastNs = frameTimeNanos
        if (dt > 0.05f) dt = 0.05f      // never let a hitch teleport the snake
        if (dt < 0f) dt = 0f
        G.time += dt
        Nav.update(dt)
        Fx.update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        G.w = w.toFloat()
        G.h = h.toFloat()
        G.s = G.w / 1080f
        Nav.current?.layout()
    }

    @Suppress("DEPRECATION")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        var t = 0
        var b = 0
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            t = i.top
            b = i.bottom
        } else {
            t = insets.systemWindowInsetTop
            b = insets.systemWindowInsetBottom
        }
        G.safeTop = t.toFloat()
        G.safeBottom = b.toFloat()
        Nav.current?.layout()
        return super.onApplyWindowInsets(insets)
    }

    override fun onDraw(canvas: Canvas) {
        val s = Nav.current ?: return
        s.draw(canvas)
        Fx.draw(canvas)
        if (Nav.cover > 0.001f) {
            Ui.dim(canvas, G.w, G.h, (Nav.cover * 255).toInt().coerceIn(0, 255))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = Nav.current ?: return true
        if (Nav.busy) return true
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> s.down(x, y)
            MotionEvent.ACTION_MOVE -> s.move(x, y)
            MotionEvent.ACTION_UP -> s.up(x, y)
            MotionEvent.ACTION_CANCEL -> s.up(-1f, -1f)
        }
        return true
    }
}
