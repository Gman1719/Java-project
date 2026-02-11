package dao;

import models.Attendance;
import utils.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AttendanceDAO {

    /**
     * UPDATED: Now retrieves 'attendance_type' directly for the colLeaveType.
     */
    public List<Attendance> getFilteredRecords(LocalDate date, String name, String status) {
        List<Attendance> records = new ArrayList<>();
        
        String sql = "SELECT a.attend_id, u.first_name, u.last_name, a.attendance_date, a.status, a.remarks, a.attendance_type " +
                     "FROM attendance a " +
                     "JOIN employees e ON a.emp_id = e.emp_id " +
                     "JOIN users u ON e.user_id = u.user_id WHERE 1=1";

        if (date != null) sql += " AND a.attendance_date = ?";
        if (name != null && !name.isEmpty()) sql += " AND CONCAT(u.first_name, ' ', u.last_name) LIKE ?";
        if (status != null && !status.equals("All Statuses")) sql += " AND a.status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (date != null) ps.setDate(paramIndex++, Date.valueOf(date));
            if (name != null && !name.isEmpty()) ps.setString(paramIndex++, "%" + name + "%");
            if (status != null && !status.equals("All Statuses")) ps.setString(paramIndex++, status);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Pass the 'attendance_type' column from DB to the model constructor
                Attendance att = new Attendance(
                    rs.getInt("attend_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getDate("attendance_date").toLocalDate(),
                    rs.getString("status"),
                    rs.getString("attendance_type") == null ? "Regular" : rs.getString("attendance_type"),
                    rs.getString("remarks") == null ? "" : rs.getString("remarks")
                );
                records.add(att);
            }
        } catch (SQLException e) {
            System.err.println("Database Error (Fetch): " + e.getMessage());
        }
        return records;
    }

    /**
     * UPDATED: Now accepts and updates the 'type' column.
     */
    public void updateAttendance(int id, LocalDate date, String status, String type, String remarks) {
        String sql = "UPDATE attendance SET attendance_date = ?, status = ?, attendance_type = ?, remarks = ? WHERE attend_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, status);
            ps.setString(3, type); // Fills Type column
            ps.setString(4, remarks);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) { 
            System.err.println("Database Error (Update): " + e.getMessage());
        }
    }

    /**
     * UPDATED: Now accepts and inserts the 'type' column.
     */
    public boolean saveAttendance(String fullName, LocalDate date, String status, String type, String remarks) {
        String sql = "INSERT INTO attendance (emp_id, attendance_date, status, attendance_type, remarks) " +
                     "SELECT e.emp_id, ?, ?, ?, ? FROM employees e " +
                     "JOIN users u ON e.user_id = u.user_id " +
                     "WHERE CONCAT(u.first_name, ' ', last_name) = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, status);
            ps.setString(3, type); // Fills Type column
            ps.setString(4, remarks);
            ps.setString(5, fullName);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Database Error (Save): " + e.getMessage());
            return false;
        }
    }

    
    public ObservableList<AttendanceRecord> getMonthlyAttendance(int empId, YearMonth month) {

        ObservableList<AttendanceRecord> list = FXCollections.observableArrayList();

        String sql = """
            SELECT attendance_date, time_in, time_out, status, remarks
            FROM attendance
            WHERE emp_id = ?
              AND MONTH(attendance_date) = ?
              AND YEAR(attendance_date) = ?
            ORDER BY attendance_date
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, month.getMonthValue());
            ps.setInt(3, month.getYear());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AttendanceRecord ar = new AttendanceRecord();
                ar.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
                ar.setClockInTime(
                    rs.getTime("time_in") != null ? rs.getTime("time_in").toLocalTime() : null
                );
                ar.setClockOutTime(
                    rs.getTime("time_out") != null ? rs.getTime("time_out").toLocalTime() : null
                );
                ar.setStatus(rs.getString("status"));
                ar.setRemarks(rs.getString("remarks"));
                list.add(ar);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ObservableList<String> getAllEmployeeNames() {
        ObservableList<String> names = FXCollections.observableArrayList();
        String sql = "SELECT CONCAT(u.first_name, ' ', u.last_name) as full_name " +
                     "FROM users u JOIN employees e ON u.user_id = e.user_id " +
                     "WHERE u.status = 'Active' ORDER BY full_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("full_name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return names;
    }
}