package org.example.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Класс, представляющий работника.
 * Реализует интерфейс Comparable для сортировки по умолчанию по полю id.
 */
public class Worker implements Comparable<Worker>, Serializable {
    @Serial
    private static final long serialVersionUID = 101L;
    private Long id;
    private String name;
    private Coordinates coordinates;
    private LocalDate creationDate;
    private Long salary;
    private LocalDateTime startDate;
    private ZonedDateTime endDate;
    private Position position;
    private Organization organization;
    private int ownerId;

    public Worker(String name, Coordinates coordinates, Long salary, LocalDateTime startDate, ZonedDateTime endDate, Position position, Organization organization) {
        this(null, name, coordinates, null, salary, startDate, endDate, position, organization);
    }

    public Worker(Long id, String name, Coordinates coordinates, LocalDate creationDate, Long salary, LocalDateTime startDate, ZonedDateTime endDate, Position position, Organization organization) {
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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public Long getSalary() {
        return salary;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public Position getPosition() {
        return position;
    }

    public Organization getOrganization() {
        return organization;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID должен быть больше 0 и не null");
        }
        this.id = id;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        this.name = name;
    }

    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Координаты не могут быть null");
        }
        this.coordinates = coordinates;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public void setSalary(Long salary) {
        if (salary != null && salary <= 0) {
            throw new IllegalArgumentException("Зарплата должна быть больше 0");
        }
        this.salary = salary;
    }

    public void setStartDate(LocalDateTime startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Дата начала работы не может быть null");
        }
        this.startDate = startDate;
    }

    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setOrganization(Organization organization) {
        if (organization == null) {
            throw new IllegalArgumentException("Организация не может быть null");
        }
        this.organization = organization;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Worker worker = (Worker) o;
        return ownerId == worker.ownerId && Objects.equals(id, worker.id) && Objects.equals(name, worker.name) && Objects.equals(coordinates, worker.coordinates) && Objects.equals(creationDate, worker.creationDate) && Objects.equals(salary, worker.salary) && Objects.equals(startDate, worker.startDate) && Objects.equals(endDate, worker.endDate) && position == worker.position && Objects.equals(organization, worker.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization, ownerId);
    }

    @Override
    public String toString() {
        return "Worker{" + "id=" + id + ", name='" + name + '\'' + ", coordinates=" + coordinates + ", creationDate=" + creationDate + ", salary=" + salary + ", startDate=" + startDate + ", endDate=" + endDate + ", position=" + position + ", organization=" + organization + ", ownerId=" + ownerId + '}';
    }

    @Override
    public int compareTo(Worker other) {
        if (this.id == null && other.id == null) return 0;
        if (this.id == null) return -1;
        if (other.id == null) return 1;
        return this.id.compareTo(other.id);
    }

    public static class LocationComparator implements Comparator<Worker>, Serializable {
        @Serial
        private static final long serialVersionUID = 6L;

        @Override
        public int compare(Worker w1, Worker w2) {
            Coordinates c1 = w1.getCoordinates();
            Coordinates c2 = w2.getCoordinates();

            if (c1 == null && c2 == null) return 0;
            if (c1 == null) return -1;
            if (c2 == null) return 1;

            Float x1 = c1.getX();
            Float x2 = c2.getX();
            int xCompare;
            if (x1 == null && x2 == null) xCompare = 0;
            else if (x1 == null) xCompare = -1;
            else if (x2 == null) xCompare = 1;
            else xCompare = x1.compareTo(x2);

            if (xCompare != 0) {
                return xCompare;
            }

            Double y1 = c1.getY();
            Double y2 = c2.getY();
            if (y1 == null && y2 == null) return 0;
            if (y1 == null) return -1;
            if (y2 == null) return 1;
            return y1.compareTo(y2);
        }
    }
}