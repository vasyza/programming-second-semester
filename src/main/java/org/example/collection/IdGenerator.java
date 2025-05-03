package org.example.collection;

/**
 * Класс для генерации уникальных идентификаторов.
 * Используется для автоматической генерации ID работников.
 */
public class IdGenerator {
    private static long nextId = 1;

    /**
     * Возвращает следующий уникальный идентификатор.
     */
    public static synchronized long getNextId() {
        return nextId++;
    }

    /**
     * Устанавливает значение для следующего идентификатора.
     */
    public static synchronized void setNextId(long id) {
        nextId = id;
    }
}