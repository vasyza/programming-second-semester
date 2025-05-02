package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.util.IdGenerator;
import org.example.common.model.Coordinates;
import org.example.common.model.Organization;
import org.example.common.model.OrganizationType;
import org.example.common.model.Position;
import org.example.common.model.Worker;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для управления коллекцией работников.
 */
public class CollectionManager {
    private final LinkedList<Worker> workers;
    private final LocalDate initializationDate;
    private String filePath;
    private static final Logger logger = LogManager.getLogger(CollectionManager.class);

    /**
     * Инициализирует пустую коллекцию и устанавливает дату инициализации.
     */
    public CollectionManager() {
        this.workers = new LinkedList<>();
        this.initializationDate = LocalDate.now();
    }

    /**
     * Загружает данные из CSV-файла в коллекцию.
     *
     * @param filePath путь к файлу CSV
     * @throws IOException              если произошла ошибка при чтении файла
     * @throws IllegalArgumentException если данные в файле некорректны
     */
    public void loadFromFile(String filePath) throws IOException, IllegalArgumentException {
        this.filePath = filePath;
        workers.clear();

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.error("Файл {} не найден. Коллекция будет пустой.", filePath);
            IdGenerator.setNextId(1L);
            return;
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Нет прав на чтение файла: " + filePath);
        }

        long maxId = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Worker worker = parseWorkerFromCSV(line);
                    if (worker.getId() > maxId) {
                        maxId = worker.getId();
                    }
                    workers.add(worker);
                } catch (Exception e) {
                    logger.error("Ошибка при парсе строки CSV: {}. {} Строка пропущена.", line, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IOException("Ошибка чтения файла: " + filePath, e);
        }

        IdGenerator.setNextId(maxId + 1);
    }

    /**
     * Сохраняет коллекцию в CSV-файл.
     *
     * @throws IOException если произошла ошибка при записи в файл
     */
    public void saveToFile() throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalStateException("Путь к файлу не задан. Невозможно сохранить коллекцию.");
        }

        Path path = Paths.get(filePath);

        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new IOException("Не удалось создать родительские каталоги для файла: " + filePath, e);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("id,name,coordinates_x,coordinates_y,creationDate,salary,startDate,endDate,position,organization_annualTurnover,organization_type");
            writer.newLine();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            DateTimeFormatter zonedDateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

            for (Worker worker : workers) {
                StringBuilder sb = new StringBuilder();
                sb.append(worker.getId()).append(',');
                sb.append(escapeCSV(worker.getName())).append(',');
                sb.append(worker.getCoordinates().getX()).append(',');
                sb.append(worker.getCoordinates().getY()).append(',');
                sb.append(worker.getCreationDate().format(dateFormatter)).append(',');
                sb.append(worker.getSalary() != null ? worker.getSalary() : "").append(',');
                sb.append(worker.getStartDate().format(dateTimeFormatter)).append(',');
                sb.append(worker.getEndDate() != null ? worker.getEndDate().format(zonedDateTimeFormatter) : "").append(',');
                sb.append(worker.getPosition() != null ? worker.getPosition() : "").append(',');
                sb.append(worker.getOrganization().getAnnualTurnover() != null ? worker.getOrganization().getAnnualTurnover() : "").append(',');
                sb.append(worker.getOrganization().getType());

                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IOException("Ошибка записи в файл: " + filePath, e);
        }
    }

    /**
     * Экранирует специальные символы в строке для CSV.
     *
     * @param value строка для экранирования
     * @return экранированная строка
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /**
     * Разбирает строку CSV и создает объект Worker.
     *
     * @param line строка CSV
     * @return объект Worker
     * @throws IllegalArgumentException если данные в строке некорректны
     */
    private Worker parseWorkerFromCSV(String line) throws IllegalArgumentException {
        String[] parts = line.split(",");
        if (parts.length < 11) {
            throw new IllegalArgumentException("Недостаточно данных в строке CSV");
        }

        try {
            Long id = Long.parseLong(parts[0]);
            String name = parts[1];
            Float x = Float.parseFloat(parts[2]);
            Double y = Double.parseDouble(parts[3]);
            LocalDate creationDate = LocalDate.parse(parts[4]);

            Long salary = parts[5].isEmpty() ? null : Long.parseLong(parts[5]);
            LocalDateTime startDate = LocalDateTime.parse(parts[6]);
            ZonedDateTime endDate = parts[7].isEmpty() ? null : ZonedDateTime.parse(parts[7]);

            Position position = parts[8].isEmpty() ? null : Position.valueOf(parts[8]);
            Integer annualTurnover = parts[9].isEmpty() ? null : Integer.parseInt(parts[9]);
            OrganizationType organizationType = OrganizationType.valueOf(parts[10]);

            Coordinates coordinates = new Coordinates(x, y);
            Organization organization = new Organization(annualTurnover, organizationType);

            return new Worker(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка при преобразовании числового значения: " + e.getMessage());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ошибка при преобразовании даты: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка в данных: " + e.getMessage());
        }
    }

    /**
     * Добавляет работника в коллекцию.
     *
     * @param worker работник для добавления
     */
    public void addWorker(Worker worker) {
        worker.setId(IdGenerator.getNextId());
        worker.setCreationDate(LocalDate.now());

        workers.add(worker);
    }

    /**
     * Обновляет работника с указанным ID.
     *
     * @param id        ID работника для обновления
     * @param newWorker новые данные работника
     * @return true, если работник был обновлен, false - если работник с указанным
     * ID не найден
     */
    public boolean updateWorker(Long id, Worker newWorker) {
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).getId().equals(id)) {
                workers.set(i, newWorker);
                return true;
            }
        }
        return false;
    }

    /**
     * Удаляет работника с указанным ID.
     *
     * @param id ID работника для удаления
     * @return true, если работник был удален, false - если работник с указанным ID
     * не найден
     */
    public boolean removeWorkerById(Long id) {
        return workers.removeIf(worker -> worker.getId().equals(id));
    }

    /**
     * Очищает коллекцию.
     */
    public void clear() {
        workers.clear();
    }

    /**
     * Добавляет работника, если его значение превышает значение наибольшего
     * элемента коллекции.
     *
     * @param worker работник для добавления
     * @return true, если работник был добавлен, false - если не был добавлен
     */
    public boolean addIfMax(Worker worker) {
        if (workers.isEmpty() || worker.compareTo(Collections.max(workers)) > 0) {
            workers.add(worker);
            return true;
        }
        return false;
    }

    /**
     * Добавляет работника, если его значение меньше, чем у наименьшего элемента
     * коллекции.
     *
     * @param worker работник для добавления
     * @return true, если работник был добавлен, false - если не был добавлен
     */
    public boolean addIfMin(Worker worker) {
        if (workers.isEmpty() || worker.compareTo(Collections.min(workers)) < 0) {
            workers.add(worker);
            return true;
        }
        return false;
    }

    /**
     * Возвращает список работников в порядке убывания.
     *
     * @return список работников в порядке убывания
     */
    public List<Worker> getDescending() {
        List<Worker> sortedList = new ArrayList<>(workers);
        sortedList.sort(Collections.reverseOrder());
        return sortedList;
    }

    /**
     * Возвращает значения поля salary всех элементов в порядке возрастания.
     *
     * @return список зарплат в порядке возрастания
     */
    public List<Long> getSalariesAscending() {
        return workers.stream().map(Worker::getSalary).filter(Objects::nonNull).sorted().collect(Collectors.toList());
    }

    /**
     * Возвращает значения поля salary всех элементов в порядке убывания.
     *
     * @return список зарплат в порядке убывания
     */
    public List<Long> getSalariesDescending() {
        return workers.stream().map(Worker::getSalary).filter(Objects::nonNull).sorted(Collections.reverseOrder()).collect(Collectors.toList());
    }

    /**
     * Возвращает дату инициализации коллекции.
     */
    public LocalDate getInitializationDate() {
        return initializationDate;
    }

    /**
     * Возвращает размер коллекции.
     */
    public int size() {
        return workers.size();
    }

    /**
     * Возвращает тип коллекции.
     */
    public String getType() {
        return "LinkedList<Worker>";
    }

    public String getInfo() {
        return "Информация о коллекции:\n" + "Тип: " + getType() + "\n" + "Дата инициализации: " + getInitializationDate() + "\n" + "Количество элементов: " + size() + "\n";
    }

    public List<Worker> getSortedWorkers() {
        return workers.stream().sorted().collect(Collectors.toList());
    }
}