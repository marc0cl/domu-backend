package com.domu.database;

import com.domu.dto.MarketItemResponse;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarketRepository {

    private final DataSource dataSource;

    @Inject
    public MarketRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<MarketItemResponse> findAllByBuilding(Long buildingId, Long categoryId, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT i.*, c.name as category_name, u.first_name, u.last_name
                FROM market_item i
                JOIN market_category c ON i.category_id = c.id
                JOIN users u ON i.user_id = u.id
                WHERE i.building_id = ?
                """);
        
        List<Object> params = new ArrayList<>();
        params.add(buildingId);

        if (categoryId != null) {
            sql.append(" AND i.category_id = ?");
            params.add(categoryId);
        }
        if (status != null) {
            sql.append(" AND i.status = ?");
            params.add(status);
        } else {
            sql.append(" AND i.status = 'AVAILABLE'");
        }

        sql.append(" ORDER BY i.created_at DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<MarketItemResponse> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapItem(rs, conn));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listando items del mercado", e);
        }
    }

    public Long insertItem(Long userId, Long buildingId, Long categoryId, String title, String description, Double price, String originalPriceLink) {
        String sql = """
                INSERT INTO market_item (user_id, building_id, category_id, title, description, price, original_price_link)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, buildingId);
            stmt.setLong(3, categoryId);
            stmt.setString(4, title);
            stmt.setString(5, description);
            stmt.setDouble(6, price);
            stmt.setString(7, originalPriceLink);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new RepositoryException("Error obteniendo ID del item creado");
        } catch (SQLException e) {
            throw new RepositoryException("Error insertando item en el mercado", e);
        }
    }

    public void updateBoxMetadata(Long itemId, String folderId, String mainImageUrl) {
        String sql = "UPDATE market_item SET box_folder_id = ?, main_image_url = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, folderId);
            stmt.setString(2, mainImageUrl);
            stmt.setLong(3, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando metadatos de Box en item", e);
        }
    }

    public void updateItem(Long itemId, Long userId, Long categoryId, String title, String description, Double price) {
        String sql = "UPDATE market_item SET category_id = ?, title = ?, description = ?, price = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, categoryId);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setDouble(4, price);
            stmt.setLong(5, itemId);
            stmt.setLong(6, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando item del mercado", e);
        }
    }

    public void deleteItem(Long itemId, Long userId) {
        String sql = "DELETE FROM market_item WHERE id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando item del mercado", e);
        }
    }

    public void insertImage(Long itemId, String url, String boxFileId, boolean isMain) {
        String sql = "INSERT INTO market_item_image (item_id, url, box_file_id, is_main) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.setString(2, url);
            stmt.setString(3, boxFileId);
            stmt.setBoolean(4, isMain);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error insertando imagen del mercado", e);
        }
    }

    public void deleteImage(Long itemId, String url) {
        String sql = "DELETE FROM market_item_image WHERE item_id = ? AND url = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.setString(2, url);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando imagen del mercado", e);
        }
    }

    private List<String> findImagesByItem(Long itemId, Connection conn) throws SQLException {
        String sql = "SELECT url FROM market_item_image WHERE item_id = ? ORDER BY id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> urls = new ArrayList<>();
                while (rs.next()) urls.add(rs.getString("url"));
                return urls;
            }
        }
    }

    private MarketItemResponse mapItem(ResultSet rs, Connection conn) throws SQLException {
        Long itemId = rs.getLong("id");
        return MarketItemResponse.builder()
                .id(itemId)
                .userId(rs.getLong("user_id"))
                .sellerName(rs.getString("first_name") + " " + rs.getString("last_name"))
                .categoryId(rs.getLong("category_id"))
                .categoryName(rs.getString("category_name"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .price(rs.getDouble("price"))
                .originalPriceLink(rs.getString("original_price_link"))
                .status(rs.getString("status"))
                .mainImageUrl(rs.getString("main_image_url"))
                .imageUrls(findImagesByItem(itemId, conn))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}