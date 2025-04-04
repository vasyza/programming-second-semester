package org.example.model;

import java.util.Objects;

/**
 * Класс, представляющий координаты работника.
 */
public class Coordinates {
    private Float x; // Поле не может быть null
    private Double y; // Значение поля должно быть больше -72, Поле не может быть null

    /**
     * Конструктор класса Coordinates.
     *
     * @param x координата x
     * @param y координата y (должна быть больше -72)
     * @throws IllegalArgumentException если y <= -72 или какой-либо из параметров равен null
     */
    public Coordinates(Float x, Double y) {
        if (x == null) {
            throw new IllegalArgumentException("Координата x не может быть null");
        }
        if (y == null) {
            throw new IllegalArgumentException("Координата y не может быть null");
        }
        if (y <= -72) {
            throw new IllegalArgumentException("Координата y должна быть больше -72");
        }
        this.x = x;
        this.y = y;
    }

    /**
     * Получить координату x.
     *
     * @return координата x
     */
    public Float getX() {
        return x;
    }

    /**
     * Получить координату y.
     *
     * @return координата y
     */
    public Double getY() {
        return y;
    }

    /**
     * Установить координату x.
     *
     * @param x новая координата x
     * @throws IllegalArgumentException если x равен null
     */
    public void setX(Float x) {
        if (x == null) {
            throw new IllegalArgumentException("Координата x не может быть null");
        }
        this.x = x;
    }

    /**
     * Установить координату y.
     *
     * @param y новая координата y (должна быть больше -72)
     * @throws IllegalArgumentException если y <= -72 или равен null
     */
    public void setY(Double y) {
        if (y == null) {
            throw new IllegalArgumentException("Координата y не может быть null");
        }
        if (y <= -72) {
            throw new IllegalArgumentException("Координата y должна быть больше -72");
        }
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return Objects.equals(x, that.x) && Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}