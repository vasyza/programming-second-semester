package org.example.client;

import org.example.common.model.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class UserInputHandler {
    private final Scanner scanner;

    public UserInputHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    public Worker readWorker(boolean fromScript) {
        String name = readString(fromScript ? null : "Введите имя работника: ", fromScript, false);
        Float x = readFloat(fromScript ? null : "Введите координату x: ", fromScript, false);
        Double y = readDouble(fromScript ? null : "Введите координату y (> -72): ", fromScript, false);
        Long salary = readLong(fromScript ? null : "Введите зарплату (> 0, или пустая строка для null): ", fromScript, true, true);
        LocalDateTime startDate = readLocalDateTime(fromScript ? null : "Введите дату начала работы (гггг-ММ-ддТЧЧ:мм:сс): ", fromScript, false);
        ZonedDateTime endDate = readZonedDateTime(fromScript ? null : "Введите дату окончания работы (гггг-ММ-ддТЧЧ:мм:сс[+-]ЧЧ:ММ[ЗонаID], или пустая строка для null): ", fromScript, true);

        String positionPrompt = "Введите должность из списка [" + Position.getAllValues() + "] (или пустая строка для null): ";
        Position position = readEnum(Position.class, fromScript ? null : positionPrompt, fromScript, true);

        Integer annualTurnover = readInteger(fromScript ? null : "Введите годовой оборот организации (> 0, или пустая строка для null): ", fromScript, true, true);

        String orgTypePrompt = "Введите тип организации из списка [" + OrganizationType.getAllValues() + "]: ";
        OrganizationType organizationType = readEnum(OrganizationType.class, fromScript ? null : orgTypePrompt, fromScript, false);

        Coordinates coordinates = new Coordinates(x, y);
        Organization organization = new Organization(annualTurnover, organizationType);

        return new Worker(name, coordinates, salary, startDate, endDate, position, organization);
    }

    private <T extends Enum<T>> T readEnum(Class<T> enumClass, String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            if (!fromScript && prompt != null) {
                System.out.print(prompt);
            }
            String input = readString(null, fromScript, allowEmpty);

            if (input == null && allowEmpty) return null;
            if (input == null) {
                System.err.println("Ошибка: ввод не может быть null здесь. Повторите.");
                continue;
            }

            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                StringBuilder validValues = new StringBuilder();
                for (T val : enumClass.getEnumConstants()) {
                    validValues.append(val.name()).append(", ");
                }
                if (!validValues.isEmpty()) validValues.setLength(validValues.length() - 2);

                String errorMsg = "Некорректное значение. Допустимые значения для " + enumClass.getSimpleName() + ": " + validValues;
                if (fromScript) throw new IllegalArgumentException(errorMsg + ". Получено: " + input);
                System.err.println(errorMsg + ". Повторите ввод.");
            }
        }
    }

    public String readString(String prompt, boolean fromScript, boolean allowEmpty) {
        if (!fromScript && prompt != null) {
            System.out.print(prompt);
        }
        String input;
        try {
            input = scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            String errorMsg = "Достигнут конец ввода" + (fromScript ? " в скрипте" : "") + ".";
            if (fromScript) throw new IllegalStateException(errorMsg, e);
            System.err.println("\n" + errorMsg + " Завершение работы клиента.");
            System.exit(0);
            return null;
        }

        if (input.isEmpty()) {
            if (allowEmpty) return null;
            String errorMsg = "Это поле не может быть пустым.";
            if (fromScript) throw new IllegalArgumentException(errorMsg);
            System.err.println(errorMsg + " Повторите ввод.");
            return readString(prompt, false, false);
        }
        return input;
    }

    private <T extends Number> T readNumber(String prompt, boolean fromScript, boolean allowEmpty, boolean allowZeroOrNegative, java.util.function.Function<String, T> parser, java.util.function.Predicate<T> validator, String validationErrorMsg) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null && allowEmpty) return null;
            if (input == null && fromScript)
                throw new IllegalArgumentException("Пустой ввод не разрешен для этого числового поля в скрипте.");

            try {
                T value = parser.apply(input);
                if (!allowZeroOrNegative && (value.doubleValue() <= 0)) {
                    String errorMsg = "Значение должно быть больше 0.";
                    if (fromScript) throw new IllegalArgumentException(errorMsg);
                    System.err.println(errorMsg + " Повторите ввод.");
                    continue;
                }
                if (!validator.test(value)) {
                    String errorMsg = validationErrorMsg != null ? validationErrorMsg : "Недопустимое значение.";
                    if (fromScript) throw new IllegalArgumentException(errorMsg);
                    System.err.println(errorMsg + " Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                String errorMsg = "Некорректный формат числа: " + input;
                if (fromScript) throw new IllegalArgumentException(errorMsg);
                System.err.println(errorMsg + ". Повторите ввод.");
            }
        }
    }

    public Integer readInteger(String prompt, boolean fromScript, boolean allowEmpty, boolean isAnnualTurnover) {
        return readNumber(prompt, fromScript, allowEmpty, !isAnnualTurnover, Integer::parseInt, val -> !isAnnualTurnover || val > 0, isAnnualTurnover ? "Годовой оборот должен быть больше 0." : "Значение Integer должно быть > 0 (если не null).");
    }

    public Long readLong(String prompt, boolean fromScript, boolean allowEmpty, boolean isSalaryField) {
        return readNumber(prompt, fromScript, allowEmpty, !isSalaryField, Long::parseLong, val -> !isSalaryField || val > 0, isSalaryField ? "Зарплата должна быть больше 0." : "Значение Long должно быть > 0.");
    }

    public Float readFloat(String prompt, boolean fromScript, boolean allowEmpty) {
        return readNumber(prompt, fromScript, allowEmpty, true, Float::parseFloat, val -> true, null);
    }

    public Double readDouble(String prompt, boolean fromScript, boolean allowEmpty) {
        return readNumber(prompt, fromScript, allowEmpty, true, Double::parseDouble, val -> val > -72, "Значение координаты Y должно быть больше -72.");
    }

    public LocalDateTime readLocalDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null && allowEmpty) return null;
            if (input == null && fromScript)
                throw new IllegalArgumentException("Пустой ввод не разрешен для LocalDateTime в скрипте.");

            try {
                assert input != null;
                return LocalDateTime.parse(input);
            } catch (DateTimeParseException e) {
                String errorMsg = "Некорректный формат даты и времени (ожидается гггг-ММ-ддТЧЧ:мм:сс): " + input;
                if (fromScript) throw new IllegalArgumentException(errorMsg);
                System.err.println(errorMsg + ". Повторите ввод.");
            }
        }
    }

    public ZonedDateTime readZonedDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null && allowEmpty) return null;
            if (input == null && fromScript)
                throw new IllegalArgumentException("Пустой ввод не разрешен для ZonedDateTime в скрипте.");

            try {
                assert input != null;
                return ZonedDateTime.parse(input);
            } catch (DateTimeParseException e) {
                String errorMsg = "Некорректный формат даты и времени с зоной (ожидается гггг-ММ-ддТЧЧ:мм:сс+ЧЧ:ММ[Зона]): " + input;
                if (fromScript) throw new IllegalArgumentException(errorMsg);
                System.err.println(errorMsg + ". Повторите ввод.");
            }
        }
    }

    /**
     * Парсит Long из строки. Используется для ID в командах update, remove_by_id.
     *
     * @throws IllegalArgumentException если строка не может быть преобразована в
     *                                  Long или <= 0.
     */
    public Long parseLong(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым.");
        }
        try {
            long value = Long.parseLong(str.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " должен быть больше 0.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат " + fieldName + ": '" + str + "'. Ожидалось целое число больше 0.");
        }
    }

    public String[] readCredentials(String argsString, String commandName) {
        String username, password;
        if (argsString != null && !argsString.isBlank()) {
            String[] parts = argsString.split("\\s+", 2);
            if (parts.length == 2) {
                username = parts[0];
                password = parts[1];
                if (username.isEmpty() || password.isEmpty()) {
                    System.out.println("Ошибка: Имя пользователя и пароль не могут быть пустыми, если указаны в одной строке.");
                    return null;
                }
                return new String[]{username, password};
            } else {
                System.out.println("Ошибка: Для команды '" + commandName + "' ожидается <username> <password> в одной строке, либо введите их по запросу.");
            }
        }
        System.out.print("Введите имя пользователя: ");
        username = readString(null, false, false);
        if (username == null) return null;

        System.out.print("Введите пароль: ");
        password = readString(null, false, false);
        if (password == null) return null;

        return new String[]{username, password};
    }
}