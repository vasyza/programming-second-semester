package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.util.IdGenerator;
import org.example.common.model.*; // Импортируем все модели

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

public class CollectionManager {
    private final LinkedList<Worker> workers;
    private final LocalDate initializationDate;
    private String filePath;
    private static final Logger logger = LogManager.getLogger(CollectionManager.class);
    private final Worker.LocationComparator locationComparator = new Worker.LocationComparator();

    public CollectionManager() {
        this.workers = new LinkedList<>();
        this.initializationDate = LocalDate.now();
    }

    public synchronized String getFilePath() {
        return filePath;
    }

    public synchronized void setFilePath(String filePath) {
        this.filePath = filePath;
        logger.info("Путь к файлу данных установлен: {}", filePath);
    }

    public synchronized void loadFromFile() throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("Путь к файлу не задан. Загрузка невозможна. Коллекция будет пустой.");
            IdGenerator.setNextId(1L);
            workers.clear();
            return;
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.warn("Файл {} не найден. Коллекция будет пустой. Файл будет создан при первом сохранении.", filePath);
            IdGenerator.setNextId(1L);
            workers.clear();
            return;
        }
        if (!Files.isReadable(path)) {
            logger.error("Нет прав на чтение файла: {}", filePath);
            throw new IOException("Нет прав на чтение файла: " + filePath);
        }

        workers.clear();
        List<String> lines;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine();
            lines = reader.lines().toList();
        } catch (IOException e) {
            logger.error("Ошибка чтения файла {}: {}", filePath, e.getMessage());
            throw new IOException("Ошибка чтения файла: " + filePath, e);
        }

        List<Worker> loadedWorkers = lines.stream()
                .map(line -> {
                    try {
                        return parseWorkerFromCSV(line);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Ошибка парсинга строки CSV: '{}'. Причина: {}. Строка пропущена.", line, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        workers.addAll(loadedWorkers);

        long maxId = workers.stream()
                .mapToLong(Worker::getId)
                .max()
                .orElse(0L);
        IdGenerator.setNextId(maxId + 1);
        logger.info("Коллекция успешно загружена из файла {}. Загружено {} элементов. Следующий ID: {}", filePath, workers.size(), maxId + 1);
    }


    private Worker parseWorkerFromCSV(String line) throws IllegalArgumentException {
        String[] parts = line.split(",", -1);
        if (parts.length < 11) {
            throw new IllegalArgumentException("Недостаточно данных в строке CSV (" + parts.length + " полей): " + line);
        }

        try {
            Long id = Long.parseLong(parts[0].trim());
            String name = parts[1].trim();
            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1).replace("\"\"", "\"");
            }

            Float x = Float.parseFloat(parts[2].trim());
            Double y = Double.parseDouble(parts[3].trim());
            LocalDate creationDate = LocalDate.parse(parts[4].trim());

            Long salary = parts[5].trim().isEmpty() ? null : Long.parseLong(parts[5].trim());
            LocalDateTime startDate = LocalDateTime.parse(parts[6].trim());
            ZonedDateTime endDate = parts[7].trim().isEmpty() ? null : ZonedDateTime.parse(parts[7].trim());

            Position position = parts[8].trim().isEmpty() ? null : Position.valueOf(parts[8].trim().toUpperCase());

            Integer annualTurnover = parts[9].trim().isEmpty() ? null : Integer.parseInt(parts[9].trim());
            OrganizationType organizationType = OrganizationType.valueOf(parts[10].trim().toUpperCase());


            Coordinates coordinates = new Coordinates(x, y);
            Organization organization = new Organization(annualTurnover, organizationType);

            return new Worker(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка преобразования числа: " + e.getMessage() + " в строке: " + line, e);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ошибка преобразования даты: " + e.getMessage() + " в строке: " + line, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка в данных: " + e.getMessage() + " в строке: " + line, e);
        }
    }


    public synchronized void saveToFile() throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            logger.error("Путь к файлу не задан. Сохранение невозможно.");
            throw new IllegalStateException("Путь к файлу не задан. Невозможно сохранить коллекцию.");
        }
        logger.info("Начало сохранения коллекции в файл: {}", filePath);
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                logger.info("Созданы родительские директории: {}", parentDir);
            } catch (IOException e) {
                logger.error("Не удалось создать директории для файла {}: {}", filePath, e.getMessage());
                throw new IOException("Не удалось создать директории: " + filePath, e);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("id,name,coordinates_x,coordinates_y,creationDate,salary,startDate,endDate,position,organization_annualTurnover,organization_type");
            writer.newLine();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            DateTimeFormatter zonedDateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

            String dataToWrite = workers.stream()
                    .sorted()
                    .map(worker -> String.join(",",
                            worker.getId().toString(),
                            escapeCSV(worker.getName()),
                            worker.getCoordinates().getX().toString(),
                            worker.getCoordinates().getY().toString(),
                            worker.getCreationDate().format(dateFormatter),
                            worker.getSalary() != null ? worker.getSalary().toString() : "",
                            worker.getStartDate().format(dateTimeFormatter),
                            worker.getEndDate() != null ? worker.getEndDate().format(zonedDateTimeFormatter) : "",
                            worker.getPosition() != null ? worker.getPosition().name() : "",
                            worker.getOrganization().getAnnualTurnover() != null ? worker.getOrganization().getAnnualTurnover().toString() : "",
                            worker.getOrganization().getType().name()
                    ))
                    .collect(Collectors.joining("\n"));
            if (!dataToWrite.isEmpty()){
                writer.write(dataToWrite);
                writer.newLine();
            }

            logger.info("Коллекция из {} элементов успешно сохранена в файл {}", workers.size(), filePath);
        } catch (IOException e) {
            logger.error("Ошибка записи в файл {}: {}", filePath, e.getMessage());
            throw new IOException("Ошибка записи в файл: " + filePath, e);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }


    public synchronized void addWorker(Worker worker) {
        if (worker == null) {
            logger.warn("Попытка добавить null работника.");
            return;
        }
        worker.setId(IdGenerator.getNextId());
        worker.setCreationDate(LocalDate.now());
        workers.add(worker);
        logger.info("Добавлен новый работник с ID {}: {}", worker.getId(), worker);
    }

    public synchronized boolean updateWorker(Long id, Worker newWorkerData) {
        if (id == null || newWorkerData == null) {
            logger.warn("Попытка обновить работника с null ID или null данными.");
            return false;
        }
        Optional<Worker> workerToUpdateOpt = workers.stream()
                .filter(w -> w.getId().equals(id))
                .findFirst();

        if (workerToUpdateOpt.isPresent()) {
            Worker workerToUpdate = workerToUpdateOpt.get();
            LocalDate originalCreationDate = workerToUpdate.getCreationDate();
            Long originalId = workerToUpdate.getId();

            workerToUpdate.setName(newWorkerData.getName());
            workerToUpdate.setCoordinates(newWorkerData.getCoordinates());
            workerToUpdate.setSalary(newWorkerData.getSalary());
            workerToUpdate.setStartDate(newWorkerData.getStartDate());
            workerToUpdate.setEndDate(newWorkerData.getEndDate());
            workerToUpdate.setPosition(newWorkerData.getPosition());
            workerToUpdate.setOrganization(newWorkerData.getOrganization());

            workerToUpdate.setId(originalId);
            workerToUpdate.setCreationDate(originalCreationDate);

            logger.info("Работник с ID {} успешно обновлен.", id);
            return true;
        }
        logger.warn("Работник с ID {} для обновления не найден.", id);
        return false;
    }


    public synchronized boolean removeWorkerById(Long id) {
        if (id == null) return false;
        boolean removed = workers.removeIf(worker -> worker.getId().equals(id));
        if (removed) {
            logger.info("Работник с ID {} удален из коллекции.", id);
        } else {
            logger.warn("Работник с ID {} для удаления не найден.", id);
        }
        return removed;
    }

    public synchronized void clear() {
        workers.clear();
        logger.info("Коллекция была очищена. Текущий размер: 0.");
    }

    public synchronized boolean addIfMax(Worker worker) {
        if (worker == null) {
            logger.warn("Попытка add_if_max с null работником.");
            return false;
        }
        Optional<Worker> maxWorkerInCollection = workers.stream().max(Worker::compareTo);
        if (maxWorkerInCollection.isEmpty() || worker.compareTo(maxWorkerInCollection.get()) > 0) {
            worker.setId(IdGenerator.getNextId());
            worker.setCreationDate(LocalDate.now());
            workers.add(worker);
            logger.info("Работник {} добавлен (add_if_max), т.к. его значение больше максимального.", worker.getId());
            return true;
        }
        logger.info("Работник {} не добавлен (add_if_max), т.к. его значение не больше максимального.", worker.getName());
        return false;
    }

    public synchronized boolean addIfMin(Worker worker) {
        if (worker == null) {
            logger.warn("Попытка add_if_min с null работником.");
            return false;
        }
        Optional<Worker> minWorkerInCollection = workers.stream().min(Worker::compareTo);
        if (minWorkerInCollection.isEmpty() || worker.compareTo(minWorkerInCollection.get()) < 0) {
            worker.setId(IdGenerator.getNextId());
            worker.setCreationDate(LocalDate.now());
            workers.add(worker);
            logger.info("Работник {} добавлен (add_if_min), т.к. его значение меньше минимального.", worker.getId());
            return true;
        }
        logger.info("Работник {} не добавлен (add_if_min), т.к. его значение не меньше минимального.", worker.getName());
        return false;
    }

    public synchronized List<Worker> getDescendingById() {
        return workers.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    public synchronized List<Long> getSalariesAscending() {
        return workers.stream()
                .map(Worker::getSalary)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    public synchronized List<Long> getSalariesDescending() {
        return workers.stream()
                .map(Worker::getSalary)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    public synchronized String getInfo() {
        return String.format("Тип коллекции: %s\nДата инициализации: %s\nКоличество элементов: %d\nФайл данных: %s",
                workers.getClass().getName(),
                initializationDate.format(DateTimeFormatter.ISO_DATE),
                workers.size(),
                (filePath != null && !filePath.isEmpty() ? filePath : "не задан"));
    }

    /**
     * Возвращает всех работников, отсортированных по местоположению (X, затем Y).
     * @return Неизменяемый список работников.
     */
    public synchronized List<Worker> getWorkersSortedByLocation() {
        return workers.stream()
                .sorted(locationComparator)
                .toList();
    }
}