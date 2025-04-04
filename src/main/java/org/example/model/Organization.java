package org.example.model;

import java.util.Objects;

/**
 * Класс, представляющий организацию работника.
 */
public class Organization {
    private Integer annualTurnover; // Поле может быть null, Значение поля должно быть больше 0
    private OrganizationType type; // Поле не может быть null

    /**
     * Конструктор класса Organization.
     *
     * @param annualTurnover годовой оборот организации (должен быть больше 0 или null)
     * @param type тип организации (не может быть null)
     * @throws IllegalArgumentException если annualTurnover <= 0 или type равен null
     */
    public Organization(Integer annualTurnover, OrganizationType type) {
        if (annualTurnover != null && annualTurnover <= 0) {
            throw new IllegalArgumentException("Годовой оборот должен быть больше 0");
        }
        if (type == null) {
            throw new IllegalArgumentException("Тип организации не может быть null");
        }
        this.annualTurnover = annualTurnover;
        this.type = type;
    }

    /**
     * Получить годовой оборот организации.
     *
     * @return годовой оборот
     */
    public Integer getAnnualTurnover() {
        return annualTurnover;
    }

    /**
     * Получить тип организации.
     *
     * @return тип организации
     */
    public OrganizationType getType() {
        return type;
    }

    /**
     * Установить годовой оборот организации.
     *
     * @param annualTurnover новый годовой оборот (должен быть больше 0 или null)
     * @throws IllegalArgumentException если annualTurnover <= 0
     */
    public void setAnnualTurnover(Integer annualTurnover) {
        if (annualTurnover != null && annualTurnover <= 0) {
            throw new IllegalArgumentException("Годовой оборот должен быть больше 0");
        }
        this.annualTurnover = annualTurnover;
    }

    /**
     * Установить тип организации.
     *
     * @param type новый тип организации (не может быть null)
     * @throws IllegalArgumentException если type равен null
     */
    public void setType(OrganizationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Тип организации не может быть null");
        }
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organization that = (Organization) o;
        return Objects.equals(annualTurnover, that.annualTurnover) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(annualTurnover, type);
    }

    @Override
    public String toString() {
        return "Organization{" +
                "annualTurnover=" + annualTurnover +
                ", type=" + type +
                '}';
    }
}