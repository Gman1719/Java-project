package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Notification;
import utils.DBConnection;
import utils.SessionManager;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private VBox containerNotifications;
    @FXML private Label lblUnreadCount;

    private String currentFilter = "ALL";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(this::loadNotifications);
    }

    @FXML
    public void loadNotifications() {
        if (containerNotifications == null) return;
        
        containerNotifications.getChildren().clear();
        if (lblUnreadCount != null) lblUnreadCount.setText("Loading...");

        int userId = SessionManager.getCurrentUserId();
        int empId = SessionManager.getCurrentEmployeeId();
        List<Notification> allData = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            fetchGeneralNotifications(conn, userId, allData);
            fetchPersonalAlerts(conn, empId, allData);
            fetchPayrollAlerts(conn, empId, allData);
            fetchAnnouncements(conn, empId, allData);

            allData.sort(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

            int unread = 0;
            for (Notification n : allData) {
                if (shouldShow(n)) {
                    if ("Unread".equalsIgnoreCase(n.getStatus())) unread++;
                    addNotificationUI(n);
                }
            }
            
            final int count = unread;
            Platform.runLater(() -> {
                if (lblUnreadCount != null) lblUnreadCount.setText("You have " + count + " unread alerts");
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldShow(Notification n) {
        if ("ALL".equalsIgnoreCase(currentFilter)) return true;
        String target = n.getTarget();
        if (target == null) target = "System"; 
        if ("PERSONAL".equalsIgnoreCase(currentFilter)) {
            return "Personal".equalsIgnoreCase(target) || "Attendance".equalsIgnoreCase(target);
        }
        return target.equalsIgnoreCase(currentFilter);
    }

    private void fetchGeneralNotifications(Connection c, int userId, List<Notification> l) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                l.add(new Notification(
                    rs.getInt("notify_id"), userId, rs.getString("title"), 
                    rs.getString("message"), rs.getString("target"), 
                    rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        }
    }

    private void fetchPersonalAlerts(Connection c, int empId, List<Notification> l) throws SQLException {
        String sql = "SELECT leave_type, status, requested_on FROM leave_requests WHERE emp_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                l.add(new Notification(0, 0, "Leave Request", 
                    rs.getString("leave_type") + " is " + rs.getString("status"), 
                    "Personal", "Read", rs.getTimestamp("requested_on").toLocalDateTime()));
            }
        }
    }

    private void fetchPayrollAlerts(Connection c, int empId, List<Notification> l) throws SQLException {
        String sql = "SELECT month, year, generated_on FROM payroll WHERE emp_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                l.add(new Notification(0, 0, "Payroll", 
                    "Payslip for " + rs.getString("month") + " " + rs.getInt("year"), 
                    "Payroll", "Read", rs.getTimestamp("generated_on").toLocalDateTime()));
            }
        }
    }

    private void fetchAnnouncements(Connection c, int empId, List<Notification> l) throws SQLException {
        String sql = "SELECT type, description, created_at FROM calendar_events WHERE emp_id IS NULL OR emp_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                l.add(new Notification(0, 0, rs.getString("type"), 
                    rs.getString("description"), "System", "Unread", 
                    rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
    }

    private void addNotificationUI(Notification n) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");

        // Text area
        VBox text = new VBox(5);
        text.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(n.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #212529; -fx-font-size: 14px;");
        Label msg = new Label(n.getMessage());
        msg.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px;");
        msg.setWrapText(true);
        Label date = new Label(n.getCreatedAt().toString().replace("T", " ").substring(0, 16));
        date.setStyle("-fx-text-fill: #868E96; -fx-font-size: 10px;");

        text.getChildren().addAll(title, msg, date);

        // Push button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(text, spacer);

        // Mark Read button for unread notifications
        if ("Unread".equalsIgnoreCase(n.getStatus()) && n.getNotificationId() > 0) {
            Button btn = new Button("Mark Read");
            btn.setStyle("-fx-background-color: #e7f5ff; -fx-text-fill: #228BE6; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
            btn.setOnAction(e -> markRead(n.getNotificationId()));
            card.getChildren().add(btn);
        }

        containerNotifications.getChildren().add(card);
    }


    private void markRead(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET status='Read' WHERE notify_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadNotifications();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void markAllRead() {
        int userId = SessionManager.getCurrentUserId();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET status='Read' WHERE user_id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            loadNotifications();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void filterAll() { currentFilter = "ALL"; loadNotifications(); }
    @FXML private void filterPersonal() { currentFilter = "PERSONAL"; loadNotifications(); }
    @FXML private void filterSystem() { currentFilter = "System"; loadNotifications(); }
    @FXML private void filterPayroll() { currentFilter = "Payroll"; loadNotifications(); }
}