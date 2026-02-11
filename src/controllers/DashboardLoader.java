package controllers;

import dao.EmployeeDAO;
import models.Employee;
import models.AttendanceRecord;
import models.LeaveRequest;
import utils.DBConnection;
import utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType; 
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.chart.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.SimpleStringProperty;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.Month;
import java.time.YearMonth;

public class DashboardLoader {

    // --- 1. PROFILE LOADING ---
    public static void loadProfile(Label lblName, Label lblRole, ImageView imgProfile) {
        Employee emp = SessionManager.getCurrentEmployee();
        if (emp != null) {
            lblName.setText(emp.getFirstName() + " " + emp.getLastName());
            lblRole.setText(emp.getRoleName());
            try {
                // Use absolute path from resources
                URL resource = DashboardLoader.class.getResource("/images/default_profile.png");
                if (resource != null) {
                    imgProfile.setImage(new Image(resource.toExternalForm()));
                }
            } catch (Exception e) {
                System.err.println("Profile image load failed: " + e.getMessage());
            }
        }
    }

    // --- 2. METRICS LOADING (Salary & Leaves) ---
    public static void loadSalaryAndLeaves(Label lblLeaves, Label lblSalary) {
        int empId = SessionManager.getCurrentEmployeeId();
        double totalAllowedLeaves = 15.0;

        try (Connection conn = DBConnection.getConnection()) {
            // Leave Balance
            String leaveSql = "SELECT IFNULL(SUM(total_days),0) AS used FROM leave_requests WHERE emp_id=? AND status='Approved'";
            try (PreparedStatement psL = conn.prepareStatement(leaveSql)) {
                psL.setInt(1, empId);
                ResultSet rsL = psL.executeQuery();
                if (rsL.next()) {
                    double remaining = totalAllowedLeaves - rsL.getDouble("used");
                    lblLeaves.setText(remaining + " Days");
                }
            }

            // Net Salary - Fixed order by generated_on
            String salSql = "SELECT net_salary FROM payroll WHERE emp_id=? ORDER BY generated_on DESC LIMIT 1";
            try (PreparedStatement psS = conn.prepareStatement(salSql)) {
                psS.setInt(1, empId);
                ResultSet rsS = psS.executeQuery();
                lblSalary.setText(rsS.next() ? String.format("$%.2f", rsS.getDouble("net_salary")) : "$0.00");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- 3. CHART LOADING (Fixed SQL Syntax) ---
    public static void loadWorkingHoursChart(StackPane chartContainer) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setTitle("Last 6 Months Hours");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // FIXED SQL: MONTH(date) is used in both SELECT and ORDER BY to satisfy ONLY_FULL_GROUP_BY
        String sql = "SELECT MONTH(date) AS m, SUM(TIMESTAMPDIFF(HOUR,time_in,time_out)) AS hours " +
                     "FROM attendance WHERE emp_id=? AND time_in IS NOT NULL AND time_out IS NOT NULL " +
                     "GROUP BY m ORDER BY m DESC LIMIT 6";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, SessionManager.getCurrentEmployeeId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String monthName = Month.of(rs.getInt("m")).name();
                series.getData().add(new XYChart.Data<>(monthName, rs.getInt("hours")));
            }
            chart.getData().add(series);
            chartContainer.getChildren().setAll(chart);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- 4. TABLE INITIALIZATION ---
    public static void initializeAttendanceTable(TableView<AttendanceRecord> table) {
        table.getColumns().clear();
        TableColumn<AttendanceRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<AttendanceRecord, String> timeInCol = new TableColumn<>("Time In");
        timeInCol.setCellValueFactory(new PropertyValueFactory<>("timeIn"));
        TableColumn<AttendanceRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(dateCol, timeInCol, statusCol);
    }

    public static void initializeLeaveTable(TableView<LeaveRequest> table) {
        table.getColumns().clear();
        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        TableColumn<LeaveRequest, String> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));
        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(typeCol, daysCol, statusCol);
    }

    public static void initializeOtherRequestsTable(TableView<String> table) {
        table.getColumns().clear();
        TableColumn<String, String> col = new TableColumn<>("Request History (Type | Status | Date)");
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        table.getColumns().add(col);
    }

    // --- 5. DATA LOADING FOR TABLES ---
    public static void loadAttendanceData(TableView<AttendanceRecord> table, YearMonth month) {
        table.setItems(new EmployeeDAO().getMonthlyAttendance(SessionManager.getCurrentEmployeeId(), month));
    }

    public static void loadLeaves(TableView<LeaveRequest> table) {
        table.setItems(new EmployeeDAO().getEmployeeLeaveRequests(SessionManager.getCurrentEmployeeId()));
    }

    public static void loadOtherRequests(TableView<String> table) {
        ObservableList<String> data = FXCollections.observableArrayList();
        int empId = SessionManager.getCurrentEmployeeId();
        
        String sql = "SELECT 'Bank Change' as t, status, request_date FROM bank_requests WHERE emp_id=? " +
                     "UNION SELECT 'Salary Advance', status, request_date FROM salary_advance_requests WHERE emp_id=? " +
                     "UNION SELECT 'Reimbursement', status, request_date FROM reimbursements WHERE emp_id=? " +
                     "ORDER BY request_date DESC LIMIT 15";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, empId);
            ps.setInt(3, empId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(rs.getString("t") + " | " + rs.getString("status") + " | " + rs.getDate("request_date"));
            }
            table.setItems(data);
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    // --- 6. WINDOW & UI MANAGEMENT ---
    public static void openModal(String fxmlPath, String title, Stage owner) {
        try {
            URL resource = DashboardLoader.class.getResource(fxmlPath);
            if (resource == null) throw new IOException("FXML file not found: " + fxmlPath);
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "System Error", "Could not open window: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void performLogout(Stage currentStage) {
        if (showConfirm("Logout", "Are you sure you want to logout?")) {
            try {
                SessionManager.logout();
                currentStage.close();

                URL loginFxml = DashboardLoader.class.getResource("/views/Login.fxml");
                Parent root = FXMLLoader.load(loginFxml);
                Stage loginStage = new Stage();
                loginStage.setTitle("Login");
                loginStage.setScene(new Scene(root));
                loginStage.show();
            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Error", "Failed to load login screen.");
            }
        }
    }

    // --- HELPER METHODS ---
    public static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}