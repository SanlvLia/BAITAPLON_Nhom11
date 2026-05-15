package backends.server.DAO;

import backends.server.database.MyRequest;
import backends.common.messages.Common.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyRequestDAOImpl implements InterRequestDAO {
    // 1. Singleton Instance
    private static MyRequestDAOImpl instance;

    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATA_DIRECTORY.resolve("my_request.db");

    // 2. Private Constructor
    private MyRequestDAOImpl() {
        try {
            if (Files.notExists(DATA_DIRECTORY)) {
                Files.createDirectories(DATA_DIRECTORY);
            }
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS my_request (
                        request_id TEXT PRIMARY KEY,
                        id_user TEXT,
                        request_type TEXT,
                        request_info TEXT,
                        send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        status TEXT
                    )
                """);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Không thể khởi tạo MyRequest Database", e);
        }
    }

    // 3. Singleton Access
    public static synchronized MyRequestDAOImpl getInstance() {
        if (instance == null) {
            instance = new MyRequestDAOImpl();
        }
        return instance;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    //=====================================================================//
    // --- THỰC THI CÁC PHƯƠNG THỨC TỪ INTERFACE ---
    //======================================================================//

    // Ghi chú: Vì interface IterRequestDAO không có hàm save, bạn có thể gọi trực tiếp từ Impl
    // hoặc thêm nó vào Interface nếu muốn đồng bộ hóa hoàn toàn.
    public void saveMyRequest(Message message, String requestId) throws IOException {
        String sql = "INSERT INTO my_request (request_id, id_user, request_type, request_info, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            stmt.setString(2, message.Id_user);
            stmt.setString(3, message.messageType);
            stmt.setString(4, message.payloadJson);
            stmt.setString(5, "PENDING");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Lỗi lưu my_request", e);
        }
    }

    @Override
    public String getStatusById(String id) {
        String sql = "SELECT status FROM my_request WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("status");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateRequestStatus(String requestId, String status) throws IOException {
        String sql = "UPDATE my_request SET status = ? WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Lỗi cập nhật status", e);
        }
    }

    // Ở đây ta cast List<?> về List<MyRequest.RequestRecord> trong lúc sử dụng ở Controller
    @Override
    public List<MyRequest.RequestRecord> getRequestsByStatus(String status) throws IOException {
        String sql = "SELECT * FROM my_request WHERE status = ? ORDER BY send_at ASC";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                List<MyRequest.RequestRecord> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(new MyRequest.RequestRecord(
                            rs.getString("request_id"), rs.getString("id_user"),
                            rs.getString("request_type"), rs.getString("request_info"),
                            rs.getString("send_at"), rs.getString("status")
                    ));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteRequest(String request_id) throws IOException {
        String sql = "DELETE FROM my_request WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request_id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Lỗi xóa request", e);
        }
    }
    public boolean existsByRequestId(String requestId) throws IOException {
        String sql = "SELECT 1 FROM request_log WHERE request_id = ?";
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
}