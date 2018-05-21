package com.example.stacksquareview

/**
 * Created by anweshmishra on 21/05/18.
 */
import android.content.Context
import android.view.View
import android.view.MotionEvent
import android.graphics.*

val SQUARES = 4

class StackSquareView (ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var prevScale : Float = 0f, var dir : Float = 0f, var j : Int = 0) {

        val scales : Array<Float> = arrayOf(0f, 0f)

        fun update(stopcb : (Float) -> Unit) {
            scales[j] += 0.1f * dir
            if (Math.abs(scales[j] - prevScale) > 1) {
                prevScale = scales[j] + dir
                j += dir.toInt()
                if (j == SQUARES && j == -1) {
                    j -= dir.toInt()
                    dir = 0f
                    prevScale = scales[j]
                    stopcb(prevScale)
                }
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                startcb()
            }
        }
    }


    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(updatecb : () -> Unit) {
            if (animated) {
                updatecb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch (ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SSNode(var i : Int) {

        private var next : SSNode? = null

        var prev : SSNode? = null

        private val state : State = State()

        init {
            this.addNeighbor()
        }

        fun addNeighbor() {
            if (i < SQUARES - 1) {
                next = SSNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            val w : Float = canvas.width.toFloat()
            val h : Float = canvas.height.toFloat()
            val gap : Float = (w / (SQUARES * 3))
            paint.color = Color.WHITE
            val dx : Float = i * gap
            val sx : Float = w - gap
            val x : Float = sx + (dx - sx) * state.scales[1]
            val h2 : Float = (gap / 2) * state.scales[0]
            canvas.save()
            canvas.translate(x + gap/2, h/2)
            canvas.drawRect(RectF(-gap/2, -h2, gap/2, h2), paint)
            canvas.restore()
        }

        fun update(stopcb : (Float) -> Unit) {
            state.update(stopcb)
        }

        fun startUpdating(startcb : () -> Unit) {
            state.startUpdating(startcb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SSNode {
            var curr : SSNode? = this.prev
            if (dir == 1) {
                curr = this.next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class StackSquare (var i : Int) {

        var curr : SSNode = SSNode(0)

        var dir : Int = 1

        fun update(stopcb : (Float) -> Unit) {
            curr.update {
                curr = this.curr.getNext(dir) {
                    dir *= -1
                }
                stopcb(it)
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            curr?.prev?.draw(canvas, paint)
            curr.draw(canvas, paint)
        }

        fun startUpdating(startcb : () -> Unit) {
            curr.startUpdating(startcb)
        }
    }

    data class Renderer (var view : StackSquareView) {

        private val animator : Animator = Animator(view)

        private val stackSquare : StackSquare = StackSquare(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#212121"))
            stackSquare.draw(canvas, paint)
            animator.animate {
                stackSquare.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            stackSquare.startUpdating {
                animator.start()
            }
        }
    }
}
