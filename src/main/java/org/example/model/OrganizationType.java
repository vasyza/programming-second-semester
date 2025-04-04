package org.example.model;

/**
 * Перечисление, представляющее типы организаций.
 */
public enum OrganizationType {
    PUBLIC,
    GOVERNMENT,
    TRUST,
    PRIVATE_LIMITED_COMPANY,
    OPEN_JOINT_STOCK_COMPANY;

    /**
     * Возвращает строковое представление всех возможных значений перечисления.
     *
     * @return строка со всеми значениями перечисления
     */
    public static String getAllValues() {
        StringBuilder sb = new StringBuilder();
        for (OrganizationType type : values()) {
            sb.append(type.name()).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Удаляем последнюю запятую и пробел
        }
        return sb.toString();
    }
}