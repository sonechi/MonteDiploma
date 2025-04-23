package com.example.montediploma

import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Объект, содержащий аналитические формулы для вычисления точных значений площадей фигур
 */
object AnalyticalFormulas {
    /**
     * Вычисляет площадь круга по формуле: π * r²
     * @param radius радиус круга
     * @return точное значение площади круга
     */
    fun getCircleArea(radius: Double): Double = PI * radius * radius

    /**
     * Вычисляет площадь прямоугольника по формуле: width * height
     * @param width ширина прямоугольника
     * @param height высота прямоугольника
     * @return точное значение площади прямоугольника
     */
    fun getRectangleArea(width: Double, height: Double): Double = width * height

    /**
     * Вычисляет площадь треугольника по формуле: 0.5 * base * height
     * @param base основание треугольника
     * @param height высота треугольника
     * @return точное значение площади треугольника
     */
    fun getTriangleArea(base: Double, height: Double): Double = 0.5 * base * height

    /**
     * Вычисляет площадь квадрата по формуле: side²
     * @param side сторона квадрата
     * @return точное значение площади квадрата
     */
    fun getSquareArea(side: Double): Double = side * side

    /**
     * Вычисляет площадь эллипса по формуле: π * a * b
     * @param semiMajor большая полуось эллипса (a)
     * @param semiMinor малая полуось эллипса (b)
     * @return точное значение площади эллипса
     */
    fun getEllipseArea(semiMajor: Double, semiMinor: Double): Double = PI * semiMajor * semiMinor

    /**
     * Вычисляет площадь правильного шестиугольника по формуле: (3√3)/2 * side²
     * @param side сторона шестиугольника
     * @return точное значение площади правильного шестиугольника
     */
    fun getHexagonArea(side: Double): Double = 3 * sqrt(3.0) / 2 * side * side
}