package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NotificationService {

    /**
     * PREVIOUS LOGIC (Maintained)
     * Creates a notification in the database for a specific user.
     */
    public static void addNotification(int userId, String title, String message, String target) {
        String sql = "INSERT INTO notifications (user_id, title, message, target, status) VALUES (?, ?, ?, ?, 'Unread')";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, target);
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    /**
     * NEW ENHANCEMENT: Notify All Admins
     * Use this for system-wide alerts that every Administrator should see.
     * Logic: Finds all users with the 'Admin' role and adds a notification for each.
     */
    public static void notifyAllAdmins(String title, String message, String target) {
        // Query to find all user_ids belonging to the 'Admin' role
        String findAdminsSql = "SELECT user_id FROM users WHERE role_id = (SELECT role_id FROM roles WHERE role_name = 'Admin')";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psFind = conn.prepareStatement(findAdminsSql);
             ResultSet rs = psFind.executeQuery()) {
            
            while (rs.next()) {
                int adminId = rs.getInt("user_id");
                // Re-uses your previous logic to insert for each admin found
                addNotification(adminId, title, message, target);
            }
            System.out.println("Notification sent to all Admins: " + title);
            
        } catch (SQLException e) {
            System.err.println("Error notifying admins: " + e.getMessage());
        }
    }

    /**
     * NEW ENHANCEMENT: Specialized Payroll Notification
     * Automatically targets the Payroll Officer and Admin.
     */
    public static void notifyPayrollDepartment(String message) {
        String sql = "SELECT user_id FROM users WHERE role_id IN " +
                     "(SELECT role_id FROM roles WHERE role_name IN ('Admin', 'Payroll Officer'))";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                addNotification(rs.getInt("user_id"), "Payroll Update", message, "Payroll");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}