package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private static final String URL = "jdbc:mysql://localhost:3306/payroll_system";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Total Active Employees based on the 'employees' table status.
     */
    public int getTotalActiveEmployees() throws SQLException {
        final String SQL = "SELECT COUNT(*) FROM employees WHERE status = 'Active'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Pending Leave Requests from leave_requests table.
     */
    public int getPendingLeaveRequestsCount() throws SQLException {
        final String SQL = "SELECT COUNT(*) FROM leave_requests WHERE status = 'Pending'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Sum of all pending secondary requests (Bank, Advance, Reimbursements).
     */
    public int getPendingCorrectionRequestsCount() throws SQLException {
        final String SQL = "SELECT (" +
                "(SELECT COUNT(*) FROM bank_requests WHERE status = 'Pending') + " +
                "(SELECT COUNT(*) FROM salary_advance_requests WHERE status = 'Pending') + " +
                "(SELECT COUNT(*) FROM reimbursements WHERE status = 'Pending')" +
                ") AS total_pending";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total_pending") : 0;
        }
    }

    /**
     * Employees marked as 'Leave' on the current system date.
     */
    public int getEmployeesOnLeaveTodayCount() throws SQLException {
        // Updated to use your new column name: attendance_date
        final String SQL = "SELECT COUNT(DISTINCT emp_id) FROM attendance WHERE status = 'Leave' AND attendance_date = CURDATE()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Unified list of latest pending items.
     */
    public List<String[]> getLatestPendingRequests(int limit) throws SQLException {
        List<String[]> requests = new ArrayList<>();
        final String SQL = 
            "(SELECT 'Leave' as type, CONCAT(u.first_name, ' ', u.last_name) as name, lr.requested_on as req_date, lr.reason as details " +
            " FROM leave_requests lr JOIN employees e ON lr.emp_id = e.emp_id JOIN users u ON e.user_id = u.user_id WHERE lr.status = 'Pending') " +
            "UNION ALL " +
            "(SELECT 'Bank Change' as type, CONCAT(u.first_name, ' ', u.last_name) as name, br.request_date as req_date, CONCAT('New Acct: ', br.new_account) as details " +
            " FROM bank_requests br JOIN employees e ON br.emp_id = e.emp_id JOIN users u ON e.user_id = u.user_id WHERE br.status = 'Pending') " +
            "UNION ALL " +
            "(SELECT 'Advance' as type, CONCAT(u.first_name, ' ', u.last_name) as name, sar.request_date as req_date, CONCAT('Amt: ', sar.amount) as details " +
            " FROM salary_advance_requests sar JOIN employees e ON sar.emp_id = e.emp_id JOIN users u ON e.user_id = u.user_id WHERE sar.status = 'Pending') " +
            "ORDER BY req_date DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(new String[]{
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getString("req_date").substring(0, 10),
                        rs.getString("details")
                    });
                }
            }
        }
        return requests;
    }

    /**
     * Distribution of employees across departments for the Pie Chart.
     */
    public Map<String, Integer> getDepartmentDistribution() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT d.dept_name, COUNT(e.emp_id) as count " +
                     "FROM departments d " +
                     "LEFT JOIN employees e ON d.dept_id = e.dept_id " +
                     "GROUP BY d.dept_name";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("dept_name"), rs.getInt("count"));
            }
        }
        return stats;
    }
}