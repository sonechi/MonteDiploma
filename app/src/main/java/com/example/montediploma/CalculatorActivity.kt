package com.example.montediploma

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlin.math.*
import kotlin.random.Random

class CalculatorActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var inputLayout1: LinearLayout
    private lateinit var inputLayout2: LinearLayout
    private lateinit var parameter1Label: TextView
    private lateinit var parameter2Label: TextView
    private lateinit var parameter1Input: EditText
    private lateinit var parameter2Input: EditText
    private lateinit var calculateButton: Button
    private lateinit var resetButton: Button
    private lateinit var resultText: TextView
    private lateinit var calculationsCard: CardView
    private lateinit var pointsInsideText: TextView
    private lateinit var pointsTotalText: TextView
    private lateinit var ratioText: TextView
    private lateinit var boundingAreaText: TextView
    private lateinit var exactAreaText: TextView
    private lateinit var errorText: TextView
    private lateinit var visualizationView: MonteCarloVisualizationView
    private lateinit var numberPointsSpinner: Spinner
    private lateinit var analysisButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        // Инициализация компонентов
        spinner = findViewById(R.id.shapeSpinner)
        inputLayout1 = findViewById(R.id.inputLayout1)
        inputLayout2 = findViewById(R.id.inputLayout2)
        parameter1Label = findViewById(R.id.parameter1Label)
        parameter2Label = findViewById(R.id.parameter2Label)
        parameter1Input = findViewById(R.id.parameter1Input)
        parameter2Input = findViewById(R.id.parameter2Input)
        calculateButton = findViewById(R.id.calculateButton)
        resetButton = findViewById(R.id.resetButton)
        resultText = findViewById(R.id.resultText)
        calculationsCard = findViewById(R.id.calculationsCard)
        pointsInsideText = findViewById(R.id.pointsInsideText)
        pointsTotalText = findViewById(R.id.pointsTotalText)
        ratioText = findViewById(R.id.ratioText)
        boundingAreaText = findViewById(R.id.boundingAreaText)
        exactAreaText = findViewById(R.id.exactAreaText)
        errorText = findViewById(R.id.errorText)
        visualizationView = findViewById(R.id.visualizationView)
        numberPointsSpinner = findViewById(R.id.numberPointsSpinner)
        analysisButton = findViewById(R.id.analysisButton)

        // Настройка выпадающего списка для выбора фигуры
        ArrayAdapter.createFromResource(
            this,
            R.array.shapes_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Настройка выпадающего списка для количества точек
        ArrayAdapter.createFromResource(
            this,
            R.array.points_count_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberPointsSpinner.adapter = adapter
        }

        // Обработчик выбора формы
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                updateInputFields(pos)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делаем
            }
        }

        // Обработчик кнопки расчета
        calculateButton.setOnClickListener {
            calculateArea()
        }

        // Обработчик кнопки сброса
        resetButton.setOnClickListener {
            resetCalculations()
        }

        // Обработчик кнопки анализа точности
        analysisButton.setOnClickListener {
            if (parameter1Input.text.toString().isEmpty()) {
                Toast.makeText(this, R.string.invalid_input_message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val shapeType = spinner.selectedItemPosition
                val param1 = parameter1Input.text.toString().toDouble()
                var param2 = 0.0

                if (inputLayout2.visibility == View.VISIBLE && parameter2Input.text.toString().isNotEmpty()) {
                    param2 = parameter2Input.text.toString().toDouble()
                }

                if (param1 <= 0 || (inputLayout2.visibility == View.VISIBLE && param2 <= 0)) {
                    Toast.makeText(this, R.string.positive_values_message, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(this, PrecisionAnalysisActivity::class.java).apply {
                    putExtra("shapeType", shapeType)
                    putExtra("param1", param1)
                    putExtra("param2", param2)
                }
                startActivity(intent)

            } catch (e: NumberFormatException) {
                Toast.makeText(this, R.string.invalid_input_message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateInputFields(shapePosition: Int) {
        when (shapePosition) {
            0 -> { // Круг
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.GONE
                parameter1Label.text = getString(R.string.radius_label)
            }
            1 -> { // Прямоугольник
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.VISIBLE
                parameter1Label.text = getString(R.string.width_label)
                parameter2Label.text = getString(R.string.height_label)
            }
            2 -> { // Треугольник
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.VISIBLE
                parameter1Label.text = getString(R.string.base_label)
                parameter2Label.text = getString(R.string.height_label)
            }
            3 -> { // Квадрат
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.GONE
                parameter1Label.text = getString(R.string.side_label)
            }
            4 -> { // Эллипс
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.VISIBLE
                parameter1Label.text = getString(R.string.semi_major_label)
                parameter2Label.text = getString(R.string.semi_minor_label)
            }
            5 -> { // Шестиугольник
                inputLayout1.visibility = View.VISIBLE
                inputLayout2.visibility = View.GONE
                parameter1Label.text = getString(R.string.side_label)
            }
        }
    }

    private fun calculateArea() {
        try {
            val shapePosition = spinner.selectedItemPosition
            val pointsCountPosition = numberPointsSpinner.selectedItemPosition
            val numberOfPoints = when (pointsCountPosition) {
                0 -> 1000
                1 -> 5000
                2 -> 10000
                3 -> 50000
                4 -> 100000
                5 -> 1000000
                else -> 10000
            }

            val param1 = parameter1Input.text.toString().toDouble()
            var param2 = 0.0
            if (inputLayout2.visibility == View.VISIBLE) {
                param2 = parameter2Input.text.toString().toDouble()
            }

            if (param1 <= 0 || (inputLayout2.visibility == View.VISIBLE && param2 <= 0)) {
                Toast.makeText(this, R.string.positive_values_message, Toast.LENGTH_SHORT).show()
                return
            }

            // Вызов соответствующей функции расчета
            val result = when (shapePosition) {
                0 -> calculateCircleArea(param1, numberOfPoints)
                1 -> calculateRectangleArea(param1, param2, numberOfPoints)
                2 -> calculateTriangleArea(param1, param2, numberOfPoints)
                3 -> calculateSquareArea(param1, numberOfPoints)
                4 -> calculateEllipseArea(param1, param2, numberOfPoints)
                5 -> calculateHexagonArea(param1, numberOfPoints)
                else -> MonteCarloResult(0.0, numberOfPoints, 0, 0.0)
            }

            // Отображаем результат
            resultText.text = getString(R.string.result_area, result.area)

            // Отображаем детали вычислений
            calculationsCard.visibility = View.VISIBLE
            pointsInsideText.text = getString(R.string.points_inside, result.pointsInside)
            pointsTotalText.text = getString(R.string.points_total, result.totalPoints)
            ratioText.text = getString(R.string.ratio, result.ratio)
            boundingAreaText.text = getString(R.string.bounding_area, result.boundingArea)
            exactAreaText.text = getString(R.string.exact_area, result.exactArea)
            errorText.text = getString(R.string.error_percentage, result.errorPercentage)

            // Обновляем визуализацию
            updateVisualization(shapePosition, param1, param2)

            // Показываем кнопку анализа точности
            analysisButton.visibility = View.VISIBLE

        } catch (e: NumberFormatException) {
            Toast.makeText(this, R.string.invalid_input_message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetCalculations() {
        // Сбрасываем поля ввода
        parameter1Input.setText("")
        parameter2Input.setText("")

        // Сбрасываем результаты
        resultText.text = getString(R.string.result_initial)
        calculationsCard.visibility = View.GONE
        analysisButton.visibility = View.GONE

        // Сбрасываем визуализацию
        visualizationView.reset()
    }

    data class MonteCarloResult(
        val area: Double,
        val totalPoints: Int,
        val pointsInside: Int,
        val boundingArea: Double,
        val ratio: Double = pointsInside.toDouble() / totalPoints,
        val exactArea: Double = 0.0,
        val errorPercentage: Double = if (exactArea > 0) abs((area - exactArea) / exactArea * 100) else 0.0
    )

    private fun calculateCircleArea(radius: Double, numberOfPoints: Int): MonteCarloResult {
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

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateRectangleArea(width: Double, height: Double, numberOfPoints: Int): MonteCarloResult {
        // Для прямоугольника метод Монте-Карло избыточен, но реализуем для учебных целей
        val pointsInside = numberOfPoints  // Все точки внутри
        val boundingArea = width * height
        val ratio = 1.0
        val area = width * height
        val exactArea = AnalyticalFormulas.getRectangleArea(width, height)

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateTriangleArea(base: Double, height: Double, numberOfPoints: Int): MonteCarloResult {
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

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateSquareArea(side: Double, numberOfPoints: Int): MonteCarloResult {
        // Как и для прямоугольника, метод Монте-Карло избыточен
        val pointsInside = numberOfPoints
        val boundingArea = side * side
        val ratio = 1.0
        val area = side * side
        val exactArea = AnalyticalFormulas.getSquareArea(side)

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateEllipseArea(semiMajor: Double, semiMinor: Double, numberOfPoints: Int): MonteCarloResult {
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

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun calculateHexagonArea(side: Double, numberOfPoints: Int): MonteCarloResult {
        var pointsInside = 0
        // Высота правильного шестиугольника = side * sqrt(3)
        val height = side * sqrt(3.0)
        // Ширина шестиугольника = 2 * side
        val width = 2 * side

        // Ограничивающий прямоугольник немного больше шестиугольника
        val boundingArea = 2 * width * height

        // Центр шестиугольника
        val centerX = 0.0
        val centerY = 0.0

        for (i in 0 until numberOfPoints) {
            val x = Random.nextDouble(-width, width)
            val y = Random.nextDouble(-height, height)

            // Проверка, находится ли точка внутри шестиугольника
            // Для правильного шестиугольника можно проверить расстояние до центра и углы
            if (isInsideHexagon(x, y, centerX, centerY, side)) {
                pointsInside++
            }
        }

        val ratio = pointsInside.toDouble() / numberOfPoints
        val area = ratio * boundingArea
        val exactArea = AnalyticalFormulas.getHexagonArea(side)

        return MonteCarloResult(area, numberOfPoints, pointsInside, boundingArea, ratio, exactArea)
    }

    private fun isInsideHexagon(x: Double, y: Double, centerX: Double, centerY: Double, side: Double): Boolean {
        // Смещаем точку относительно центра
        val dx = abs(x - centerX)
        val dy = abs(y - centerY)

        // Высота от центра до вершины
        val r = side

        // Высота от центра до середины стороны
        val h = side * sqrt(3.0) / 2

        // Проверяем, находится ли точка внутри шестиугольника
        return (dx <= side / 2 * 2) && (dy <= h) && (side * h - side / 2 * dy - h * dx >= 0)
    }

    private fun updateVisualization(shapePosition: Int, param1: Double, param2: Double) {
        visualizationView.setShape(shapePosition, param1, param2)
        visualizationView.invalidate()
    }
}