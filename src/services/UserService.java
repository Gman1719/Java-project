// File: services/UserService.java

package services; 

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList; // Added for the new method
import java.util.List;      // Added for the new method

import models.Employee; 

// ⚠️ WARNING: This entire class stores and compares passwords in plaintext.
// This is for demonstration based on your request, but is highly insecure.

public class UserService {

    // Database Credentials (Based on your input)
	private static final String URL = "jdbc:mysql://localhost:3306/payroll_system";
	private static final String USER = "root";
	private static final String PASSWORD = "root";
    
    // ----------------------------------------------------------------------
    // Database Connection Helper
    // ----------------------------------------------------------------------

    private Connection getConnection() throws SQLException {
        // NOTE: The MySQL driver MUST be included in your project dependencies.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    // ----------------------------------------------------------------------
    // JDBC Methods for Authentication (Password Management) - (EXISTING CODE)
    // ----------------------------------------------------------------------

    /**
     * Verifies the user's current plain text password against the database,
     * using .trim() to eliminate potential whitespace mismatches.
     */
    public boolean verifyCurrentPasswordPlaintext(int userId, String currentPassword) throws SQLException {
        final String SQL = "SELECT password FROM users WHERE user_id = ?"; 
        
        // --- DEBUGGING --- (KEPT AS REQUESTED)
        System.out.println("DEBUG: User ID being checked: " + userId);
        System.out.println("DEBUG: Current Pass entered (Untrimmed): >" + currentPassword + "<");
        
        // Trim the password entered by the user
        String trimmedCurrentPass = currentPassword.trim();
        System.out.println("DEBUG: Current Pass entered (Trimmed): >" + trimmedCurrentPass + "<");
        // -----------------
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    
                    // --- DEBUGGING --- (KEPT AS REQUESTING)
                    System.out.println("DEBUG: Stored Pass in DB (Untrimmed): >" + storedPassword + "<");
                    // -----------------

                    // Trim the password retrieved from the database
                    String trimmedStoredPass = storedPassword.trim();
                    System.out.println("DEBUG: Stored Pass in DB (Trimmed): >" + trimmedStoredPass + "<");

                    // 🚨 INSECURE COMPARISON: Comparing trimmed plain text passwords
                    return trimmedStoredPass.equals(trimmedCurrentPass);
                }
            }
        }
        return false; // User not found or connection failed
    }

    /**
     * Updates the user's plain text password in the 'users' table.
     */
    public boolean updatePasswordPlaintext(int userId, String newPassword) throws SQLException {
        final String SQL = "UPDATE users SET password = ? WHERE user_id = ?"; 
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setString(1, newPassword); 
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // ----------------------------------------------------------------------
    // JDBC Methods for Profile Management - (EXISTING CODE)
    // ----------------------------------------------------------------------

    /**
     * Retrieves the profile data for a given user ID from the 'users' table 
     * and maps it to the Employee model.
     */
    public Employee getUserProfileById(int userId) throws SQLException {
        // Selecting profile-editable fields from the 'users' table, plus required join fields
        final String SQL = "SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone, " +
                           "u.designation, u.date_of_joining, u.role_id, u.dept_id, " +
                           "r.role_name, d.dept_name, e.salary, e.emp_id " +
                           "FROM users u " +
                           "JOIN roles r ON u.role_id = r.role_id " +
                           "JOIN departments d ON u.dept_id = d.dept_id " +
                           "JOIN employees e ON u.user_id = e.user_id " +
                           "WHERE u.user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Employee emp = new Employee();
                    emp.setUserId(rs.getInt("user_id")); 
                    emp.setEmployeeId(rs.getInt("emp_id")); // from employees table
                    emp.setFirstName(rs.getString("first_name"));
                    emp.setLastName(rs.getString("last_name"));
                    emp.setEmail(rs.getString("email"));
                    emp.setPhone(rs.getString("phone"));
                    emp.setDepartment(rs.getString("dept_name")); // from departments table
                    emp.setPosition(rs.getString("designation")); // from users table
                    emp.setRoleName(rs.getString("role_name"));   // from roles table
                    emp.setRoleId(rs.getInt("role_id"));
                    emp.setSalary(rs.getDouble("salary")); // from employees table

                    // Handle date conversion
                    if (rs.getDate("date_of_joining") != null) {
                        emp.setDateJoined(rs.getDate("date_of_joining").toLocalDate());
                    }
                    
                    return emp; 
                }
            }
        }
        return null; // Profile not found
    }

    /**
     * Updates the profile fields (First Name, Last Name, Email, Phone) in the 'users' table.
     * This method splits the fullName string into first_name and last_name.
     */
    public boolean updateUserProfile(int userId, String fullName, String email, String phone) throws SQLException {
        // Split the Full Name into two parts
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = (parts.length > 1) ? parts[1] : ""; 

        // SQL updates the relevant columns in the 'users' table
        final String SQL = "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = ? WHERE user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // ----------------------------------------------------------------------
    // JDBC Methods for Employee Onboarding (NEW CODE STARTS HERE)
    // ----------------------------------------------------------------------

    /**
     * Retrieves all department names for populating the ComboBox.
     */
    public List<String> getAllDepartmentNames() throws SQLException {
        List<String> departmentNames = new ArrayList<>();
        final String SQL = "SELECT dept_name FROM departments ORDER BY dept_name";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departmentNames.add(rs.getString("dept_name"));
            }
        }
        return departmentNames;
    }

    /**
     * Retrieves the department ID (dept_id) based on the department name.
     */
    public int getDepartmentId(String deptName) throws SQLException {
        final String SQL = "SELECT dept_id FROM departments WHERE dept_name = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setString(1, deptName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                }
            }
        }
        return -1; // Not found
    }

    /**
     * Retrieves the role ID (role_id) based on the role name.
     */
    public int getRoleId(String roleName) throws SQLException {
        final String SQL = "SELECT role_id FROM roles WHERE role_name = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            
            stmt.setString(1, roleName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }
        return -1; // Not found
    }

    /**
     * Creates a new employee record across the 'users' and 'employees' tables 
     * in a single, atomic database transaction.
     * @return The newly created user_id, or -1 on failure.
     */
    public int createNewEmployee(String firstName, String lastName, String email, String gender, 
                                 int deptId, String jobTitle, LocalDate hireDate, double salary, int roleId) throws SQLException {
        
        // 1. Generate a default username and password
        // Appends a random number (0-99) to ensure uniqueness if name is common
        String baseUsername = (firstName.substring(0, 1) + lastName).toLowerCase();
        String username = baseUsername + new java.util.Random().nextInt(100);
        String tempPassword = "password123"; // 🚨 INSECURE: MUST be changed on first login

        int newUserId = -1;
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // --- STEP A: INSERT into USERS TABLE ---
            final String USER_SQL = "INSERT INTO users (username, password, role_id, first_name, last_name, email, phone, dept_id, designation, date_of_joining) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)"; // Phone is NULL initially
            
            try (PreparedStatement stmt = conn.prepareStatement(USER_SQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, username);
                stmt.setString(2, tempPassword);
                stmt.setInt(3, roleId);
                stmt.setString(4, firstName);
                stmt.setString(5, lastName);
                stmt.setString(6, email); 
                stmt.setInt(7, deptId);
                stmt.setString(8, jobTitle);
                stmt.setDate(9, java.sql.Date.valueOf(hireDate));
                
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newUserId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no user_id obtained.");
                    }
                }
            }

            // --- STEP B: INSERT into EMPLOYEES TABLE ---
            final String EMP_SQL = "INSERT INTO employees (user_id, role_id, gender, email, dept_id, position, salary, date_joined) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(EMP_SQL)) {
                stmt.setInt(1, newUserId);
                stmt.setInt(2, roleId);
                stmt.setString(3, gender);
                stmt.setString(4, email);
                stmt.setInt(5, deptId);
                stmt.setString(6, jobTitle);
                stmt.setDouble(7, salary); 
                stmt.setDate(8, java.sql.Date.valueOf(hireDate));
                
                stmt.executeUpdate();
            }

            conn.commit(); // Commit both inserts if successful 
            return newUserId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Roll back if any part failed
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e; // Re-throw exception for controller to handle
        } finally {
            if (conn != null) {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true); // Restore default
                    conn.close();
                }
            }
        }
    }
}