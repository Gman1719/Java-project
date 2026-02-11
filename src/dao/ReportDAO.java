package dao;

import models.Report;
import utils.DBConnection; // Assumed utility class
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays; // For error handling, though less critical now

public class ReportDAO {
    
    /**
     * Helper method to execute a query and map it to a list of generic Report objects.
     */
    private List<Report> executeReportQuery(String sql) {
        List<Report> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String c1 = rs.getString(1);
                String c2 = rs.getString(2);
                String c3 = (rs.getMetaData().getColumnCount() >= 3) ? rs.getString(3) : "";
                String c4 = (rs.getMetaData().getColumnCount() >= 4) ? rs.getString(4) : "";
                
                results.add(new Report(c1, c2, c3, c4));
            }
        } catch (SQLException e) {
            System.err.println("Error running report query: " + e.getMessage());
            e.printStackTrace();
            // Return empty list on failure, allowing the controller to continue running.
            return results; 
        }
        return results;
    }


    // --- 1. Payroll Summary (FIXED: Uses 'payroll' table) ---

    public List<Report> getPayrollSummaryTableData() {
        // SQL query corrected to use the 'payroll' table, join through 'employees' 
        // to get 'dept_id', and group by 'dept_name'.
        String sql = "SELECT d.dept_name, SUM(p.net_salary), AVG(p.net_salary), COUNT(DISTINCT e.emp_id) " +
                     "FROM payroll p " +
                     "JOIN employees e ON p.emp_id = e.emp_id " +
                     "JOIN departments d ON e.dept_id = d.dept_id " +
                     "GROUP BY d.dept_name";
                     
        List<Report> results = executeReportQuery(sql);
        
        // Simple fallback to ensure something displays if DB is down or tables are incomplete
        if (results.isEmpty()) {
             return Arrays.asList(
                 new Report("No Data (Check DB)", "0.00", "0.00", "0")
             );
        }
        return results;
    }


    // --- 2. Attendance Summary (FIXED: Correct join path) ---

    public List<Report> getAttendanceSummaryTableData() {
        // SQL query corrected to join attendance -> employees -> users to retrieve the employee name
        // MySQL uses CONCAT instead of || for string concatenation
        String sql = "SELECT CONCAT(u.first_name, ' ', u.last_name) AS employee_name, " +
                     "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END), " +
                     "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END), " +
                     "SUM(CASE WHEN a.status = 'Leave' THEN 1 ELSE 0 END) " +
                     "FROM attendance a " +
                     "JOIN employees e ON a.emp_id = e.emp_id " +
                     "JOIN users u ON e.user_id = u.user_id " +
                     "GROUP BY employee_name " +
                     "ORDER BY employee_name";
        return executeReportQuery(sql);
    }


    // --- 3. Requests Summary (FIXED: Uses UNION ALL for multiple request tables) ---

    public List<Report> getRequestsSummaryTableData() {
        // MySQL-compatible query using UNION ALL to aggregate data from multiple request tables
        // Table name corrected from 'leaves' to 'leave_requests'
        String sql = "SELECT 'Leave' AS type, COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) AS approved, " +
                     "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending " +
                     "FROM leave_requests " +
                     "UNION ALL " +
                     "SELECT 'Bank Change' AS type, COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) AS approved, " +
                     "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending " +
                     "FROM bank_requests " +
                     "UNION ALL " +
                     "SELECT 'Salary Advance' AS type, COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) AS approved, " +
                     "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending " +
                     "FROM salary_advance_requests " +
                     "UNION ALL " +
                     "SELECT 'Reimbursement' AS type, COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) AS approved, " +
                     "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending " +
                     "FROM reimbursements";
        return executeReportQuery(sql);
    }
}