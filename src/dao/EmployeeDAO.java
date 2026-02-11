package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.AttendanceRecord;
import models.LeaveRequest;
import models.Payslip;
import utils.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public class EmployeeDAO {

    /* ===============================
       ATTENDANCE (ALL / BY MONTH)
       =============================== */

    /**
     * Fetch all attendance for an employee.
     */
    public ObservableList<AttendanceRecord> getAllAttendance(int empId) {
        ObservableList<AttendanceRecord> list = FXCollections.observableArrayList();
        String sql = """
            SELECT CONCAT(u.first_name, ' ', u.last_name) AS employee_name,
                   a.attendance_date,
                   a.status,
                   a.attendance_type,
                   a.remarks
            FROM attendance a
            JOIN employees e ON a.emp_id = e.emp_id
            JOIN users u ON e.user_id = u.user_id
            WHERE a.emp_id = ?
            ORDER BY a.attendance_date DESC
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new AttendanceRecord(
                    rs.getString("employee_name"),
                    rs.getDate("attendance_date").toLocalDate(),
                    rs.getString("status"),
                    rs.getString("attendance_type"),
                    rs.getString("remarks")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching attendance: " + e.getMessage());
        }

        return list;
    }

    /**
     * Fetch monthly attendance for an employee.
     */
    public ObservableList<AttendanceRecord> getMonthlyAttendance(int empId, YearMonth month) {
        ObservableList<AttendanceRecord> list = FXCollections.observableArrayList();
        String sql = """
            SELECT CONCAT(u.first_name, ' ', u.last_name) AS employee_name,
                   a.attendance_date,
                   a.status,
                   a.attendance_type,
                   a.remarks
            FROM attendance a
            JOIN employees e ON a.emp_id = e.emp_id
            JOIN users u ON e.user_id = u.user_id
            WHERE a.emp_id = ? AND MONTH(a.attendance_date) = ? AND YEAR(a.attendance_date) = ?
            ORDER BY a.attendance_date
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, month.getMonthValue());
            ps.setInt(3, month.getYear());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AttendanceRecord(
                    rs.getString("employee_name"),
                    rs.getDate("attendance_date").toLocalDate(),
                    rs.getString("status"),
                    rs.getString("attendance_type"),
                    rs.getString("remarks")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching monthly attendance: " + e.getMessage());
        }

        return list;
    }

    /* ===============================
       LEAVE REQUESTS
       =============================== */

    public ObservableList<LeaveRequest> getEmployeeLeaveRequests(int empId) {
        ObservableList<LeaveRequest> list = FXCollections.observableArrayList();
        String sql = """
            SELECT leave_type, start_date, end_date, reason, status
            FROM leave_requests
            WHERE emp_id = ?
            ORDER BY requested_on DESC
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new LeaveRequest(
                    rs.getString("leave_type"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate(),
                    rs.getString("reason"),
                    rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error loading leave requests: " + e.getMessage());
        }

        return list;
    }

    /* ===============================
       OTHER REQUESTS (BANK + ADVANCE)
       =============================== */

    public ObservableList<String> getEmployeeOtherRequestsStatus(int empId) {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = """
            SELECT 'Bank Change' AS type, status, request_date AS dt
            FROM bank_requests
            WHERE emp_id = ?
            UNION ALL
            SELECT 'Salary Advance', status, request_date
            FROM salary_advance_requests
            WHERE emp_id = ?
            ORDER BY dt DESC
            LIMIT 10
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, empId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("type") + " - " + rs.getString("status"));
            }

        } catch (SQLException e) {
            System.err.println("Error loading other requests: " + e.getMessage());
        }

        return list;
    }

    /* ===============================
       PAYSLIP / SALARY
       =============================== */

    public Optional<Payslip> getLatestPayslip(int empId) {
        String sql = """
            SELECT p.payroll_id, p.base_salary, p.allowances, p.deductions, p.tax, p.net_salary,
                   p.month, p.year, p.generated_on,
                   CONCAT(u.first_name, ' ', u.last_name) AS employee_name
            FROM payroll p
            JOIN employees e ON p.emp_id = e.emp_id
            JOIN users u ON e.user_id = u.user_id
            WHERE p.emp_id = ?
            ORDER BY p.generated_on DESC
            LIMIT 1
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Payslip payslip = new Payslip(
                    rs.getInt("payroll_id"),
                    rs.getInt("payroll_id"),
                    empId,
                    rs.getString("employee_name"),
                    rs.getString("month"),
                    rs.getInt("year"),
                    rs.getBigDecimal("base_salary"),
                    rs.getBigDecimal("allowances"),
                    rs.getBigDecimal("deductions"),
                    rs.getBigDecimal("tax"),
                    rs.getBigDecimal("net_salary"),
                    rs.getTimestamp("generated_on")
                );
                return Optional.of(payslip);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching latest payslip: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Get remaining leave balance for the employee
     */
    public int getRemainingLeaveDays(int empId) {
        String sql = "SELECT remaining_days FROM leave_balance WHERE emp_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("remaining_days");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching leave balance: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get latest net salary for the employee
     */
    public BigDecimal getLatestNetSalary(int empId) {
        String sql = "SELECT net_salary FROM payroll WHERE emp_id = ? ORDER BY generated_on DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("net_salary");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching latest net salary: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
