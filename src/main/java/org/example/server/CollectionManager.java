package org.example.server;

import org.example.common.util.IdGenerator;
import org.example.common.model.Coordinates;
import org.example.common.model.Organization;
import org.example.common.model.OrganizationType;
import org.example.common.model.Position;
import org.example.common.model.Worker;

import java.io.FileWriter;
import java.io.IOException;
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

        try (Scanner scanner = new Scanner(new java.io.File(filePath))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            long maxId = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                try {
                    Worker worker = parseWorkerFromCSV(line);
                    if (worker.getId() > maxId) {
                        maxId = worker.getId();
                    }
                    workers.add(worker);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Ошибка при разборе строки CSV: " + line + ". " + e.getMessage());
                }
            }

            IdGenerator.setNextId(maxId + 1);
        }
    }

    /**
     * Сохраняет коллекцию в CSV-файл.
     *
     * @throws IOException если произошла ошибка при записи в файл
     */
    public void saveToFile() throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalStateException("Путь к файлу не задан");
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("id,name,coordinates_x,coordinates_y,creationDate,salary,startDate,endDate,position,organization_annualTurnover,organization_type\n");

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
                sb.append('\n');

                writer.write(sb.toString());
            }
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

    /**
     * Возвращает список всех работников.
     */
    public List<Worker> getAllWorkers() {
        return new ArrayList<>(workers);
    }

    /**
     * Проверяет, существует ли работник с указанным ID.
     *
     * @param id ID для проверки
     * @return true, если работник с указанным ID существует, false - если не
     * существует
     */
    public boolean existsById(Long id) {
        return workers.stream().anyMatch(worker -> worker.getId().equals(id));
    }

    public String getInfo() {
        return "Информация о коллекции:\n" + "Тип: " + getType() + "\n" + "Дата инициализации: " + getInitializationDate() + "\n" + "Количество элементов: " + size() + "\n";
    }

    public List<Worker> getSortedWorkers() {
        return workers.stream().sorted()
                .collect(Collectors.toList());
    }

    public Map<LocalDateTime, Long> groupCountingByStartDate() {
        return workers.stream().collect(Collectors.groupingBy(Worker::getStartDate, Collectors.counting()));
    }

    public boolean removeAnyByStartDate(LocalDateTime startDate) {
        Optional<Worker> workerToRemove = workers.stream().filter(w -> w.getStartDate().equals(startDate)).findFirst();

        if (workerToRemove.isPresent()) {
            workers.remove(workerToRemove.get());
            return true;
        }
        return false;
    }

    public long removeLower(Worker thresholdWorker) {
        long initialSize = workers.size();
        workers.removeIf(worker -> worker.compareTo(thresholdWorker) < 0);
        return initialSize - workers.size();
    }

    public Optional<Worker> removeHead() {
        if (workers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(workers.removeFirst());
    }
}