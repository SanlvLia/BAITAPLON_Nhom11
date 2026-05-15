package backends.server.DAO;

import backends.common.models.core.Item;
import backends.common.models.items.Art;
import backends.common.models.items.Electronics;
import backends.common.models.items.Vehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAOImpl implements InterRequestDAO {
    // 1. Singleton Instance
    private static InventoryDAOImpl instance;

    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATA_DIRECTORY.resolve("inventory.db");

    // 2. Private Constructor (Thay thế cho constructor Inventory cũ)
    private InventoryDAOImpl() {
        try {
            if (Files.notExists(DATA_DIRECTORY)) {
                Files.createDirectories(DATA_DIRECTORY);
            }
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS inventory (
                        ItemId TEXT PRIMARY KEY,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        price DOUBLE,
                        itemDescription TEXT,
                        request_id TEXT,
                        userId TEXT,
                        status VARCHAR(20),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Không thể khởi tạo Inventory Database", e);
        }
    }

    public static synchronized InventoryDAOImpl getInstance() {
        if (instance == null) {
            instance = new InventoryDAOImpl();
        }
        return instance;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    // --- THỰC THI CÁC PHƯƠNG THỨC TỪ INTERFACE ---

    @Override
    public Item findById(String id) throws IOException {
        String sql = "SELECT ItemId, type, name, price, itemDescription FROM inventory WHERE ItemId = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<Item> getRequestsByStatus(String status) throws IOException {
        String sql = "SELECT ItemId, type, name, price, itemDescription FROM inventory WHERE status = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Item getRequesttoAuction(String status) throws IOException {
        String sql = "SELECT ItemId, type, name, price, itemDescription FROM inventory WHERE status = ? ORDER BY created_at ASC LIMIT 1";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<Item> getRequestsByUserId(String userId) throws IOException {
        String sql = "SELECT ItemId, type, name, price, itemDescription FROM inventory WHERE userId = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getStatusById(String id) {
        String sql = "SELECT status FROM inventory WHERE request_id = ? OR ItemId = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("status");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateRequestStatus(String requestId, String status) throws IOException {
        String sql = "UPDATE inventory SET status = ? WHERE ItemId = ? OR request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, requestId);
            stmt.setString(3, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void removeByRequestId(String requestId) throws IOException {
        String sql = "DELETE FROM inventory WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean existsByRequestId(String requestId) throws IOException {
        String sql = "SELECT 1 FROM inventory WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // --- HÀM RIÊNG CỦA INVENTORY (Không có trong Interface chung) ---

    public void saveItem(Item item, String userId, String requestId) throws IOException {
        String sql = "INSERT INTO inventory(ItemId, type, name, price, itemDescription, request_id, userId, status) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getType());
            stmt.setString(3, item.getName());
            stmt.setDouble(4, item.getPrices());
            stmt.setString(5, item.getInfo());
            stmt.setString(6, requestId);
            stmt.setString(7, userId);
            stmt.setString(8, "WAITING");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    // Helper method để map ResultSet sang Object (Logic đa hình)
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String itemId = rs.getString("ItemId");
        String type = rs.getString("type");
        String name = rs.getString("name");
        double price = rs.getDouble("price");
        String description = rs.getString("itemDescription");

        return switch (type) {
            case "Electronics" -> new Electronics(itemId, name, price, description);
            case "Art" -> new Art(itemId, name, price, description);
            case "Vehicle" -> new Vehicle(itemId, name, price, description);
            default -> throw new SQLException("Loai san pham khong hop le: " + type);
        };
    }
}