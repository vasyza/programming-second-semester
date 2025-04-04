package org.example.model;

/**
 * Перечисление, представляющее должности работников.
 */
public enum Position {
    DIRECTOR,
    LABORER,
    BAKER,
    COOK;

    /**
     * Возвращает строковое представление всех возможных значений перечисления.
     *
     * @return строка со всеми значениями перечисления
     */
    public static String getAllValues() {
        StringBuilder sb = new StringBuilder();
        for (Position position : values()) {
            sb.append(position.name()).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Удаляем последнюю запятую и пробел
        }
        return sb.toString();
    }
}