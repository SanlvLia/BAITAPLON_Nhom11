package backends.server.DAO;

import backends.server.database.RequestLog;
import backends.common.Extra.IdGenerator;
import backends.common.messages.Common.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static backends.server.database.RequestLog.STATUS_PENDING;

public class RequestLogDAOImpl implements InterRequestDAO {
    // 1. Singleton Instance
    private static RequestLogDAOImpl instance;

    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATA_DIRECTORY.resolve("request_log.db");

    // 2. Private Constructor (Khởi tạo DB và Table)
    private RequestLogDAOImpl() {
        try {
            if (Files.notExists(DATA_DIRECTORY)) {
                Files.createDirectories(DATA_DIRECTORY);
            }
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS request_log (
                        request_id TEXT PRIMARY KEY,
                        id_user TEXT,
                        request_type TEXT,
                        request_info TEXT,
                        send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        selected BOOLEAN,
                        status TEXT DEFAULT 'PENDING'
                    )
                """);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Không thể khởi tạo RequestLog Database", e);
        }
    }

    public static synchronized RequestLogDAOImpl getInstance() {
        if (instance == null) {
            instance = new RequestLogDAOImpl();
        }
        return instance;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
    //========================================================//
    // --- THỰC THI CÁC PHƯƠNG THỨC TỪ INTERFACE ---
    //===========================================================//

    @Override
    public String getStatusById(String id) {
        String sql = "SELECT status FROM request_log WHERE request_id = ?";
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
        // Tự động set selected = false khi cập nhật status (giống logic cũ của bạn)
        String sql = "UPDATE request_log SET status = ?, selected = false WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void updateRequestsStatus(List<String> itemIds, String status) throws IOException {
        String sql = "UPDATE request_log SET status = ?, selected = false WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String id : itemIds) {
                stmt.setString(1, status);
                stmt.setString(2, id);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<RequestLog.RequestRecord> getRequestsByStatus(String status) throws IOException {
        String sql = "SELECT * FROM request_log WHERE status = ? ORDER BY send_at ASC";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                List<RequestLog.RequestRecord> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(new RequestLog.RequestRecord(
                            rs.getString("request_id"), rs.getString("id_user"),
                            rs.getString("request_type"), rs.getString("request_info"),
                            rs.getString("send_at"), rs.getBoolean("selected"),
                            rs.getString("status")
                    ));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void removeByRequestId(String requestId) throws IOException {
        String sql = "DELETE FROM request_log WHERE request_id = ?";
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
    @Override
    public List<RequestLog.RequestRecord> getRequestsByType(String requestType) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, id_user, request_type, request_info ,send_at, selected, status
                     FROM request_log
                     WHERE request_type = ? AND status = ?
                     ORDER BY send_at ASC
                     """)) {
            statement.setString(1, requestType);
            statement.setString(2, STATUS_PENDING);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RequestLog.RequestRecord> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(new RequestLog.RequestRecord(
                            resultSet.getString("request_id"),
                            resultSet.getString("id_user"),
                            resultSet.getString("request_type"),
                            resultSet.getString("request_info"),
                            resultSet.getString("send_at"),
                            resultSet.getBoolean("selected"),
                            resultSet.getString("status")
                    ));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay danh sach request", e);
        }
    }

    // --- CÁC HÀM ĐẶC THÙ (Cần giữ lại từ class cũ) ---

    public static String save_request(Message message) throws  IOException {
        String requestId = "REQ" + IdGenerator.nextId();
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO request_log (request_id, id_user , request_type , request_info, selected, status ) VALUES (?, ? , ? , ? , ?, ?)""")
        ){
            statement.setString(1, requestId);
            statement.setString(2,message.Id_user);
            statement.setString(3,message.messageType);
            statement.setString(4, message.payloadJson);
            statement.setBoolean(5,false);
            statement.setString(6, STATUS_PENDING);
            statement.executeUpdate();
            return requestId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSelected(String requestId, boolean selected) {
        String sql = "UPDATE request_log SET selected = ? WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, selected);
            stmt.setString(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
