package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import utils.DBConnection;

public class HRNotificationsController implements Initializable {

    @FXML private Tab tabUnread;
    @FXML private Tab tabArchived;
    @FXML private VBox vboxUnreadNotifications;
    @FXML private VBox vboxArchivedNotifications;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadNotifications();
    }

    /**
     * Fetches all requests and notifications from the database using UNION.
     */
    private void loadNotifications() {
        vboxUnreadNotifications.getChildren().clear();
        vboxArchivedNotifications.getChildren().clear();

        // The query now includes the primary ID from each table so buttons know what to update
        String query = 
            "SELECT leave_id AS id, 'LEAVE' AS type, CONCAT('Leave: ', leave_type) AS title, " +
            "CONCAT('Emp ID ', emp_id, ' requested ', total_days, ' days.') AS msg, status FROM leave_requests " +
            "UNION ALL " +
            "SELECT request_id AS id, 'BANK' AS type, 'Bank Account Change' AS title, " +
            "CONCAT('Emp ID ', emp_id, ' new acc: ', new_account) AS msg, status FROM bank_requests " +
            "UNION ALL " +
            "SELECT advance_id AS id, 'SALARY' AS type, 'Salary Advance Request' AS title, " +
            "CONCAT('Emp ID ', emp_id, ' amount: ', amount) AS msg, status FROM salary_advance_requests " +
            "UNION ALL " +
            "SELECT notify_id AS id, 'GENERAL' AS type, title, message AS msg, status FROM notifications " +
            "ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String title = rs.getString("title");
                String message = rs.getString("msg");
                String status = rs.getString("status");

                String color = getColorByTarget(type);

                if (status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("Unread")) {
                    addNotification(title, message, color, vboxUnreadNotifications, id, type, status);
                } else {
                    addNotification(title, message, "#6c757d", vboxArchivedNotifications, id, type, status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateTabLabels();
    }

    private String getColorByTarget(String type) {
        if (type == null) return "#007bff";
        return switch (type.toUpperCase()) {
            case "LEAVE"   -> "#ffc107"; // Yellow
            case "BANK"    -> "#17a2b8"; // Cyan
            case "SALARY"  -> "#28a745"; // Green
            case "SECURITY"-> "#dc3545"; // Red
            default        -> "#007bff"; // Blue
        };
    }

    /**
     * Creates a notification card with a "Mark as Read" button for individual processing.
     */
    private void addNotification(String title, String message, String color, VBox targetVBox, int id, String type, String currentStatus) {
        VBox notificationBox = new VBox(5);
        notificationBox.setStyle(String.format(
            "-fx-border-color: %s; -fx-border-width: 0 0 0 4; -fx-background-color: white; -fx-padding: 12; " +
            "-fx-border-radius: 0 4 4 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);", color));

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 14px;");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-text-fill: #555; -fx-wrap-text: true;");

        notificationBox.getChildren().addAll(lblTitle, lblMessage);

        if (currentStatus.equalsIgnoreCase("Unread") || currentStatus.equalsIgnoreCase("Pending")) {
            Button btnRead = new Button("Mark as Read");
            btnRead.setStyle("-fx-font-size: 11px; -fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-cursor: hand; -fx-font-weight: bold;");
            
            btnRead.setOnAction(e -> markSingleAsRead(id, type));
            
            HBox actionBox = new HBox(btnRead);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            notificationBox.getChildren().add(actionBox);
        }

        targetVBox.getChildren().add(notificationBox);
    }

    /**
     * Updates the status of a specific record in its respective table.
     */
    private void markSingleAsRead(int id, String type) {
        String table;
        String column;
        String statusValue = "Read";

        switch (type.toUpperCase()) {
            case "LEAVE" -> { table = "leave_requests"; column = "leave_id"; statusValue = "Approved"; }
            case "BANK"  -> { table = "bank_requests"; column = "request_id"; statusValue = "Approved"; }
            case "SALARY"-> { table = "salary_advance_requests"; column = "advance_id"; statusValue = "Approved"; }
            default      -> { table = "notifications"; column = "notify_id"; statusValue = "Read"; }
        }

        String sql = "UPDATE " + table + " SET status = ? WHERE " + column + " = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, statusValue);
            pst.setInt(2, id);
            pst.executeUpdate();
            
            loadNotifications(); // Refresh both tabs
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update status.");
        }
    }

    @FXML
    private void handleMarkAllRead(ActionEvent event) {
        // This button typically clears the general 'notifications' table
        String updateSql = "UPDATE notifications SET status = 'Read' WHERE status = 'Unread'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            
            int affectedRows = pstmt.executeUpdate();
            loadNotifications();
            showAlert(Alert.AlertType.INFORMATION, "Refresh", "Notifications updated successfully.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update notifications.");
        }
    }

    private void updateTabLabels() {
        tabUnread.setText(String.format("Unread (%d)", vboxUnreadNotifications.getChildren().size()));
        tabArchived.setText(String.format("Archived (%d)", vboxArchivedNotifications.getChildren().size()));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}