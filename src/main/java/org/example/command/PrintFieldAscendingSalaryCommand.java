package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Команда для вывода значений поля salary всех элементов в порядке возрастания.
 */
public class PrintFieldAscendingSalaryCommand extends AbstractCommand {

    /**
     * Конструктор команды print_field_ascending_salary.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public PrintFieldAscendingSalaryCommand(WorkerCollection collection, Scanner scanner) {
        super("print_field_ascending_salary", "вывести значения поля salary всех элементов в порядке возрастания", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        List<Long> salaries = collection.getSalariesAscending();
        if (salaries.isEmpty()) {
            System.out.println("Коллекция пуста или ни у одного работника не указана зарплата.");
        } else {
            System.out.println("Значения поля salary всех элементов в порядке возрастания:");
            for (Long salary : salaries) {
                System.out.println(salary);
            }
        }
        return false;
    }
}