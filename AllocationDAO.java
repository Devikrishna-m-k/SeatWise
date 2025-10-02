import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AllocationDAO {

    public static void clearAllocationsForExam(Connection conn, String examSlotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Allocation WHERE exam_slot_id = ?")) {
            ps.setString(1, examSlotId);
            ps.executeUpdate();
        }
    }

    public static void saveAllocationBatch(Connection conn, List<AllocationRecord> allocations) throws SQLException {
        String sql = "INSERT INTO Allocation(student_id, room_id, seat_no, exam_slot_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (AllocationRecord r : allocations) {
                ps.setString(1, r.getStudentId());
                ps.setString(2, r.getRoomId());
                ps.setInt(3, r.getSeatNo());
                ps.setString(4, r.getExamSlotId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static List<AllocationRecord> fetchAllAllocations(Connection conn) throws SQLException {
        List<AllocationRecord> list = new ArrayList<>();
        String sql = "SELECT a.student_id, s.name, s.branch, a.room_id, a.seat_no, a.exam_slot_id " +
                     "FROM Allocation a JOIN Student s ON a.student_id = s.student_id ORDER BY a.room_id, a.seat_no";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AllocationRecord(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5), rs.getString(6)
                ));
            }
        }
        return list;
    }

    public static AllocationRecord fetchAllocationForStudent(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT a.student_id, s.name, s.branch, a.room_id, a.seat_no, a.exam_slot_id " +
                     "FROM Allocation a JOIN Student s ON a.student_id = s.student_id WHERE a.student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AllocationRecord(
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getInt(5), rs.getString(6)
                    );
                }
            }
        }
        return null;
    }

    public static void saveAdminWarning(Connection conn, String text) throws SQLException {
        String sql = "INSERT INTO AdminWarning(warning_text) VALUES(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            ps.executeUpdate();
        }
    }
}
