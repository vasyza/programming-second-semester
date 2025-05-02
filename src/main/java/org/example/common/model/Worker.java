package org.example.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Класс, представляющий работника.
 * Реализует интерфейс Comparable для сортировки по умолчанию по полю id.
 */
public class Worker implements Comparable<Worker>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id; // Поле не может быть null, Значение поля должно быть больше 0, Значение этого
                     // поля должно быть уникальным, Значение этого поля должно генерироваться
                     // автоматически
    private String name; // Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; // Поле не может быть null
    private LocalDate creationDate; // Поле не может быть null, Значение этого поля должно генерироваться
                                    // автоматически
    private Long salary; // Поле может быть null, Значение поля должно быть больше 0
    private LocalDateTime startDate; // Поле не может быть null
    private ZonedDateTime endDate; // Поле может быть null
    private Position position; // Поле может быть null
    private Organization organization; // Поле не может быть null

    /**
     * Конструктор класса Worker.
     *
     * @param id           уникальный идентификатор работника (генерируется
     *                     автоматически)
     * @param name         имя работника (не может быть null или пустым)
     * @param coordinates  координаты работника (не может быть null)
     * @param creationDate дата создания записи (генерируется автоматически)
     * @param salary       зарплата работника (может быть null, но если не null, то
     *                     должна быть больше 0)
     * @param startDate    дата начала работы (не может быть null)
     * @param endDate      дата окончания работы (может быть null)
     * @param position     должность работника (может быть null)
     * @param organization организация работника (не может быть null)
     * @throws IllegalArgumentException если какой-либо из параметров не
     *                                  соответствует требованиям
     */
    public Worker(Long id, String name, Coordinates coordinates, LocalDate creationDate,
            Long salary, LocalDateTime startDate, ZonedDateTime endDate,
            Position position, Organization organization) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("Координаты не могут быть null");
        }
        if (salary != null && salary <= 0) {
            throw new IllegalArgumentException("Зарплата должна быть больше 0");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Дата начала работы не может быть null");
        }
        if (organization == null) {
            throw new IllegalArgumentException("Организация не может быть null");
        }

        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.salary = salary;
        this.startDate = startDate;
        this.endDate = endDate;
        this.position = position;
        this.organization = organization;
    }

    /**
     * Получить ID работника.
     *
     * @return ID работника
     */
    public Long getId() {
        return id;
    }

    /**
     * Получить имя работника.
     *
     * @return имя работника
     */
    public String getName() {
        return name;
    }

    /**
     * Получить координаты работника.
     *
     * @return координаты работника
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Получить дату создания записи.
     *
     * @return дата создания
     */
    public LocalDate getCreationDate() {
        return creationDate;
    }

    /**
     * Получить зарплату работника.
     *
     * @return зарплата работника
     */
    public Long getSalary() {
        return salary;
    }

    /**
     * Получить дату начала работы.
     *
     * @return дата начала работы
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Получить дату окончания работы.
     *
     * @return дата окончания работы
     */
    public ZonedDateTime getEndDate() {
        return endDate;
    }

    /**
     * Получить должность работника.
     *
     * @return должность работника
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Получить организацию работника.
     *
     * @return организация работника
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Установить имя работника.
     *
     * @param name новое имя (не может быть null или пустым)
     * @throws IllegalArgumentException если name равен null или пустой
     */
    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        this.name = name;
    }

    /**
     * Установить координаты работника.
     *
     * @param coordinates новые координаты (не может быть null)
     * @throws IllegalArgumentException если coordinates равен null
     */
    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Координаты не могут быть null");
        }
        this.coordinates = coordinates;
    }

    /**
     * Установить зарплату работника.
     *
     * @param salary новая зарплата (может быть null, но если не null, то должна
     *               быть больше 0)
     * @throws IllegalArgumentException если salary <= 0
     */
    public void setSalary(Long salary) {
        if (salary != null && salary <= 0) {
            throw new IllegalArgumentException("Зарплата должна быть больше 0");
        }
        this.salary = salary;
    }

    /**
     * Установить дату начала работы.
     *
     * @param startDate новая дата начала работы (не может быть null)
     * @throws IllegalArgumentException если startDate равен null
     */
    public void setStartDate(LocalDateTime startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Дата начала работы не может быть null");
        }
        this.startDate = startDate;
    }

    /**
     * Установить дату окончания работы.
     *
     * @param endDate новая дата окончания работы (может быть null)
     */
    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    /**
     * Установить должность работника.
     *
     * @param position новая должность (может быть null)
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Установить организацию работника.
     *
     * @param organization новая организация (не может быть null)
     * @throws IllegalArgumentException если organization равен null
     */
    public void setOrganization(Organization organization) {
        if (organization == null) {
            throw new IllegalArgumentException("Организация не может быть null");
        }
        this.organization = organization;
    }

    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID должен быть больше 0");
        }
        this.id = id;
    }

    public void setCreationDate(LocalDate creationDate) {
        if (creationDate == null) {
            throw new IllegalArgumentException("Дата создания не может быть null");
        }
        this.creationDate = creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Worker worker = (Worker) o;
        return Objects.equals(id, worker.id) &&
                Objects.equals(name, worker.name) &&
                Objects.equals(coordinates, worker.coordinates) &&
                Objects.equals(creationDate, worker.creationDate) &&
                Objects.equals(salary, worker.salary) &&
                Objects.equals(startDate, worker.startDate) &&
                Objects.equals(endDate, worker.endDate) &&
                position == worker.position &&
                Objects.equals(organization, worker.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization);
    }

    @Override
    public String toString() {
        return "Worker{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", salary=" + salary +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", position=" + position +
                ", organization=" + organization +
                '}';
    }

    @Override
    public int compareTo(Worker other) {
        return this.id.compareTo(other.id);
    }
}