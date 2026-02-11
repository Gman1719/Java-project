package utils;

import models.Employee;

/**
 * Manages the global state of the logged-in user.
 * This class acts as a central hub for session data, allowing different
 * controllers to share user information without passing objects manually.
 */
public class SessionManager {

    // Holds the currently logged-in employee/user
    private static Employee currentEmployee;

    /**
     * Sets the session data after a successful login.
     *
     * @param emp The Employee object populated from the database.
     */
    public static void setCurrentEmployee(Employee emp) {
        currentEmployee = emp;

        if (emp != null) {
            System.out.println(
                "Session started: " +
                emp.getUsername() +
                " [" + emp.getRoleName() + "]"
            );
        }
    }

    /**
     * @return The full Employee object currently in session.
     */
    public static Employee getCurrentEmployee() {
        return currentEmployee;
    }

    /**
     * Alias for getCurrentEmployee().
     * Useful when controllers expect a "User" context.
     */
    public static Employee getCurrentUser() {
        return currentEmployee;
    }

    /**
     * Retrieves the USER ID (users.user_id).
     * Used for:
     * - Notifications
     * - Audit Logs
     *
     * @return user_id or -1 if no active session
     */
    public static int getCurrentUserId() {
        return (currentEmployee != null)
                ? currentEmployee.getUserId()
                : -1;
    }

    /**
     * Retrieves the EMPLOYEE ID (employees.emp_id).
     * Used for:
     * - Attendance
     * - Payroll
     * - Leave Requests
     *
     * @return emp_id or -1 if no active session
     */
    public static int getCurrentEmployeeId() {
        return (currentEmployee != null)
                ? currentEmployee.getEmployeeId()
                : -1;
    }

    /**
     * @return Username of the logged-in user, or "Guest" if none.
     */
    public static String getUsername() {
        return (currentEmployee != null)
                ? currentEmployee.getUsername()
                : "Guest";
    }

    /**
     * @return Role name of the logged-in user (Admin, HR, Employee, etc.)
     */
    public static String getRole() {
        return (currentEmployee != null)
                ? currentEmployee.getRoleName()
                : null;
    }

    /**
     * Checks if the current user is an Admin.
     * Useful for role-based UI restrictions.
     */
    public static boolean isAdmin() {
        return currentEmployee != null
                && "Admin".equalsIgnoreCase(currentEmployee.getRoleName());
    }

    /**
     * Updates the session object after profile changes.
     *
     * @param updatedEmp Updated Employee object
     */
    public static void refreshSession(Employee updatedEmp) {
        currentEmployee = updatedEmp;

        if (updatedEmp != null) {
            System.out.println(
                "Session refreshed for: " +
                updatedEmp.getUsername()
            );
        }
    }

    /**
     * @return true if a user is currently logged in
     */
    public static boolean isLoggedIn() {
        return currentEmployee != null;
    }

    /**
     * Logs out the current user and clears session data.
     * Matches calls used in DashboardController.
     */
    public static void logout() {
        if (currentEmployee != null) {
            System.out.println(
                "User " +
                currentEmployee.getUsername() +
                " logged out."
            );
        }
        currentEmployee = null;
    }

    /**
     * Clears all session data (hard reset).
     */
    public static void clearSession() {
        currentEmployee = null;
    }
}
