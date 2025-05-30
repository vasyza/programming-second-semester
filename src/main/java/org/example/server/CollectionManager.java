package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.model.Worker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionManager {
    private final LinkedList<Worker> workers;
    private final LocalDate initializationDate;
    private final DatabaseManager databaseManager;
    private static final Logger logger = LogManager.getLogger(CollectionManager.class);
    private final Worker.LocationComparator locationComparator = new Worker.LocationComparator();

    public CollectionManager(DatabaseManager dbManager) {
        this.workers = new LinkedList<>();
        this.initializationDate = LocalDate.now();
        this.databaseManager = dbManager;
        loadFromDatabase();
    }

    public synchronized void loadFromDatabase() {
        workers.clear();
        List<Worker> loadedWorkers = databaseManager.loadAllWorkers();
        workers.addAll(loadedWorkers);
        logger.info("Collection successfully loaded from database. Loaded {} elements.", workers.size());
    }

    public synchronized String addWorker(Worker worker, int userId) {
        if (worker == null) {
            logger.warn("Attempt to add null worker by user {}.", userId);
            return "Cannot add null worker.";
        }
        Worker dbWorker = databaseManager.addWorker(worker, userId);
        if (dbWorker != null) {
            workers.add(dbWorker);
            logger.info("User {} added new worker with ID {}: {}", userId, dbWorker.getId(), dbWorker.getName());
            return "Работник успешно добавлен с ID " + dbWorker.getId() + ".";
        } else {
            logger.warn("Failed to add worker to database for user {}.", userId);
            return "Ошибка: Не удалось добавить работника в базу данных.";
        }
    }

    public synchronized String updateWorker(Long workerId, Worker newWorkerData, int userId) {
        if (workerId == null || newWorkerData == null) {
            logger.warn("User {} attempted to update worker with null ID or null data.", userId);
            return "ID работника и новые данные не могут быть null.";
        }

        Optional<Worker> workerToUpdateOpt = workers.stream().filter(w -> w.getId().equals(workerId)).findFirst();

        if (workerToUpdateOpt.isEmpty()) {
            logger.warn("Worker with ID {} for update not found in memory by user {}.", workerId, userId);
            return "Работник с ID " + workerId + " не найден для обновления.";
        }

        Worker existingWorker = workerToUpdateOpt.get();
        if (existingWorker.getOwnerId() != userId) {
            logger.warn("User {} (owner {}) attempted to update worker {} owned by user {}.", userId, newWorkerData.getOwnerId(), workerId, existingWorker.getOwnerId());
            return "Ошибка: Вы можете обновлять только тех работников, которых вы создали.";
        }

        newWorkerData.setId(workerId);
        newWorkerData.setOwnerId(userId);

        if (databaseManager.updateWorker(newWorkerData, userId)) {

            existingWorker.setName(newWorkerData.getName());
            existingWorker.setCoordinates(newWorkerData.getCoordinates());
            existingWorker.setSalary(newWorkerData.getSalary());
            existingWorker.setStartDate(newWorkerData.getStartDate());
            existingWorker.setEndDate(newWorkerData.getEndDate());
            existingWorker.setPosition(newWorkerData.getPosition());
            existingWorker.setOrganization(newWorkerData.getOrganization());

            logger.info("User {} successfully updated worker with ID {}.", userId, workerId);
            return "Работник с ID " + workerId + " успешно обновлен.";
        } else {
            logger.warn("Failed to update worker {} in database by user {}.", workerId, userId);
            return "Ошибка: Не удалось обновить работника в базе данных или он вам не принадлежит.";
        }
    }


    public synchronized String removeWorkerById(Long workerId, int userId) {
        if (workerId == null) {
            logger.warn("User {} attempted to remove worker with null ID.", userId);
            return "ID для удаления не может быть null.";
        }

        Optional<Worker> workerToRemoveOpt = workers.stream().filter(w -> w.getId().equals(workerId)).findFirst();

        if (workerToRemoveOpt.isEmpty()) {
            logger.warn("Worker with ID {} for removal not found in memory by user {}.", workerId, userId);
            return "Работник с ID " + workerId + " не найден для удаления.";
        }

        if (workerToRemoveOpt.get().getOwnerId() != userId) {
            logger.warn("User {} attempted to remove worker {} not owned by them.", userId, workerId);
            return "Ошибка: Вы можете удалять только тех работников, которых вы создали.";
        }

        if (databaseManager.deleteWorker(workerId, userId)) {
            boolean removedFromMemory = workers.removeIf(worker -> worker.getId().equals(workerId) && worker.getOwnerId() == userId);
            if (removedFromMemory) {
                logger.info("User {} removed worker with ID {} from collection and DB.", userId, workerId);
                return "Работник с ID " + workerId + " успешно удален.";
            } else {
                logger.error("Worker {} deleted from DB by user {} but failed to remove from memory collection!", workerId, userId);
                loadFromDatabase();
                return "Работник удален из БД, но произошла ошибка синхронизации с памятью. Коллекция перезагружена.";
            }
        } else {
            logger.warn("Failed to remove worker {} from database by user {}.", workerId, userId);
            return "Ошибка: Не удалось удалить работника из базы данных или он вам не принадлежит.";
        }
    }

    public synchronized String clear(int userId) {
        int affectedDBRows = databaseManager.clearWorkersByUserId(userId);
        if (affectedDBRows >= 0) {
            long initialMemorySize = workers.size();
            workers.removeIf(worker -> worker.getOwnerId() == userId);
            long removedMemoryCount = initialMemorySize - workers.size();
            logger.info("User {} cleared their workers. {} removed from DB, {} removed from memory.", userId, affectedDBRows, removedMemoryCount);
            if (affectedDBRows != removedMemoryCount) {
                logger.warn("DB ({}) and memory ({}) cleared counts differ for user {}. Reloading for consistency.", affectedDBRows, removedMemoryCount, userId);
                loadFromDatabase();
                return "Работники пользователя очищены. Обнаружено несоответствие с базой данных, коллекция перезагружена.";
            }
            return "Все принадлежащие вам работники (" + affectedDBRows + ") были удалены.";
        } else {
            logger.error("Error clearing workers for user {} from DB.", userId);
            return "Произошла ошибка при очистке ваших работников из базы данных.";
        }
    }

    public synchronized String addIfMax(Worker worker, int userId) {
        if (worker == null) {
            logger.warn("User {} attempted add_if_max with null worker.", userId);
            return "Cannot add_if_max with null worker.";
        }
        Optional<Worker> maxWorkerInCollection = workers.stream().max(Worker::compareTo);
        if (maxWorkerInCollection.isEmpty() || worker.compareTo(maxWorkerInCollection.get()) > 0) {
            Worker dbWorker = databaseManager.addWorker(worker, userId);
            if (dbWorker != null) {
                workers.add(dbWorker);
                Collections.sort(workers);
                logger.info("User {} added worker {} (add_if_max) with ID {}.", userId, dbWorker.getName(), dbWorker.getId());
                return "Работник " + dbWorker.getName() + " добавлен (add_if_max) с ID " + dbWorker.getId() + ".";
            } else {
                logger.warn("Failed to add worker to database (add_if_max) for user {}.", userId);
                return "Ошибка: Не удалось добавить работника в базу данных (add_if_max).";
            }
        }
        logger.info("Worker {} not added (add_if_max) by user {}, not greater than max.", worker.getName(), userId);
        return "Работник " + worker.getName() + " не добавлен (add_if_max), т.к. его значение не больше максимального.";
    }

    public synchronized String addIfMin(Worker worker, int userId) {
        if (worker == null) {
            logger.warn("User {} attempted add_if_min with null worker.", userId);
            return "Cannot add_if_min with null worker.";
        }
        Optional<Worker> minWorkerInCollection = workers.stream().min(Worker::compareTo);
        if (minWorkerInCollection.isEmpty() || worker.compareTo(minWorkerInCollection.get()) < 0) {
            Worker dbWorker = databaseManager.addWorker(worker, userId);
            if (dbWorker != null) {
                workers.add(dbWorker);
                Collections.sort(workers);
                logger.info("User {} added worker {} (add_if_min) with ID {}.", userId, dbWorker.getName(), dbWorker.getId());
                return "Работник " + dbWorker.getName() + " добавлен (add_if_min) с ID " + dbWorker.getId() + ".";
            } else {
                logger.warn("Failed to add worker to database (add_if_min) for user {}.", userId);
                return "Ошибка: Не удалось добавить работника в базу данных (add_if_min).";
            }
        }
        logger.info("Worker {} not added (add_if_min) by user {}, not less than min.", worker.getName(), userId);
        return "Работник " + worker.getName() + " не добавлен (add_if_min), т.к. его значение не меньше минимального.";
    }

    public synchronized List<Worker> getDescendingById() {
        return workers.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public synchronized List<Long> getSalariesAscending() {
        return workers.stream().map(Worker::getSalary).filter(Objects::nonNull).sorted().collect(Collectors.toList());
    }

    public synchronized List<Long> getSalariesDescending() {
        return workers.stream().map(Worker::getSalary).filter(Objects::nonNull).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public synchronized String getInfo() {
        return String.format("Тип коллекции: %s (в памяти, синхронизирована с БД PostgreSQL)\nДата инициализации сервера: %s\nКоличество элементов в памяти: %d", workers.getClass().getName(), initializationDate.format(DateTimeFormatter.ISO_DATE), workers.size());
    }

    /**
     * Возвращает всех работников, отсортированных по местоположению (X, затем Y).
     *
     * @return Неизменяемый список работников.
     */
    public synchronized List<Worker> getWorkersSortedByLocation() {
        return workers.stream().sorted(locationComparator).toList();
    }
}