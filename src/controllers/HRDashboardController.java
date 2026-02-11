package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL; 
import java.util.ResourceBundle;

// --- Imports from previous updates ---
import javafx.scene.chart.PieChart;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import models.Employee; 
import services.UserService; 
import services.DashboardService; 
import utils.SessionManager; 
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// These are necessary for the Alert and were confirmed as present/needed
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType; 
import javafx.scene.control.ButtonType;
// ------------------------------------

public class HRDashboardController implements Initializable {

    // FXML Containers & Metrics
    @FXML private StackPane mainContentArea; 
    @FXML private VBox vboxDashboardOverview; 
    
    // Profile Area (from FXML)
    @FXML private ImageView imgProfile;
    @FXML private Label lblName;
    @FXML private Label lblRole;
    
    // Dashboard Metric Labels
    @FXML private Label lblTotalEmployees; 
    @FXML private Label lblPendingRequests; 
    @FXML private Label lblNewHires;
    @FXML private Label lblLeavesToday;
    
    // Dashboard Components
    @FXML private StackPane chartDepartment;
    @FXML private TableView<RequestData> tblLatestRequests;
    
    // FXML components from the Sidebar (for controlling active state)
    @FXML private Button btnDashboardOverview;
    @FXML private Button btnEmployeeRecords;

    private UserService userService = new UserService(); 
    private DashboardService dashboardService = new DashboardService(); 
    
    // Simple helper model for the TableView (Nested to keep controller self-contained)
    public static class RequestData {
        private final String type;
        private final String employeeName;
        private final String dateSubmitted;
        private final String details;
        
        // Constructor, Getters (required for TableView PropertyValueFactory)
        public RequestData(String type, String employeeName, String dateSubmitted, String details) {
            this.type = type;
            this.employeeName = employeeName;
            this.dateSubmitted = dateSubmitted;
            this.details = details;
        }

        public String getType() { return type; }
        public String getEmployeeName() { return employeeName; }
        public String getDateSubmitted() { return dateSubmitted; }
        public String getDetails() { return details; }
    }
    
    // --- Initialization ---
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadProfileInfo(); 
        setupTableView();  
        loadDashboardMetrics(); 
        
        vboxDashboardOverview.setVisible(true);
        vboxDashboardOverview.setManaged(true);
        mainContentArea.getChildren().setAll(vboxDashboardOverview);
    }
    
    private void loadProfileInfo() {
        Employee currentEmployee = SessionManager.getCurrentEmployee();
        if (currentEmployee != null) {
            lblName.setText(currentEmployee.getFullName());
            lblRole.setText(currentEmployee.getRoleName());
            // TODO: Add logic to load profile image into imgProfile
        } else {
            lblName.setText("HR Manager");
            lblRole.setText("Administrator");
        }
    }
    
    private void setupTableView() {
        // Assume FXML has 5 columns: Type, Employee Name, Date Submitted, Details/Reason, Action
        ObservableList<TableColumn<RequestData, ?>> columns = tblLatestRequests.getColumns();
        
        if (columns.size() > 0) columns.get(0).setCellValueFactory(new PropertyValueFactory<>("type"));
        if (columns.size() > 1) columns.get(1).setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        if (columns.size() > 2) columns.get(2).setCellValueFactory(new PropertyValueFactory<>("dateSubmitted"));
        if (columns.size() > 3) columns.get(3).setCellValueFactory(new PropertyValueFactory<>("details"));
        
        if (columns.size() > 4) {
             TableColumn<RequestData, Void> actionCol = (TableColumn<RequestData, Void>) columns.get(4);
             actionCol.setCellFactory(param -> new TableCell<RequestData, Void>() {
                private final Button btn = new Button("Review");
                {
                    btn.getStyleClass().add("action-button");
                    btn.setOnAction(event -> {
                        loadRequestsView(); 
                    });
                }
    
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }
    }


    /**
     * Loads metrics, table data, and charts from the database using DashboardService.
     */
    /**
     * Loads metrics, table data, and charts from the database using DashboardService.
     * UPDATED: Integrated with real database queries from DashboardService.
     */
    private void loadDashboardMetrics() {
        try {
            // --- 1. Load Metric Counts (Using DashboardService) ---
            int totalActive = dashboardService.getTotalActiveEmployees();
            int pendingLeave = dashboardService.getPendingLeaveRequestsCount();
            int pendingCorrection = dashboardService.getPendingCorrectionRequestsCount();
            int leavesToday = dashboardService.getEmployeesOnLeaveTodayCount();
            
            // --- Set Labels with Live Data ---
            lblTotalEmployees.setText(String.valueOf(totalActive));
            lblTotalEmployees.getStyleClass().setAll("metric-value");
            
            // Pending Requests = Leave Requests + Correction (Bank/Advance) Requests
            lblPendingRequests.setText(String.valueOf(pendingLeave + pendingCorrection));
            
            // Setting a static value or a specific query if available for new hires
            lblNewHires.setText("5"); 
            
            lblLeavesToday.setText(String.valueOf(leavesToday));
            
            // --- 2. Load Table Data (Latest Pending Requests) ---
            // This pulls combined requests (Leave, Bank, Salary) from the service
            List<String[]> latestRequests = dashboardService.getLatestPendingRequests(5); 
            ObservableList<RequestData> tableData = FXCollections.observableArrayList();
            
            for (String[] req : latestRequests) {
                // req array structure: [0:Type, 1:Employee Name, 2:Date Submitted, 3:Details]
                if (req.length >= 4) {
                    tableData.add(new RequestData(req[0], req[1], req[2], req[3]));
                }
            }
            tblLatestRequests.setItems(tableData);
            
            // --- 3. Load Chart ---
            loadDepartmentChart();

        } catch (SQLException e) {
            System.err.println("❌ Database Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Show error indicators
            lblTotalEmployees.setText("!");
            lblPendingRequests.setText("!");
            showAlert(Alert.AlertType.ERROR, "Data Load Error", 
                      "Failed to retrieve dashboard metrics from the database.");
        }
    }
    
    /**
     * Loads the department distribution chart using live data from the database.
     * UPDATED: Now groups real employees by their assigned departments.
     */
    private void loadDepartmentChart() {
        try {
            // Fetch real distribution from the database
            Map<String, Integer> stats = dashboardService.getDepartmentDistribution();
            
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            
            // If the database is empty, stats will be empty; handle it gracefully
            if (stats != null && !stats.isEmpty()) {
                stats.forEach((deptName, count) -> {
                    pieChartData.add(new PieChart.Data(deptName, count));
                });
            } else {
                // Optional: add a "No Data" slice if the DB is fresh/empty
                pieChartData.add(new PieChart.Data("No Employees Found", 1));
            }
                
            final PieChart chart = new PieChart(pieChartData);
            chart.setTitle("Employee Distribution");
            chart.setLegendVisible(true);
            chart.setLabelsVisible(true); // Shows department names on the chart
            chart.setLabelLineLength(10);
            
            // Refresh the UI container
            chartDepartment.getChildren().clear();
            chartDepartment.getChildren().add(chart);
            
        } catch (SQLException e) {
            System.err.println("❌ Error loading Department Chart: " + e.getMessage());
            // Fallback to an empty chart container or error message
            chartDepartment.getChildren().clear();
            chartDepartment.getChildren().add(new Label("Chart Unavailable"));
        }
    }

    /**
     * Loads a new FXML view into the center pane.
     */
    private void loadCenterContent(String fxmlFileName) {
        mainContentArea.getChildren().clear(); 
        final String resourcePath = "/views/" + fxmlFileName;
        
        try {
            URL fxmlUrl = getClass().getResource(resourcePath);
            if (fxmlUrl == null) {
                // Fallback attempt for files not in /views/
                fxmlUrl = getClass().getResource("/" + fxmlFileName);
                if (fxmlUrl == null) {
                     throw new IOException("Resource not found at path: " + resourcePath + ". Check file name and case in src/views/.");
                }
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent newContent = loader.load();
            
            mainContentArea.getChildren().add(newContent);
            
        } catch (IOException e) {
            System.err.println("❌ FXML Load Failed: " + resourcePath);
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Error: Could not load view from " + resourcePath + ". " + e.getMessage()));
        }
    }
    
    // -------------------------------------------------------------------------
    // --- NAVIGATION ACTIONS (Standard Views) --- 
    // -------------------------------------------------------------------------
    
    @FXML
    private void showOverview() {
        mainContentArea.getChildren().clear();
        
        if (!mainContentArea.getChildren().contains(vboxDashboardOverview)) {
            mainContentArea.getChildren().add(vboxDashboardOverview);
        }
        vboxDashboardOverview.setVisible(true);
        vboxDashboardOverview.setManaged(true);
        vboxDashboardOverview.toFront();
        
        loadDashboardMetrics(); // Reload data on return
    }
    
    @FXML
    private void loadEmployeeManagementView() {
        loadCenterContent("hr_employee_management.fxml"); 
    }

    @FXML
    private void openPayrollWizard() {
        loadCenterContent("hr_payroll_support.fxml");
    }

    @FXML
    private void loadLeaveManagementView() {
        loadCenterContent("AttendanceLeaves.fxml"); 
    }

    @FXML
    private void loadRequestsView() {
        loadCenterContent("hr_requests_view.fxml"); 
    }
 
    @FXML
    private void loadReportsView() {
        loadCenterContent("hr_reports_analytics.fxml"); 
    }

    @FXML
    private void loadRecruitmentView() {
        loadCenterContent("hr_recruitment_view.fxml"); 
    }
    
    // -------------------------------------------------------------------------
    // --- QUICK ACTIONS --- 
    // -------------------------------------------------------------------------
    
    @FXML
    public void openProfile() { 
        loadCenterContent("profile.fxml");
    }
    
    @FXML
    public void changePassword() {
        loadCenterContent("quick_action_change_password.fxml");
    }
    
    @FXML
    public void openNotifications() { 
        loadCenterContent("quick_action_notifications.fxml");
    }
    
    @FXML
    private void openAddEmployee() {
        // Loads the employee form FXML
        loadCenterContent("AddEmployee.fxml"); 
    }
    
    @FXML
    private void openBulkUpload() {
        loadCenterContent("quick_action_bulk_upload.fxml");
    }

    @FXML
    private void validatePayrollData() {
        loadCenterContent("quick_action_payroll_validation.fxml");
    }
    
    /**
     * FIXED: Handles user logout by clearing the session and switching the scene to the login view.
     * Uses the reliable method to retrieve the Stage via the main content node.
     */
    @FXML
    private void handleLogout() {
        // 1. Show Confirmation Alert
        // FIX: Using the AlertType constructor and setting properties separately.
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout Confirmation");
        confirmation.setHeaderText(null); // Recommended for a clean confirmation dialog
        confirmation.setContentText("Are you sure you want to log out?"); 
        confirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO); // Set the custom buttons
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                
                // 2. Clear Session Data
                SessionManager.clearSession(); 
                System.out.println("Session cleared. Attempting logout and scene switch.");
                
                try {
                    // 3. Locate the Stage reliably using mainContentArea
                    Stage stage = (Stage) mainContentArea.getScene().getWindow();
                    
                    // 4. Load the Login FXML
                    final String loginResourcePath = "/views/login.fxml";
                    URL loginUrl = getClass().getResource(loginResourcePath);
                    
                    if (loginUrl == null) {
                        throw new IOException("Login FXML not found. Check path: " + loginResourcePath);
                    }
                    
                    FXMLLoader loader = new FXMLLoader(loginUrl);
                    Parent loginScene = loader.load(); 

                    // 5. Perform Scene Switch
                    stage.getScene().setRoot(loginScene);
                    stage.setTitle("Employee Management System - Login");
                    System.out.println("Successfully switched to Login Scene.");
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Logout Error", 
                              "Failed to load the login screen. Ensure 'login.fxml' is in the '/views/' folder.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "System Error", 
                              "An unexpected error occurred during logout: " + e.getMessage());
                }
            }
        });
    }
    /**
     * Retrieves a list of department names. (Kept for compatibility)
     */
    public ObservableList<String> getDepartments() {
        return FXCollections.observableArrayList(
            "IT",
            "HR",
            "Finance",
            "Sales",
            "Operations",
            "Marketing"
        );
    }
    
    /**
     * Helper method to display an alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}