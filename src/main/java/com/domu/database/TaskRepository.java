package com.domu.database;

import com.domu.dto.TaskRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {

    private final DataSource dataSource;

    @Inject
    public TaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<TaskResponse> findByBuilding(Long buildingId) {
        String sql = "SELECT t.* FROM tasks t WHERE t.building_id = ? ORDER BY t.created_at DESC";
        List<TaskResponse> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing tasks", e);
        }
        return tasks;
    }

    public Optional<TaskResponse> findById(Long id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs, conn));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding task", e);
        }
        return Optional.empty();
    }

    public TaskResponse insert(TaskRequest req, Long createdByUserId) {
        String sql = "INSERT INTO tasks (building_id, title, description, assignee_id, status, priority, due_date, created_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setLong(1, req.communityId());
                stmt.setString(2, req.title());
                stmt.setString(3, req.description());
                // Mantener assignee_id para compatibilidad, pero usar null si hay assigneeIds
                Long singleAssigneeId = null;
                if (req.assigneeIds() != null && !req.assigneeIds().isEmpty()) {
                    // Si hay múltiples asignados, no usar assignee_id
                    singleAssigneeId = null;
                } else if (req.assigneeId() != null) {
                    singleAssigneeId = req.assigneeId();
                }
                if (singleAssigneeId != null) stmt.setLong(4, singleAssigneeId); else stmt.setNull(4, Types.BIGINT);
                stmt.setString(5, req.status() != null ? req.status() : "PENDING");
                stmt.setString(6, req.priority() != null ? req.priority() : "MEDIUM");
                if (req.dueDate() != null) stmt.setTimestamp(7, Timestamp.valueOf(req.dueDate())); else stmt.setNull(7, Types.TIMESTAMP);
                if (createdByUserId != null) stmt.setLong(8, createdByUserId); else stmt.setNull(8, Types.BIGINT);
                
                stmt.executeUpdate();
                Long taskId;
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        taskId = keys.getLong(1);
                    } else {
                        throw new RepositoryException("Failed to get generated task id");
                    }
                }
                
                // Insertar asignaciones múltiples
                List<Long> assigneeIds = req.assigneeIds();
                if (assigneeIds != null && !assigneeIds.isEmpty()) {
                    insertAssignments(conn, taskId, assigneeIds);
                } else if (req.assigneeId() != null) {
                    // Compatibilidad: si solo hay assigneeId, también crear en task_assignments
                    insertAssignments(conn, taskId, List.of(req.assigneeId()));
                }
                
                conn.commit();
                return findById(taskId).orElseThrow();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting task", e);
        }
    }

    public TaskResponse update(Long id, TaskRequest req) {
        String sql = "UPDATE tasks SET title = ?, description = ?, assignee_id = ?, status = ?, priority = ?, due_date = ?, completed_at = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, req.title());
                stmt.setString(2, req.description());
                
                // Manejar assignee_id (mantener compatibilidad)
                Long singleAssigneeId = null;
                if (req.assigneeIds() != null && !req.assigneeIds().isEmpty()) {
                    singleAssigneeId = null; // Si hay múltiples, no usar assignee_id
                } else if (req.assigneeId() != null) {
                    singleAssigneeId = req.assigneeId();
                }
                if (singleAssigneeId != null) stmt.setLong(3, singleAssigneeId); else stmt.setNull(3, Types.BIGINT);
                
                stmt.setString(4, req.status());
                stmt.setString(5, req.priority());
                if (req.dueDate() != null) stmt.setTimestamp(6, Timestamp.valueOf(req.dueDate())); else stmt.setNull(6, Types.TIMESTAMP);
                
                if ("COMPLETED".equalsIgnoreCase(req.status())) {
                    stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                } else if (req.completedAt() != null) {
                    stmt.setTimestamp(7, Timestamp.valueOf(req.completedAt()));
                } else {
                    stmt.setNull(7, Types.TIMESTAMP);
                }
                
                stmt.setLong(8, id);
                stmt.executeUpdate();
                
                // Actualizar asignaciones múltiples
                List<Long> assigneeIds = req.assigneeIds();
                if (assigneeIds != null) {
                    // Eliminar asignaciones existentes y crear nuevas
                    deleteAssignments(conn, id);
                    if (!assigneeIds.isEmpty()) {
                        insertAssignments(conn, id, assigneeIds);
                    }
                } else if (req.assigneeId() != null) {
                    // Compatibilidad: actualizar también task_assignments
                    deleteAssignments(conn, id);
                    insertAssignments(conn, id, List.of(req.assigneeId()));
                }
                
                conn.commit();
                return findById(id).orElseThrow();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error updating task", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting task", e);
        }
    }

    private TaskResponse mapRow(ResultSet rs, Connection conn) throws SQLException {
        Long taskId = rs.getLong("id");
        List<Long> assigneeIds = getAssigneeIds(conn, taskId);
        
        return new TaskResponse(
                taskId,
                rs.getLong("building_id"),
                rs.getString("title"),
                rs.getString("description"),
                (Long) rs.getObject("assignee_id"), // Mantenido para compatibilidad
                assigneeIds, // Lista de asignados
                rs.getString("status"),
                rs.getString("priority"),
                rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toLocalDateTime() : null,
                rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null,
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
    
    private List<Long> getAssigneeIds(Connection conn, Long taskId) throws SQLException {
        String sql = "SELECT staff_id FROM task_assignments WHERE task_id = ? ORDER BY assigned_at";
        List<Long> assigneeIds = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    assigneeIds.add(rs.getLong("staff_id"));
                }
            }
        }
        return assigneeIds;
    }
    
    private void insertAssignments(Connection conn, Long taskId, List<Long> staffIds) throws SQLException {
        String sql = "INSERT INTO task_assignments (task_id, staff_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Long staffId : staffIds) {
                stmt.setLong(1, taskId);
                stmt.setLong(2, staffId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    private void deleteAssignments(Connection conn, Long taskId) throws SQLException {
        String sql = "DELETE FROM task_assignments WHERE task_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Valida que todos los staffIds pertenezcan al mismo building
     */
    public void validateStaffBelongsToBuilding(Long buildingId, List<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return; // No hay staff para validar
        }
        
        // Verificar que el building existe
        String checkBuildingSql = "SELECT COUNT(*) as count FROM buildings WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkBuildingSql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("count") == 0) {
                    throw new RepositoryException("Building no encontrado: " + buildingId);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error verificando building", e);
        }
        
        // Construir la consulta con placeholders seguros para validar staff
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) as count FROM staff WHERE id IN (");
        for (int i = 0; i < staffIds.size(); i++) {
            if (i > 0) sqlBuilder.append(",");
            sqlBuilder.append("?");
        }
        sqlBuilder.append(") AND building_id = ? AND active = TRUE");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            for (Long staffId : staffIds) {
                stmt.setLong(paramIndex++, staffId);
            }
            stmt.setLong(paramIndex, buildingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count != staffIds.size()) {
                        throw new RepositoryException(
                            String.format("Algunos miembros del personal no pertenecen al mismo building. Esperados: %d, Encontrados: %d", 
                                staffIds.size(), count));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error validando staff del building", e);
        }
    }

    public record TaskResponse(
            Long id,
            Long buildingId,
            String title,
            String description,
            Long assigneeId, // Mantenido para compatibilidad hacia atrás
            List<Long> assigneeIds, // Lista de IDs del personal asignado
            String status,
            String priority,
            LocalDateTime dueDate,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {}
}
