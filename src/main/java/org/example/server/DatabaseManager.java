package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.model.*;
import org.example.server.util.PasswordHasher;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseManager(String host, String dbName, String user, String password) {
        this.dbUrl = "jdbc:postgresql://" + host + "/" + dbName;
        this.dbUser = user;
        this.dbPassword = password;
        initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" + "id SERIAL PRIMARY KEY," + "username VARCHAR(255) UNIQUE NOT NULL," + "password_hash VARCHAR(255) NOT NULL" + ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS workers (" + "id SERIAL PRIMARY KEY," + "name VARCHAR(255) NOT NULL," + "coordinates_x FLOAT NOT NULL," + "coordinates_y DOUBLE PRECISION NOT NULL CHECK (coordinates_y > -72)," + "creation_date DATE NOT NULL DEFAULT CURRENT_DATE," + "salary BIGINT CHECK (salary IS NULL OR salary > 0)," + "start_date TIMESTAMP NOT NULL," + "end_date TIMESTAMPTZ," + "position VARCHAR(255)," + "organization_annual_turnover INTEGER CHECK (organization_annual_turnover IS NULL OR organization_annual_turnover > 0)," + "organization_type VARCHAR(255) NOT NULL," + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE" + ");");
            
            logger.info("Tables 'users' and 'workers' successfully created or already exist in the database.");
        } catch (SQLException e) {
            logger.fatal("Error connecting to database: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при подключении к базе данных.", e);
        }
    }

    public Optional<User> registerUser(String username, String plainPassword) {
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        logger.info("User {} successfully registered with ID {}.", username, userId);
                        return Optional.of(new User(userId, username, hashedPassword));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // Если юзер существует
                logger.warn("User registration error for {}: User already exists.", username);
            } else {
                logger.error("User registration error for {}: {}", username, e.getMessage(), e);
            }
        }
        return Optional.empty();
    }

    public Optional<User> getUserByUsername(String username) {
        String sql = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(rs.getInt("id"), username, rs.getString("password_hash")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving user {}: {}", username, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public synchronized Worker addWorker(Worker worker, int userId) {
        String sql = "INSERT INTO workers (name, coordinates_x, coordinates_y, creation_date, salary, start_date, end_date, position, organization_annual_turnover, organization_type, user_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, worker.getName());
            pstmt.setFloat(2, worker.getCoordinates().getX());
            pstmt.setDouble(3, worker.getCoordinates().getY());
            LocalDate creationDate = LocalDate.now();
            pstmt.setDate(4, Date.valueOf(creationDate));
            if (worker.getSalary() != null) pstmt.setLong(5, worker.getSalary());
            else pstmt.setNull(5, Types.BIGINT);
            pstmt.setTimestamp(6, Timestamp.valueOf(worker.getStartDate()));
            if (worker.getEndDate() != null) pstmt.setTimestamp(7, Timestamp.from(worker.getEndDate().toInstant()));
            else pstmt.setNull(7, Types.TIMESTAMP_WITH_TIMEZONE);
            if (worker.getPosition() != null) pstmt.setString(8, worker.getPosition().name());
            else pstmt.setNull(8, Types.VARCHAR);
            if (worker.getOrganization().getAnnualTurnover() != null)
                pstmt.setInt(9, worker.getOrganization().getAnnualTurnover());
            else pstmt.setNull(9, Types.INTEGER);
            pstmt.setString(10, worker.getOrganization().getType().name());
            pstmt.setInt(11, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long newId = generatedKeys.getLong(1);
                        Worker dbWorker = new Worker(newId, worker.getName(), worker.getCoordinates(), creationDate, worker.getSalary(), worker.getStartDate(), worker.getEndDate(), worker.getPosition(), worker.getOrganization());
                        dbWorker.setOwnerId(userId);
                        logger.info("Worker '{}' created in database with ID {}.", worker.getName(), newId);
                        return dbWorker;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to add worker to database: {}", e.getMessage(), e);
        }
        return null;
    }

    public synchronized boolean updateWorker(Worker worker, int userId) {
        String checkOwnerSql = "SELECT user_id FROM workers WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkOwnerSql)) {
            checkStmt.setLong(1, worker.getId());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                if (rs.getInt("user_id") != userId) {
                    logger.warn("User {} attempted to update worker with ID {}, but didn't create it.", userId, worker.getId());
                    return false;
                }
            } else {
                logger.warn("Worker with ID {} not found for update.", worker.getId());
                return false;
            }
        } catch (SQLException e) {
            logger.error("Failed to check owner of worker {}: {}", worker.getId(), e.getMessage(), e);
            return false;
        }


        String sql = "UPDATE workers SET name = ?, coordinates_x = ?, coordinates_y = ?, salary = ?, " + "start_date = ?, end_date = ?, position = ?, organization_annual_turnover = ?, organization_type = ? " + "WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, worker.getName());
            pstmt.setFloat(2, worker.getCoordinates().getX());
            pstmt.setDouble(3, worker.getCoordinates().getY());
            if (worker.getSalary() != null) pstmt.setLong(4, worker.getSalary());
            else pstmt.setNull(4, Types.BIGINT);
            pstmt.setTimestamp(5, Timestamp.valueOf(worker.getStartDate()));
            if (worker.getEndDate() != null) pstmt.setTimestamp(6, Timestamp.from(worker.getEndDate().toInstant()));
            else pstmt.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE);
            if (worker.getPosition() != null) pstmt.setString(7, worker.getPosition().name());
            else pstmt.setNull(7, Types.VARCHAR);
            if (worker.getOrganization().getAnnualTurnover() != null)
                pstmt.setInt(8, worker.getOrganization().getAnnualTurnover());
            else pstmt.setNull(8, Types.INTEGER);
            pstmt.setString(9, worker.getOrganization().getType().name());
            pstmt.setLong(10, worker.getId());
            pstmt.setInt(11, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Worker with ID {} successfully updated by user {}.", worker.getId(), userId);
                return true;
            }
            logger.warn("Worker with ID {} not found or not created by user {} for update.", worker.getId(), userId);
        } catch (SQLException e) {
            logger.error("Error updating worker {} in database: {}", worker.getId(), e.getMessage(), e);
        }
        return false;
    }

    public synchronized boolean deleteWorker(long workerId, int userId) {
        String sql = "DELETE FROM workers WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, workerId);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Worker with ID {} successfully deleted by user {}.", workerId, userId);
                return true;
            }
            logger.warn("Worker with ID {} not found or not created by user {} for deletion.", workerId, userId);
        } catch (SQLException e) {
            logger.error("Error deleting worker {} from database: {}", workerId, e.getMessage(), e);
        }
        return false;
    }

    public synchronized int clearWorkersByUserId(int userId) {
        String sql = "DELETE FROM workers WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            logger.info("{} workers created by user {} deleted from database.", affectedRows, userId);
            return affectedRows;
        } catch (SQLException e) {
            logger.error("Error clearing workers for user {} from database: {}", userId, e.getMessage(), e);
        }
        return 0;
    }


    public List<Worker> loadAllWorkers() {
        List<Worker> workers = new LinkedList<>();
        String sql = "SELECT w.id, w.name, w.coordinates_x, w.coordinates_y, w.creation_date, w.salary, " + "w.start_date, w.end_date, w.position, w.organization_annual_turnover, w.organization_type, w.user_id, u.username as owner_username " + "FROM workers w JOIN users u ON w.user_id = u.id";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                Coordinates coordinates = new Coordinates(rs.getFloat("coordinates_x"), rs.getDouble("coordinates_y"));
                LocalDate creationDate = rs.getDate("creation_date").toLocalDate();
                Long salary = rs.getLong("salary");
                if (rs.wasNull()) salary = null;
                LocalDateTime startDate = rs.getTimestamp("start_date").toLocalDateTime();

                ZonedDateTime endDate = null;
                Timestamp endTimestamp = rs.getTimestamp("end_date");
                if (endTimestamp != null) {
                    endDate = ZonedDateTime.ofInstant(endTimestamp.toInstant(), ZoneOffset.UTC);
                }

                Position position = null;
                String posStr = rs.getString("position");
                if (posStr != null) position = Position.valueOf(posStr);

                Integer annualTurnover = rs.getInt("organization_annual_turnover");
                if (rs.wasNull()) annualTurnover = null;
                OrganizationType orgType = OrganizationType.valueOf(rs.getString("organization_type"));
                Organization organization = new Organization(annualTurnover, orgType);

                int ownerId = rs.getInt("user_id");

                Worker worker = new Worker(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization);
                worker.setOwnerId(ownerId);
                workers.add(worker);
            }
            logger.info("Loaded {} workers from database.", workers.size());
        } catch (SQLException e) {
            logger.error("Error loading workers from database: {}", e.getMessage(), e);
        }
        return workers;
    }
}