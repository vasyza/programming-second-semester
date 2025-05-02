package org.example.client;

import org.example.common.model.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Класс для обработки пользовательского ввода.
 * Содержит методы для чтения различных типов данных из консоли или скрипта.
 */
public class UserInputHandler {
    private final Scanner scanner;

    /**
     * Конструктор класса InputHandler.
     *
     * @param scanner сканнер для чтения ввода пользователя
     */
    public UserInputHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Читает данные работника из ввода пользователя.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @return новый объект Worker
     */
    public Worker readWorker(boolean fromScript) {
        String name = readString("Введите имя работника: ", fromScript, false);
        Float x = readFloat("Введите координату x: ", fromScript, false);
        Double y = readDouble("Введите координату y (> -72): ", fromScript, false);
        Long salary = readLong("Введите зарплату (> 0, или пустая строка для null): ", fromScript, true);
        LocalDateTime startDate = readLocalDateTime("Введите дату начала работы (в формате yyyy-MM-ddTHH:mm:ss): ", fromScript, false);
        ZonedDateTime endDate = readZonedDateTime("Введите дату окончания работы (в формате yyyy-MM-ddTHH:mm:ss+ZZ:ZZ, или пустая строка для null): ", fromScript, true);
        Position position = readPosition("Введите должность " + Position.getAllValues() + " (или пустая строка для null): ", fromScript, true);
        Integer annualTurnover = readInteger("Введите годовой оборот организации (> 0, или пустая строка для null): ", fromScript, true);
        OrganizationType organizationType = readOrganizationType("Введите тип организации " + OrganizationType.getAllValues() + ": ", fromScript, false);

        Coordinates coordinates = new Coordinates(x, y);
        Organization organization = new Organization(annualTurnover, organizationType);

        return new Worker(null, name, coordinates, null, salary, startDate, endDate, position, organization);
    }

    /**
     * Читает строку из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная строка или null, если ввод пуст и allowEmpty=true
     */
    public String readString(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            if (!fromScript) {
                System.out.print(prompt);
            }

            String input;
            if (scanner.hasNextLine()) {
                input = scanner.nextLine().trim();
            } else {
                throw new IllegalStateException("Достигнут конец ввода.");
            }

            if (input.isEmpty()) {
                if (allowEmpty) {
                    return null;
                } else if (fromScript) {
                    throw new IllegalArgumentException("Пустая строка не допускается.");
                } else {
                    System.out.println("Пустая строка не допускается. Повторите ввод.");
                    continue;
                }
            }

            return input;
        }
    }

    /**
     * Читает целое число типа Integer из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    public Integer readInteger(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                int value = Integer.parseInt(input);
                if (value <= 0) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше 0.");
                    }
                    System.out.println("Значение должно быть больше 0. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает целое число типа Long из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    public Long readLong(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                long value = Long.parseLong(input);
                if (value <= 0) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше 0.");
                    }
                    System.out.println("Значение должно быть больше 0. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает число с плавающей точкой типа Float из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    public Float readFloat(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return Float.parseFloat(input);
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает число с плавающей точкой типа Double из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    public Double readDouble(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                double value = Double.parseDouble(input);
                if (value <= -72) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше -72.");
                    }
                    System.out.println("Значение должно быть больше -72. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает дату и время типа LocalDateTime из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная дата и время или null, если ввод пуст и allowEmpty=true
     */
    public LocalDateTime readLocalDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return LocalDateTime.parse(input);
            } catch (DateTimeParseException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат даты и времени: " + input);
                }
                System.out.println("Некорректный формат даты и времени. Используйте формат yyyy-MM-ddTHH:mm:ss. Повторите ввод.");
            }
        }
    }

    /**
     * Читает дату и время с часовым поясом типа ZonedDateTime из ввода
     * пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная дата и время с часовым поясом или null, если ввод пуст и
     * allowEmpty=true
     */
    public ZonedDateTime readZonedDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return ZonedDateTime.parse(input);
            } catch (DateTimeParseException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат даты и времени с часовым поясом: " + input);
                }
                System.out.println("Некорректный формат даты и времени с часовым поясом. Используйте формат yyyy-MM-ddTHH:mm:ss+ZZ:ZZ. Повторите ввод.");
            }
        }
    }

    /**
     * Читает должность типа Position из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная должность или null, если ввод пуст и allowEmpty=true
     */
    public Position readPosition(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return Position.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректное значение должности: " + input);
                }
                System.out.println("Некорректное значение должности. Допустимые значения: " + Position.getAllValues() + ". Повторите ввод.");
            }
        }
    }

    /**
     * Читает тип организации типа OrganizationType из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенный тип организации или null, если ввод пуст и allowEmpty=true
     */
    public OrganizationType readOrganizationType(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return OrganizationType.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректное значение типа организации: " + input);
                }
                System.out.println("Некорректное значение типа организации. Допустимые значения: " + OrganizationType.getAllValues() + ". Повторите ввод.");
            }
        }
    }

    /**
     * Парсит Long из строки.
     *
     * @param str       строка для парсинга
     * @param fieldName имя поля для сообщения об ошибке
     * @return распарсенное значение
     * @throws IllegalArgumentException если строка не может быть преобразована в
     *                                  Long
     */
    public Long parseLong(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым.");
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат " + fieldName + ": " + str);
        }
    }
}