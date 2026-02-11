package services;

import models.Employee;
import utils.DBConnection; 
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeService {

    /**
     * Retrieves a list of ALL employees (Active and Inactive) to support status filtering.
     * Maps to Constructor 5 in models.Employee.
     */
	
	
	public void savePayroll(int empId, String month, int year, double base, double allow, double deduct, double tax) throws SQLException {
	    String query = "INSERT INTO payroll (emp_id, month, year, base_salary, allowances, deductions, tax, status) " +
	                   "VALUES (?, ?, ?, ?, ?, ?, ?, 'Generated')";
	    
	    try (Connection conn = DBConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(query)) {
	        pstmt.setInt(1, empId);
	        pstmt.setString(2, month);
	        pstmt.setInt(3, year);
	        pstmt.setDouble(4, base);
	        pstmt.setDouble(5, allow);
	        pstmt.setDouble(6, deduct);
	        pstmt.setDouble(7, tax);
	        pstmt.executeUpdate();
	    }
	}
	public void updateEmployeeDepartment(int empId, String departmentName) throws SQLException {
	    // 1. Get the dept_id for the chosen department name
	    String getIdQuery = "SELECT dept_id FROM departments WHERE dept_name = ?";
	    // 2. Update the employee record using emp_id (not employee_id)
	    String updateQuery = "UPDATE employees SET dept_id = ? WHERE emp_id = ?";

	    try (Connection conn = DBConnection.getConnection()) {
	        int deptId = -1;

	        // Step 1: Find the ID
	        try (PreparedStatement pstmt1 = conn.prepareStatement(getIdQuery)) {
	            pstmt1.setString(1, departmentName);
	            ResultSet rs = pstmt1.executeQuery();
	            if (rs.next()) {
	                deptId = rs.getInt("dept_id");
	            }
	        }

	        // Step 2: Update the Employee
	        if (deptId != -1) {
	            try (PreparedStatement pstmt2 = conn.prepareStatement(updateQuery)) {
	                pstmt2.setInt(1, deptId);
	                pstmt2.setInt(2, empId);
	                pstmt2.executeUpdate();
	            }
	        } else {
	            throw new SQLException("Department name not found in database.");
	        }
	    }
	}
    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> employees = new ArrayList<>();
        
        // Use LEFT JOIN for departments to ensure employees without a department still show up
        String sql = "SELECT e.emp_id, u.user_id, u.username, u.first_name, u.last_name, " +
                     "e.phone, e.email, d.dept_name, e.position, e.status, e.date_joined, " +
                     "e.gender, e.bank_account " + 
                     "FROM employees e " +
                     "JOIN users u ON e.user_id = u.user_id " +
                     "LEFT JOIN departments d ON e.dept_id = d.dept_id " +
                     "ORDER BY e.emp_id";

        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int empId = rs.getInt("emp_id");
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String department = rs.getString("dept_name");
                String position = rs.getString("position");
                String status = rs.getString("status");
                LocalDate dateJoined = rs.getDate("date_joined") != null ? rs.getDate("date_joined").toLocalDate() : null;
                String gender = rs.getString("gender");
                String bankAccount = rs.getString("bank_account");

                Employee employee = new Employee(empId, userId, username, firstName, lastName, 
                                                 phone, email, department, position, status, dateJoined,
                                                 gender, bankAccount);
                employees.add(employee);
            }
        }
        return employees;
    }

    /**
     * Legacy method name kept for controller compatibility.
     */
    public List<Employee> getAllActiveEmployees() throws SQLException {
        return getAllEmployees();
    }
    
    // --- PAYROLL GENERATION METHODS ---

    /**
     * Calculates payroll based on the 'employees' and 'settings' tables in your SQL schema.
     */
    public double generatePayroll(int employeeId) throws SQLException {
        double baseSalary = 0.0;
        double taxRate = 0.0;
        
        // 1. Fetch Salary from 'employees' and Tax Rate from 'settings'
        String sqlData = "SELECT e.salary, s.tax_rate FROM employees e, settings s WHERE e.emp_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlData)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    baseSalary = rs.getDouble("salary");
                    taxRate = rs.getDouble("tax_rate") / 100.0; // Convert percentage to decimal
                    
                    double taxAmount = baseSalary * taxRate;
                    double netPay = baseSalary - taxAmount;

                    // 2. Insert into the 'payroll' table defined in your SQL script
                    String currentMonth = LocalDate.now().getMonth().name();
                    int currentYear = LocalDate.now().getYear();

                    String sqlInsert = "INSERT INTO payroll (emp_id, month, year, base_salary, tax, status) " +
                                     "VALUES (?, ?, ?, ?, ?, 'Processed') " +
                                     "ON DUPLICATE KEY UPDATE base_salary = VALUES(base_salary), tax = VALUES(tax)";
                    
                    try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                        stmtInsert.setInt(1, employeeId);
                        stmtInsert.setString(2, currentMonth);
                        stmtInsert.setInt(3, currentYear);
                        stmtInsert.setDouble(4, baseSalary);
                        stmtInsert.setDouble(5, taxAmount);
                        stmtInsert.executeUpdate();
                    }
                    
                    return netPay;
                } else {
                    throw new SQLException("Employee not found for payroll generation.");
                }
            }
        }
    }

    /**
     * Terminate Employee: Updates both 'employees' and 'users' tables to 'Inactive'.
     */
    public void terminateEmployee(int employeeId) throws SQLException {
        String sqlEmployee = "UPDATE employees SET status = 'Inactive' WHERE emp_id = ?";
        String sqlUser = "UPDATE users u JOIN employees e ON u.user_id = e.user_id SET u.status = 'Inactive' WHERE e.emp_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmtEmp = conn.prepareStatement(sqlEmployee)) {
                stmtEmp.setInt(1, employeeId);
                stmtEmp.executeUpdate();
            }
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {
                stmtUser.setInt(1, employeeId);
                stmtUser.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Fetches dynamic department list from the 'departments' table.
     */
    public ObservableList<String> getAllDepartmentNames() throws SQLException {
        ObservableList<String> departments = FXCollections.observableArrayList();
        String sql = "SELECT dept_name FROM departments ORDER BY dept_name";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(rs.getString("dept_name"));
            }
        }
        return departments;
    }

    /**
     * Hardcoded list of employment types for the UI filter.
     */
    public ObservableList<String> getAllEmploymentTypes() {
        return FXCollections.observableArrayList("Full-Time", "Part-Time", "Contract", "Internship");
    }
}