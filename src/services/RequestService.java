package services;

import models.EmployeeRequest;
import utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestService {

    public List<EmployeeRequest> getAllRequests() {
        List<EmployeeRequest> list = new ArrayList<>();
        
        // Match columns exactly to your provided SQL schema
        // Note: I added '' as hr_comment because your base CREATE TABLE didn't show hr_comment 
        // in all tables, but you can add them later.
        String sql = 
            "SELECT leave_id AS id, 'Leave' AS type, emp_id, status, requested_on AS sub_date, reason AS justification, " +
            "start_date, end_date, NULL AS old_val, NULL AS new_val FROM leave_requests " +
            "UNION ALL " +
            "SELECT request_id AS id, 'Bank Change' AS type, emp_id, status, request_date AS sub_date, 'Account Update' AS justification, " +
            "request_date, request_date, old_account AS old_val, new_account AS new_val FROM bank_requests " +
            "UNION ALL " +
            "SELECT advance_id AS id, 'Salary Advance' AS type, emp_id, status, request_date AS sub_date, reason AS justification, " +
            "request_date, request_date, amount AS old_val, NULL AS new_val FROM salary_advance_requests " +
            "UNION ALL " +
            "SELECT reimb_id AS id, 'Reimbursement' AS type, emp_id, status, request_date AS sub_date, reason AS justification, " +
            "request_date, request_date, amount AS old_val, NULL AS new_val FROM reimbursements " +
            "ORDER BY sub_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            
            while (rs.next()) {
                EmployeeRequest req = new EmployeeRequest();
                req.setRequestId(rs.getInt("id"));
                req.setEmployeeId(rs.getInt("emp_id"));
                req.setRequestType(rs.getString("type"));
                req.setStatus(rs.getString("status"));
                
                // Use empty string for HR Comment if it's missing in SQL for now
                req.setHrComment(""); 
                
                req.setOldValue(rs.getString("old_val"));
                req.setNewValue(rs.getString("new_val"));
                
                Timestamp ts = rs.getTimestamp("sub_date");
                if (ts != null) req.setSubmittedDate(ts.toLocalDateTime().toLocalDate());
                
                Date sDate = rs.getDate("start_date");
                if (sDate != null) req.setStartDate(sDate.toLocalDate());
                
                Date eDate = rs.getDate("end_date");
                if (eDate != null) req.setEndDate(eDate.toLocalDate());

                req.setJustification(rs.getString("justification") == null ? "" : rs.getString("justification"));
                req.setEmployeeName(fetchEmployeeFullName(conn, rs.getInt("emp_id")));
                
                list.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String fetchEmployeeFullName(Connection conn, int empId) throws SQLException {
        // Updated to match your USERS and EMPLOYEES join logic
        String nameSql = "SELECT u.first_name, u.last_name FROM users u " +
                         "JOIN employees e ON u.user_id = e.user_id WHERE e.emp_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(nameSql)) {
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name") + " (ID: " + empId + ")";
            }
        }
        return "Unknown Employee (" + empId + ")";
    }

    public boolean updateRequest(EmployeeRequest request) {
        String tableName = "";
        String idColumn = "";

        switch (request.getRequestType()) {
            case "Leave" -> { tableName = "leave_requests"; idColumn = "leave_id"; }
            case "Bank Change" -> { tableName = "bank_requests"; idColumn = "request_id"; }
            case "Salary Advance" -> { tableName = "salary_advance_requests"; idColumn = "advance_id"; }
            case "Reimbursement" -> { tableName = "reimbursements"; idColumn = "reimb_id"; }
            default -> { return false; }
        }

        // Note: Your current SQL schema (tables 8, 12, 13, 14) does NOT have an 'hr_comment' column.
        // I am updating ONLY the status to prevent a SQL error.
        String sql = "UPDATE " + tableName + " SET status = ? WHERE " + idColumn + " = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, request.getStatus());
            pst.setInt(2, request.getRequestId());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}