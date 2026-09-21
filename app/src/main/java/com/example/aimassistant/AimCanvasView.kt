package com.example.aimassistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AimCanvasView(context: Context) : View(context) {

    // Draggable control nodes
    private var cueBall = PointF(300f, 800f)
    private var targetBall = PointF(600f, 600f)
    private var cushionPoint = PointF(800f, 200f)

    private var activeNode: PointF? = null
    private val touchRadius = 70f

    // Visual Paints
    private val cueLinePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val targetLinePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val reflectionLinePaint = Paint().apply {
        color = Color.MAGENTA
        strokeWidth = 5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val nodeBorderPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw trajectory from Cue Ball to Target Ball
        canvas.drawLine(cueBall.x, cueBall.y, targetBall.x, targetBall.y, cueLinePaint)

        // 2. Vector math for Target Ball deflection / pocket direction
        val dx = targetBall.x - cueBall.x
        val dy = targetBall.y - cueBall.y
        val angle = atan2(dy.toDouble(), dx.toDouble())

        val lineLength = 500f
        val targetNextX = targetBall.x + (lineLength * cos(angle)).toFloat()
        val targetNextY = targetBall.y + (lineLength * sin(angle)).toFloat()

        // Line representing post-collision path
        canvas.drawLine(targetBall.x, targetBall.y, targetNextX, targetNextY, targetLinePaint)

        // 3. Cushion Reflection Line (Vector Physics: Angle_in = Angle_out)
        canvas.drawLine(targetBall.x, targetBall.y, cushionPoint.x, cushionPoint.y, cueLinePaint)
        
        val reflectedPoint = calculateReflectionPoint(targetBall, cushionPoint, lineLength)
        canvas.drawLine(cushionPoint.x, cushionPoint.y, reflectedPoint.x, reflectedPoint.y, reflectionLinePaint)

        // 4. Draw interactive handle indicators
        drawHandle(canvas, cueBall, "Cue")
        drawHandle(canvas, targetBall, "Target")
        drawHandle(canvas, cushionPoint, "Cushion")
    }

    /**
     * Physics Reflection Vector formula: R = D - 2 * (D . N) * N
     * Implements Angle_in = Angle_out relative to normal vector (Assumes horizontal cushion boundary).
     */
    private fun calculateReflectionPoint(start: PointF, impact: PointF, length: Float): PointF {
        val dx = impact.x - start.x
        val dy = impact.y - start.y

        // Incident vector
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0f) return impact
        
        val ix = dx / len
        val iy = dy / len

        // Surface normal for horizontal cushion rail (N = (0, -1))
        val nx = 0f
        val ny = -1f

        // Dot product (I . N)
        val dot = ix * nx + iy * ny

        // Reflected direction R = I - 2*(I.N)*N
        val rx = ix - 2 * dot * nx
        val ry = iy - 2 * dot * ny

        return PointF(impact.x + rx * length, impact.y + ry * length)
    }

    private fun drawHandle(canvas: Canvas, point: PointF, label: String) {
        canvas.drawCircle(point.x, point.y, 25f, handlePaint)
        canvas.drawCircle(point.x, point.y, 25f, nodeBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeNode = when {
                    isTouchInside(x, y, cueBall) -> cueBall
                    isTouchInside(x, y, targetBall) -> targetBall
                    isTouchInside(x, y, cushionPoint) -> cushionPoint
                    else -> null
                }
            }
            MotionEvent.ACTION_MOVE -> {
                activeNode?.let { node ->
                    node.x = x
                    node.y = y
                    invalidate() // Trigger canvas redraw
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeNode = null
            }
        }
        return true
    }

    private fun isTouchInside(touchX: Float, touchY: Float, node: PointF): Boolean {
        val dx = touchX - node.x
        val dy = touchY - node.y
        return sqrt(dx * dx + dy * dy) <= touchRadius
    }
}
