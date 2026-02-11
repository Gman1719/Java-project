package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent; 
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node; 
import javafx.scene.Parent; 
import javafx.scene.Scene; 
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage; 
import models.Employee;
import utils.DBConnection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import utils.SessionManager;
import java.io.File;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // ------------------ SUMMARY CARDS ------------------
    @FXML private Label lblTotalEmployees;
    @FXML private Label lblPendingLeaves;
    @FXML private Label lblPendingRequests;
    @FXML private Label lblPayrollPending;

    @FXML private Label lblAdminName;          
    @FXML private ImageView imgDashboardProfile;

    // ------------------ CHART CONTAINERS ------------------
    @FXML private StackPane salaryChartContainer;
    @FXML private StackPane attendanceChartContainer;

    // ------------------ EMPLOYEES TABLE ------------------
    @FXML private TableView<Employee> tblEmployees;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String> colUsername;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colDept;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colStatus;
    @FXML private TableColumn<Employee, Void> colActions;

    private ObservableList<Employee> employeeList = FXCollections.observableArrayList();

    // ------------------ MAIN CONTAINER ------------------
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox mainContent;

    // ------------------ INITIALIZE ------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            System.out.println("Admin Dashboard initializing...");
            refreshDashboardHeader();
            loadSummaryCards();
            loadCharts();
            setupEmployeeTable();
            loadEmployeesTable();
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Initialization Failed! Database setup or connectivity issue.");
        }
    }

    // ------------------ SUMMARY CARDS ------------------
    private void loadSummaryCards() {
        lblTotalEmployees.setText(getSingleValue("SELECT COUNT(*) FROM employees"));
        lblPendingLeaves.setText(getSingleValue("SELECT COUNT(*) FROM leave_requests WHERE status='Pending'"));
        lblPendingRequests.setText(getSingleValue("SELECT (SELECT COUNT(*) FROM salary_advance_requests WHERE status='Pending') + (SELECT COUNT(*) FROM bank_requests WHERE status='Pending')"));
        lblPayrollPending.setText(getSingleValue("SELECT COUNT(*) FROM payroll WHERE net_salary IS NULL"));
    }

    private String getSingleValue(String query) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "0";
    }

    // ------------------ CHARTS ------------------
    private void loadCharts() {
        loadSalaryChart();
        // Placeholder for Attendance Chart if needed
    }

    private void loadSalaryChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Salary Overview (Mock Data)");
        xAxis.setLabel("Month");
        yAxis.setLabel("Total Salary");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Salary");
        series.getData().add(new XYChart.Data<>("Jan", 10000));
        series.getData().add(new XYChart.Data<>("Feb", 12000));
        series.getData().add(new XYChart.Data<>("Mar", 15000));

        chart.getData().add(series);
        salaryChartContainer.getChildren().setAll(chart);
    }

    // ------------------ EMPLOYEES TABLE ------------------
    private void setupEmployeeTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()
            )
        );
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final Button btnReset = new Button("Reset PW");
            private final HBox pane = new HBox(5, btnEdit, btnDelete, btnReset);

            {
                btnEdit.setStyle("-fx-cursor: hand;");
                btnDelete.setStyle("-fx-cursor: hand;");
                btnReset.setStyle("-fx-cursor: hand;");
                
                btnEdit.setOnAction(e -> editEmployee(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteEmployee(getTableView().getItems().get(getIndex())));
                btnReset.setOnAction(e -> resetPassword(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tblEmployees.setItems(employeeList);
    }

    private void loadEmployeesTable() {
        employeeList.clear();

        String query = "SELECT e.emp_id, u.user_id, u.username, u.first_name, u.last_name, " +
                       "d.dept_name AS department, " +
                       "r.role_name, e.position, e.date_joined, e.status " +
                       "FROM employees e " +
                       "JOIN users u ON e.user_id = u.user_id " +
                       "JOIN roles r ON u.role_id = r.role_id " + 
                       "JOIN departments d ON e.dept_id = d.dept_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Employee emp = new Employee();
                emp.setEmployeeId(rs.getInt("emp_id"));
                emp.setUserId(rs.getInt("user_id"));
                emp.setUsername(rs.getString("username"));
                emp.setFirstName(rs.getString("first_name"));
                emp.setLastName(rs.getString("last_name"));
                emp.setDepartment(rs.getString("department"));
                emp.setPosition(rs.getString("position"));
                emp.setDateJoined(rs.getDate("date_joined") != null ?
                                  rs.getDate("date_joined").toLocalDate() : LocalDate.now());
                emp.setStatus(rs.getString("status"));
                emp.setRoleName(rs.getString("role_name"));
                employeeList.add(emp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showInfo("Database error loading employees: " + e.getMessage());
        }
    }

    // ------------------ ACTIONS ------------------
    private void editEmployee(Employee emp) { showInfo("Edit Employee: " + emp.getUsername()); }
    private void deleteEmployee(Employee emp) { showInfo("Delete Employee: " + emp.getUsername()); }
    private void resetPassword(Employee emp) { showInfo("Reset Password for: " + emp.getUsername()); }

    // ------------------ PAGE NAVIGATION ------------------
    @FXML private void openDashboard() { mainBorderPane.setCenter(mainContent); }
    @FXML private void openAddEmployee() { loadPage("/views/AddEmployee.fxml"); }
    @FXML private void openBulkUpload() { loadPage("/views/BulkUpload.fxml"); }
    @FXML private void openPayrollManagement() { loadPage("/views/PayrollManagement.fxml"); }
    @FXML private void openUserManagement() { loadPage("/views/UserManagement.fxml"); }
    @FXML private void openDepartmentManagement() { loadPage("/views/DepartmentManagement.fxml"); }
    @FXML private void openAttendanceLeaves() { loadPage("/views/AttendanceLeaves.fxml"); }
    @FXML private void openRequests() { loadPage("/views/Requests.fxml"); }
    @FXML private void openReports() { loadPage("/views/Reports.fxml"); }
    @FXML private void openNotifications() { loadPage("/views/Notifications.fxml"); }
    @FXML private void openSystemSettings() { loadPage("/views/SystemSettings.fxml"); }
    @FXML private void changePassword() { loadPage("/views/ChangePassword.fxml"); }

    @FXML
    private void openProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Admin Profile");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.sizeToScene(); 
            stage.setResizable(false); 
            stage.showAndWait();
            refreshDashboardHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshDashboardHeader() {
        Employee emp = SessionManager.getCurrentEmployee();
        if (emp != null) {
            if (lblAdminName != null) {
                String firstName = (emp.getFirstName() != null) ? emp.getFirstName() : "";
                String lastName = (emp.getLastName() != null) ? emp.getLastName() : "";
                lblAdminName.setText((firstName + " " + lastName).trim());
            }

            if (imgDashboardProfile != null && emp.getProfilePicturePath() != null) {
                try {
                    File file = new File(emp.getProfilePicturePath());
                    if (file.exists()) {
                        imgDashboardProfile.setImage(new Image(file.toURI().toString()));
                    }
                } catch (Exception e) {
                    System.err.println("Header refresh failed: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles Logout by clearing the session and returning to the Login screen.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // Clear the session
                SessionManager.logout();

                // Close the current Dashboard Stage
                Node source = (Node) event.getSource();
                Stage currentStage = (Stage) source.getScene().getWindow();
                currentStage.close();

                // Load and Show the Login Screen
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
                Parent root = loader.load();
                Stage loginStage = new Stage();
                loginStage.setTitle("Employee Management System - Login");
                loginStage.setScene(new Scene(root));
                loginStage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                showInfo("Logout failed: Could not load the Login view.");
            }
        }
    }

    // ------------------ UTILS (FIXED TO PREVENT CAST ERRORS) ------------------
    private void loadPage(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("FXML Error: Location not found for " + fxmlPath);
                showInfo("View file not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            // ⭐ Using 'Parent' instead of 'Pane' or 'VBox' to support all FXML root types
            Parent page = loader.load(); 
            mainBorderPane.setCenter(page);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Failed to load: " + fxmlPath + ". Error: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}