package com.example.montediploma

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itextpdf.text.Document
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class PrecisionAnalysisActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var analysisTitle: TextView
    private lateinit var descriptionText: TextView
    private lateinit var exportButton: Button
    private var shapeType = 0
    private var param1 = 0.0
    private var param2 = 0.0
    private val analysisResults = mutableListOf<AnalysisResult>()

    data class AnalysisResult(
        val pointCount: Int,
        val area: Double,
        val exactArea: Double,
        val errorPercentage: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_precision_analysis)

        // Инициализация компонентов
        lineChart = findViewById(R.id.convergenceChart)
        analysisTitle = findViewById(R.id.analysisTitle)
        descriptionText = findViewById(R.id.analysisDescription)
        exportButton = findViewById(R.id.exportButton)

        // Получаем параметры из Intent
        shapeType = intent.getIntExtra("shapeType", 0)
        param1 = intent.getDoubleExtra("param1", 0.0)
        param2 = intent.getDoubleExtra("param2", 0.0)

        // Устанавливаем заголовок в зависимости от фигуры
        analysisTitle.text = getString(R.string.precision_analysis_title, getShapeName())

        // Настройка графика
        setupChart()

        // Запуск анализа
        performAnalysis()

        // Настройка кнопки экспорта
        exportButton.setOnClickListener {
            exportResultsToPdf()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.precision_analysis)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Обрабатываем нажатие на кнопку "Назад" в ActionBar
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getShapeName(): String {
        val shapesArray = resources.getStringArray(R.array.shapes_array)
        return if (shapeType < shapesArray.size) shapesArray[shapeType] else ""
    }

    private fun getParametersDescription(): String {
        return when (shapeType) {
            0 -> "Радиус = $param1"
            1 -> "Ширина = $param1, Высота = $param2"
            2 -> "Основание = $param1, Высота = $param2"
            3 -> "Сторона = $param1"
            4 -> "Большая полуось = $param1, Малая полуось = $param2"
            5 -> "Сторона = $param1"
            else -> ""
        }
    }

    private fun setupChart() {
        // Настройка оформления графика
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 0f

        // Преобразуем значения оси X в более читаемый формат (количество точек)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when {
                    value >= 1000000 -> String.format("%.1fM", value / 1000000)
                    value >= 1000 -> String.format("%.1fK", value / 1000)
                    else -> value.toInt().toString()
                }
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawZeroLine(true)
        leftAxis.setDrawLimitLinesBehindData(true)

        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = true

        // Дополнительные настройки внешнего вида
        lineChart.animateX(1000)
    }

    private fun performAnalysis() {
        val entries = ArrayList<Entry>()
        val pointsCounts = listOf(100, 500, 1000, 5000, 10000, 50000, 100000)

        // Запуск вычислений в фоновом потоке
        lifecycleScope.launch(Dispatchers.Default) {
            analysisResults.clear()

            for (count in pointsCounts) {
                val result = when (shapeType) {
                    0 -> calculateCircleArea(param1, count)
                    1 -> calculateRectangleArea(param1, param2, count)
                    2 -> calculateTriangleArea(param1, param2, count)
                    3 -> calculateSquareArea(param1, count)
                    4 -> calculateEllipseArea(param1, param2, count)
                    5 -> calculateHexagonArea(param1, count)
                    else -> null
                }

                result?.let {
                    val analysisResult = AnalysisResult(
                        count,
                        it.area,
                        it.exactArea,
                        it.errorPercentage
                    )

                    analysisResults.add(analysisResult)
                    entries.add(Entry(count.toFloat(), it.errorPercentage.toFloat()))
                }

                // Обновление UI
                withContext(Dispatchers.Main) {
                    updateChart(entries)
                }
            }

            // Финальное обновление
            withContext(Dispatchers.Main) {
                descriptionText.text = generateAnalysisDescription()
                exportButton.visibility = View.VISIBLE
            }
        }
    }

    private fun updateChart(entries: List<Entry>) {
        // Сортируем точки по X (количеству точек)
        val sortedEntries = entries.sortedBy { it.x }

        val dataSet = LineDataSet(sortedEntries, getString(R.string.error_chart_label))
        dataSet.color = ContextCompat.getColor(this, R.color.primary_color)
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.accent_color))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawValues(true)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    @SuppressLint("StringFormatMatches")
    private fun generateAnalysisDescription(): String {
        val shapeName = getShapeName().lowercase(Locale.getDefault())
        val exactArea = analysisResults.firstOrNull()?.exactArea ?: 0.0

        val minError = analysisResults.minByOrNull { it.errorPercentage }
        val maxError = analysisResults.maxByOrNull { it.errorPercentage }

        return getString(
            R.string.analysis_description,
            shapeName,  // %1$s - строка
            exactArea,  // %2$.4f - число с плавающей точкой
            minError?.pointCount ?: 0,  // %3$d - целое число
            minError?.errorPercentage ?: 0.0,  // %4$.4f - число с плавающей точкой
            maxError?.pointCount ?: 0,  // %5$d - целое число
            maxError?.errorPercentage ?: 0.0  // %6$.4f - число с плавающей точкой
        )
    }

    private fun exportResultsToPdf() {
        try {
            val fileName = "monte_carlo_analysis_${System.currentTimeMillis()}.pdf"
            val path = getExternalFilesDir(null)?.absolutePath + "/" + fileName

            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(path))
            document.open()

            // Добавление заголовка
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            document.add(Paragraph("Анализ точности метода Монте-Карло", titleFont))

            // Добавление информации о параметрах
            val normalFont = Font(Font.FontFamily.HELVETICA, 12f)
            document.add(Paragraph("Фигура: ${getShapeName()}", normalFont))
            document.add(Paragraph("Параметры: ${getParametersDescription()}", normalFont))

            // Добавление таблицы результатов
            val table = PdfPTable(3)
            table.addCell("Кол-во точек")
            table.addCell("Площадь")
            table.addCell("Погрешность, %")

            // Заполнение таблицы данными
            for (entry in analysisResults) {
                table.addCell(entry.pointCount.toString())
                table.addCell(String.format("%.4f", entry.area))
                table.addCell(String.format("%.2f", entry.errorPercentage))
            }

            document.add(table)

            // Добавление изображения графика
            val bitmap = chartToBitmap()
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            val image = Image.getInstance(byteArray)
            image.scaleToFit(500f, 300f)
            document.add(image)

            document.close()

            Toast.makeText(this, "Результаты сохранены в $path", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun chartToBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            lineChart.width,
            lineChart.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        lineChart.draw(canvas)
        return bitmap
    }

    // Методы расчета для различных фигур (аналогичны методам в CalculatorActivity)
    private fun calculateCircleArea(radius: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        var pointsInside = 0
        val squareSize = radius * 2
        val boundingArea = squareSize * squareSize

        for (i in 0 until numberOfPoints) {
            val x = Random.nextDouble(-radius, radius)
            val y = Random.nextDouble(-radius, radius)

            if (x * x + y * y <= radius * radius) {
                pointsInside++
            }
        }

        val ratio = pointsInside.toDouble() / numberOfPoints
        val area = ratio * boundingArea
        val exactArea = AnalyticalFormulas.getCircleArea(radius)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateRectangleArea(width: Double, height: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        val pointsInside = numberOfPoints  // Все точки внутри
        val boundingArea = width * height
        val ratio = 1.0
        val area = width * height
        val exactArea = AnalyticalFormulas.getRectangleArea(width, height)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateTriangleArea(base: Double, height: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        var pointsInside = 0
        val boundingArea = base * height

        for (i in 0 until numberOfPoints) {
            val x = Random.nextDouble(0.0, base)
            val y = Random.nextDouble(0.0, height)

            // Точка внутри треугольника, если y <= (height/base) * (base - x)
            if (y <= height * (1 - x / base)) {
                pointsInside++
            }
        }

        val ratio = pointsInside.toDouble() / numberOfPoints
        val area = ratio * boundingArea
        val exactArea = AnalyticalFormulas.getTriangleArea(base, height)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateSquareArea(side: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        val pointsInside = numberOfPoints
        val boundingArea = side * side
        val ratio = 1.0
        val area = side * side
        val exactArea = AnalyticalFormulas.getSquareArea(side)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateEllipseArea(semiMajor: Double, semiMinor: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        var pointsInside = 0
        val boundingArea = 4 * semiMajor * semiMinor

        for (i in 0 until numberOfPoints) {
            val x = Random.nextDouble(-semiMajor, semiMajor)
            val y = Random.nextDouble(-semiMinor, semiMinor)

            // Точка внутри эллипса, если (x/a)² + (y/b)² <= 1
            if ((x * x) / (semiMajor * semiMajor) + (y * y) / (semiMinor * semiMinor) <= 1) {
                pointsInside++
            }
        }

        val ratio = pointsInside.toDouble() / numberOfPoints
        val area = ratio * boundingArea
        val exactArea = AnalyticalFormulas.getEllipseArea(semiMajor, semiMinor)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateHexagonArea(side: Double, numberOfPoints: Int): CalculatorActivity.MonteCarloResult {
        var pointsInside = 0
        val height = side * sqrt(3.0)
        val width = 2 * side
        val boundingArea = 2 * width * height
        val centerX = 0.0
        val centerY = 0.0

        for (i in 0 until numberOfPoints) {
            val x = Random.nextDouble(-width, width)
            val y = Random.nextDouble(-height, height)

            if (isInsideHexagon(x, y, centerX, centerY, side)) {
                pointsInside++
            }
        }

        val ratio = pointsInside.toDouble() / numberOfPoints
        val area = ratio * boundingArea
        val exactArea = AnalyticalFormulas.getHexagonArea(side)

        return CalculatorActivity.MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun isInsideHexagon(x: Double, y: Double, centerX: Double, centerY: Double, side: Double): Boolean {
        val dx = abs(x - centerX)
        val dy = abs(y - centerY)
        val r = side
        val h = side * sqrt(3.0) / 2
        return (dx <= side / 2 * 2) && (dy <= h) && (side * h - side / 2 * dy - h * dx >= 0)
    }

}