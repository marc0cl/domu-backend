package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class RoleRepository {

    private final DataSource dataSource;

    @Inject
    public RoleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<String> findPermissionsJsonByRoleId(Long roleId) {
        String sql = "SELECT permissions_json FROM roles WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("permissions_json"));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching permissions for role " + roleId, e);
        }
        return Optional.empty();
    }
}
