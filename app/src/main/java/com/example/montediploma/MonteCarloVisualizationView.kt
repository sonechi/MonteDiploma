package com.example.montediploma

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class MonteCarloVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val insidePointPaint = Paint().apply {
        color = Color.rgb(0, 128, 255) // Голубой
        style = Paint.Style.FILL
    }

    private val outsidePointPaint = Paint().apply {
        color = Color.rgb(200, 200, 200) // Светло-серый
        style = Paint.Style.FILL
    }

    private val shapePaint = Paint().apply {
        color = Color.rgb(0, 128, 255) // Голубой
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val boundaryPaint = Paint().apply {
        color = Color.rgb(100, 100, 100) // Темно-серый
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var shapeType = 0
    private var param1 = 0.0
    private var param2 = 0.0
    private val points = mutableListOf<Point>()
    private val totalPoints = 500 // Количество точек для визуализации

    data class Point(val x: Float, val y: Float, val inside: Boolean)

    fun setShape(shapeType: Int, param1: Double, param2: Double) {
        this.shapeType = shapeType
        this.param1 = param1
        this.param2 = param2
        generatePoints()
    }

    fun reset() {
        points.clear()
        invalidate()
    }

    private fun generatePoints() {
        points.clear()

        when (shapeType) {
            0 -> generateCirclePoints()
            1 -> generateRectanglePoints()
            2 -> generateTrianglePoints()
            3 -> generateSquarePoints()
            4 -> generateEllipsePoints()
            5 -> generateHexagonPoints()
        }
    }

    private fun generateCirclePoints() {
        val radius = param1.toFloat()

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * 2 * radius - radius
            val y = Random.nextFloat() * 2 * radius - radius
            val inside = x * x + y * y <= radius * radius
            points.add(Point(x, y, inside))
        }
    }

    private fun generateRectanglePoints() {
        val width = param1.toFloat()
        val height = param2.toFloat()

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val inside = x >= 0 && x <= width && y >= 0 && y <= height
            points.add(Point(x, y, inside))
        }
    }

    private fun generateTrianglePoints() {
        val base = param1.toFloat()
        val height = param2.toFloat()

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * base
            val y = Random.nextFloat() * height
            val inside = y <= height * (1 - x / base)
            points.add(Point(x, y, inside))
        }
    }

    private fun generateSquarePoints() {
        val side = param1.toFloat()

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * side
            val y = Random.nextFloat() * side
            val inside = x >= 0 && x <= side && y >= 0 && y <= side
            points.add(Point(x, y, inside))
        }
    }

    private fun generateEllipsePoints() {
        val a = param1.toFloat() // Большая полуось
        val b = param2.toFloat() // Малая полуось

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * 2 * a - a
            val y = Random.nextFloat() * 2 * b - b
            val inside = (x * x) / (a * a) + (y * y) / (b * b) <= 1
            points.add(Point(x, y, inside))
        }
    }

    private fun generateHexagonPoints() {
        val side = param1.toFloat()
        val height = (side * sqrt(3f)).toFloat()
        val width = 2 * side

        // Смещение центра шестиугольника
        val centerX = 0f
        val centerY = 0f

        for (i in 0 until totalPoints) {
            val x = Random.nextFloat() * 2 * width - width
            val y = Random.nextFloat() * 2 * height - height

            val inside = isInsideHexagon(x, y, centerX, centerY, side)
            points.add(Point(x, y, inside))
        }
    }

    private fun isInsideHexagon(x: Float, y: Float, centerX: Float, centerY: Float, side: Float): Boolean {
        // Смещаем точку относительно центра
        val dx = abs(x - centerX)
        val dy = abs(y - centerY)

        // Высота от центра до вершины
        val r = side

        // Высота от центра до середины стороны
        val h = side * sqrt(3f) / 2

        // Проверяем, находится ли точка внутри шестиугольника
        return (dx <= side) && (dy <= h) && (side * h - side / 2 * dy - h * dx >= 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val padding = 40f

        val drawWidth = viewWidth - 2 * padding
        val drawHeight = viewHeight - 2 * padding

        // Масштабируем в зависимости от формы
        val scale: Float
        val offsetX: Float
        val offsetY: Float

        when (shapeType) {
            0 -> { // Круг
                val radius = param1.toFloat()
                scale = min(drawWidth, drawHeight) / (2 * radius * 1.1f)
                offsetX = viewWidth / 2
                offsetY = viewHeight / 2

                // Рисуем границу (квадрат)
                canvas.drawRect(
                    offsetX - radius * scale,
                    offsetY - radius * scale,
                    offsetX + radius * scale,
                    offsetY + radius * scale,
                    boundaryPaint
                )

                // Рисуем круг
                canvas.drawCircle(offsetX, offsetY, radius * scale, shapePaint)
            }
            1 -> { // Прямоугольник
                val width = param1.toFloat()
                val height = param2.toFloat()
                scale = min(drawWidth / width, drawHeight / height) * 0.9f
                offsetX = padding + (drawWidth - width * scale) / 2
                offsetY = padding + (drawHeight - height * scale) / 2

                // Для прямоугольника граница и форма совпадают
                canvas.drawRect(
                    offsetX,
                    offsetY,
                    offsetX + width * scale,
                    offsetY + height * scale,
                    shapePaint
                )
            }
            2 -> { // Треугольник
                val base = param1.toFloat()
                val height = param2.toFloat()
                scale = min(drawWidth / base, drawHeight / height) * 0.9f
                offsetX = padding + (drawWidth - base * scale) / 2
                offsetY = padding + (drawHeight - height * scale) / 2

                // Рисуем границу (прямоугольник)
                canvas.drawRect(
                    offsetX,
                    offsetY,
                    offsetX + base * scale,
                    offsetY + height * scale,
                    boundaryPaint
                )

                // Рисуем треугольник
                val path = Path()
                path.moveTo(offsetX, offsetY + height * scale)
                path.lineTo(offsetX + base * scale, offsetY + height * scale)
                path.lineTo(offsetX, offsetY)
                path.close()
                canvas.drawPath(path, shapePaint)
            }
            3 -> { // Квадрат
                val side = param1.toFloat()
                scale = min(drawWidth / side, drawHeight / side) * 0.9f
                offsetX = padding + (drawWidth - side * scale) / 2
                offsetY = padding + (drawHeight - side * scale) / 2

                // Для квадрата граница и форма совпадают
                canvas.drawRect(
                    offsetX,
                    offsetY,
                    offsetX + side * scale,
                    offsetY + side * scale,
                    shapePaint
                )
            }
            4 -> { // Эллипс
                val a = param1.toFloat() // Большая полуось
                val b = param2.toFloat() // Малая полуось
                scale = min(drawWidth / (2 * a), drawHeight / (2 * b)) * 0.9f
                offsetX = viewWidth / 2
                offsetY = viewHeight / 2

                // Рисуем границу (прямоугольник)
                canvas.drawRect(
                    offsetX - a * scale,
                    offsetY - b * scale,
                    offsetX + a * scale,
                    offsetY + b * scale,
                    boundaryPaint
                )

                // Рисуем эллипс
                val rectF = android.graphics.RectF(
                    offsetX - a * scale,
                    offsetY - b * scale,
                    offsetX + a * scale,
                    offsetY + b * scale
                )
                canvas.drawOval(rectF, shapePaint)
            }
            5 -> { // Шестиугольник
                val side = param1.toFloat()
                val hexHeight = side * sqrt(3f)
                val hexWidth = 2 * side
                scale = min(drawWidth / hexWidth, drawHeight / hexHeight) * 0.9f
                offsetX = viewWidth / 2
                offsetY = viewHeight / 2

                // Рисуем границу (прямоугольник)
                canvas.drawRect(
                    offsetX - hexWidth / 2 * scale,
                    offsetY - hexHeight / 2 * scale,
                    offsetX + hexWidth / 2 * scale,
                    offsetY + hexHeight / 2 * scale,
                    boundaryPaint
                )

                // Рисуем шестиугольник
                val path = Path()
                for (i in 0 until 6) {
                    val angle = (60 * i - 30) * (Math.PI / 180)
                    val px = offsetX + side * scale * cos(angle).toFloat()
                    val py = offsetY + side * scale * sin(angle).toFloat()

                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }
                path.close()
                canvas.drawPath(path, shapePaint)
            }
            else -> return
        }

        // Рисуем точки
        val pointRadius = 4f
        for (point in points) {
            val px: Float
            val py: Float

            when (shapeType) {
                0 -> { // Круг
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                1 -> { // Прямоугольник
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                2 -> { // Треугольник
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                3 -> { // Квадрат
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                4 -> { // Эллипс
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                5 -> { // Шестиугольник
                    px = offsetX + point.x * scale
                    py = offsetY + point.y * scale
                }
                else -> continue
            }

            canvas.drawCircle(
                px,
                py,
                pointRadius,
                if (point.inside) insidePointPaint else outsidePointPaint
            )
        }
    }
}