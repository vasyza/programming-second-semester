package org.example.command;

import java.io.IOException;

/**
 * Интерфейс для всех команд в приложении.
 * Определяет общий контракт для выполнения команд.
 */
public interface Command {
    
    /**
     * Выполняет команду с указанными аргументами.
     *
     * @param args аргументы команды
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @return true, если программа должна завершиться, false - в противном случае
     * @throws IOException если произошла ошибка ввода-вывода
     */
    boolean execute(String args, boolean fromScript) throws IOException;
    
    /**
     * Возвращает имя команды.
     *
     * @return имя команды
     */
    String getName();
    
    /**
     * Возвращает описание команды.
     *
     * @return описание команды
     */
    String getDescription();
}