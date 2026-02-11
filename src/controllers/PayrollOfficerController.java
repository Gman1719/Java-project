package controllers;

import dao.EmployeeDAO;
import models.Employee;
import models.PayrollRecord;
import utils.DBConnection;
import utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;


public class PayrollOfficerController implements Initializable {

    // Main Container (Required for Logout/Scene switching)
	@FXML private BorderPane mainPane;

    // Labels & Metrics
    @FXML private Label lblOfficerName, lblTotalEmployees, lblPendingCount, 
                        lblAttendanceErrors, lblTotalPayout, lblStatusMessage, 
                        lblLiveNetPreview, lblSystemDate;

    // Filters & Navigation
    @FXML private ComboBox<String> comboMonth;
    @FXML private ComboBox<Integer> comboYear;
    @FXML private TabPane payrollTabPane;
    @FXML private TextField searchEmployee;

    // Table Setup
    @FXML private TableView<PayrollRecord> tblPayroll;
    @FXML private TableColumn<PayrollRecord, String> colEmpName, colStatus;
    @FXML private TableColumn<PayrollRecord, Double> colBaseSalary, colAllowances, colDeductions, colNetSalary;
    @FXML private TableColumn<PayrollRecord, Void> colActions;
    
    // Archive Table
    @FXML private TableView<PayrollRecord> tblPayslipHistory;
    @FXML private TableColumn<PayrollRecord, String> colHistMonth, colHistEmp;
    @FXML private TableColumn<PayrollRecord, Double> colHistAmount;

    // Structure Editor Fields
    @FXML private ListView<Employee> listEmployees;
    @FXML private TextField txtBaseSalary, txtAllowances, txtBonus, txtDeductions, txtInsurance;
    
    // Global Config Fields
    @FXML private TextField cfgTaxRate, cfgSocialRate, cfgCurrency;

    private final ObservableList<PayrollRecord> payrollData = FXCollections.observableArrayList();
    private final ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblSystemDate.setText(LocalDate.now().toString());
        // Set default currency if empty
        if(cfgCurrency != null && cfgCurrency.getText().isEmpty()) cfgCurrency.setText("$");
        
        setupTable();
        setupFilters();
        loadOfficerInfo();
        loadGlobalSettingsFromDB();

        loadEmployeeList(); 
        loadDashboardMetrics(); // Call this AFTER loading employees to get real count
 

        
        listEmployees.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadEmployeeStructure(newVal);
        });

        // Search Filter Logic
        FilteredList<Employee> filteredData = new FilteredList<>(employeeList, p -> true);
        searchEmployee.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(emp -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return emp.getFirstName().toLowerCase().contains(lowerCaseFilter) ||
                       emp.getLastName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        listEmployees.setItems(filteredData);
    }

    private void setupTable() {
        colEmpName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colBaseSalary.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colAllowances.setCellValueFactory(new PropertyValueFactory<>("allowances"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colNetSalary.setCellValueFactory(new PropertyValueFactory<>("netSalary"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        if (colHistMonth != null)
            colHistMonth.setCellValueFactory(new PropertyValueFactory<>("period"));

        if (colHistEmp != null)
            colHistEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        if (colHistAmount != null)
            colHistAmount.setCellValueFactory(new PropertyValueFactory<>("netSalary"));


        tblPayroll.setItems(payrollData);
        addTableButtons();
        
        tblPayroll.setRowFactory(tv -> new TableRow<PayrollRecord>() {
            @Override
            protected void updateItem(PayrollRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("Pending".equals(item.getStatus())) {
                    setStyle("-fx-background-color: #fff9db;"); 
                } else if ("Error".equals(item.getStatus())) {
                    setStyle("-fx-background-color: #fff5f5;"); 
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void addTableButtons() {
        Callback<TableColumn<PayrollRecord, Void>, TableCell<PayrollRecord, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnView = new Button("View");
            {
                btnView.getStyleClass().add("action-button-small");
                btnView.setOnAction(event -> {
                    PayrollRecord data = getTableView().getItems().get(getIndex());
                    showSimpleAlert(AlertType.INFORMATION, "Record Detail", "Viewing details for: " + data.getEmployeeName());
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void setupFilters() {
        comboMonth.setItems(FXCollections.observableArrayList(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        ));

        comboYear.setItems(FXCollections.observableArrayList(2024, 2025, 2026));

        // ✅ FIXED: Proper Month Name (January, not JANUARY)
        String currentMonth = LocalDate.now()
                .getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        comboMonth.setValue(currentMonth);
        comboYear.setValue(LocalDate.now().getYear());
    }


    private void loadOfficerInfo() {
        if (SessionManager.getCurrentEmployee() != null) {
            lblOfficerName.setText(SessionManager.getCurrentEmployee().getFirstName() + " " + SessionManager.getCurrentEmployee().getLastName());
        }
    }

    @FXML
    private void handleLiveUpdate() {
        double base = parseSafe(txtBaseSalary.getText());
        double allowances = parseSafe(txtAllowances.getText());
        double bonus = parseSafe(txtBonus.getText());
        double deductions = parseSafe(txtDeductions.getText());
        double insurance = parseSafe(txtInsurance.getText());

        double taxRate = parseSafe(cfgTaxRate.getText()) / 100;
        double socialRate = parseSafe(cfgSocialRate.getText()) / 100;

        double gross = base + allowances + bonus;
        double tax = gross * taxRate;
        double social = gross * socialRate;

        double net = gross - (tax + social + deductions + insurance);

        String symbol = cfgCurrency.getText();
        lblLiveNetPreview.setText(String.format("%s %.2f", symbol, net));
    }

    private double parseSafe(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    private void loadPayrollData() {
        payrollData.clear();

        String sql = """
            SELECT CONCAT(u.first_name,' ',u.last_name) AS name,
                   p.base_salary, p.allowances, p.deductions, p.tax,
                   p.net_salary, p.status
            FROM payroll p
            JOIN employees e ON p.emp_id = e.emp_id
            JOIN users u ON e.user_id = u.user_id
            WHERE p.month=? AND p.year=?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, comboMonth.getValue());
            ps.setInt(2, comboYear.getValue());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                payrollData.add(new PayrollRecord(
                    rs.getString("name"),
                    rs.getDouble("base_salary"),
                    rs.getDouble("allowances"),
                    rs.getDouble("deductions") + rs.getDouble("tax"),
                    rs.getDouble("net_salary"),
                    rs.getString("status")
                ));
            }

            lblStatusMessage.setText("Payroll loaded successfully");

        } catch (SQLException e) {
            lblStatusMessage.setText("Payroll load error");
        }
    }


    private void loadEmployeeList() {
        employeeList.setAll(employeeDAO.getAllEmployees());
    }

    private void loadEmployeeStructure(Employee emp) {
        txtBaseSalary.setText(String.valueOf(emp.getSalary()));
        txtAllowances.setText("500.00"); 
        txtBonus.setText("0.00");
        txtDeductions.setText("100.00");
        txtInsurance.setText("50.00");
        handleLiveUpdate();
    }

    @FXML
    private void handleBatchGenerate() {
        if (employeeList.isEmpty()) {
            showSimpleAlert(AlertType.WARNING, "No Data", "No employees found to process.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION, 
            "Generate payroll for " + employeeList.size() + " employees?", 
            ButtonType.YES, ButtonType.NO);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // MATCHES YOUR SCHEMA: Removed net_salary (auto-calc) and included status
                String sql = "INSERT INTO payroll (emp_id, month, year, base_salary, allowances, deductions, tax, status) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    double taxRate = parseSafe(cfgTaxRate.getText()) / 100;

                    for (Employee emp : employeeList) {
                        double base = emp.getSalary();
                        double allowances = 500.0; 
                        double deductions = 100.0;
                        double calculatedTax = (base + allowances) * taxRate;

                        pstmt.setInt(1, emp.getId()); // Must exist in 'employees' table
                        pstmt.setString(2, comboMonth.getValue());
                        pstmt.setInt(3, comboYear.getValue());
                        pstmt.setDouble(4, base);
                        pstmt.setDouble(5, allowances);
                        pstmt.setDouble(6, deductions);
                        pstmt.setDouble(7, calculatedTax);
                        pstmt.setString(8, "Processed"); 
                        
                        pstmt.addBatch();
                    }
                    
                    pstmt.executeBatch();
                    lblStatusMessage.setText("Batch processing successful!");
                    loadPayrollData(); // This will fetch the auto-calculated net_salary
                    
                } catch (SQLException e) {
                    // This will now catch and explain any remaining mismatches
                    showSimpleAlert(AlertType.ERROR, "Database Error", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadPayslipArchive() {

        ObservableList<PayrollRecord> list = FXCollections.observableArrayList();

        String sql = """
            SELECT CONCAT(u.first_name,' ',u.last_name) AS emp,
                   CONCAT(p.month,' ',p.year) AS period,
                   p.net_salary
            FROM payroll p
            JOIN employees e ON p.emp_id = e.emp_id
            JOIN users u ON e.user_id = u.user_id
            ORDER BY p.generated_on DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new PayrollRecord(
                    rs.getString("emp"),
                    0.0,
                    0.0,
                    0.0,
                    rs.getDouble("net_salary"),
                    rs.getString("period") // ✅ Month/Year
                ));
            }

            tblPayslipHistory.setItems(list);

        } catch (SQLException e) {
            showSimpleAlert(Alert.AlertType.ERROR,
                    "Archive Error",
                    "Failed to load payslip archive");
        }
    }


    
    @FXML
    private void handleLockMonth() {
        if (SessionManager.getCurrentEmployee() == null) return;

        String month = comboMonth.getValue();
        int year = comboYear.getValue();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Security Lock");
        confirm.setHeaderText("Locking Period: " + month + " " + year);
        confirm.setContentText("Once locked, no further modifications can be made to this period. Proceed?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String action = "LOCKED PAYROLL PERIOD: " + month + " " + year;
                logAction(SessionManager.getCurrentEmployee().getUserId(), action);
                lblStatusMessage.setText("Period " + month + " " + year + " is now SECURED.");
                showSimpleAlert(AlertType.INFORMATION, "Success", "Payroll period locked successfully.");
            }
        });
    }

    @FXML
    private void handleSaveStructure() {
        Employee selected = listEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSimpleAlert(AlertType.WARNING, "Selection Required", "Please select an employee to update.");
            return;
        }

        try {
            double newSalary = Double.parseDouble(txtBaseSalary.getText());
            String sql = "UPDATE employees SET salary = ? WHERE emp_id = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, newSalary);
                pstmt.setInt(2, selected.getId());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    lblStatusMessage.setText("Updated salary for " + selected.getFirstName());
                    logAction(SessionManager.getCurrentEmployee().getUserId(), 
                              "Updated Salary Structure for Emp ID: " + selected.getId());
                    showSimpleAlert(AlertType.INFORMATION, "Data Saved", "Salary structure successfully updated.");
                    loadEmployeeList(); 
                }
            }
        } catch (NumberFormatException e) {
            showSimpleAlert(AlertType.ERROR, "Invalid Input", "Please enter a valid numeric salary.");
        } catch (SQLException e) {
            e.printStackTrace();
            showSimpleAlert(AlertType.ERROR, "Database Error", "Could not save structure.");
        }
    }

    @FXML
    private void saveGlobalSettings() {
        String sql = "UPDATE settings SET tax_rate = ? WHERE id = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            double taxRate = parseSafe(cfgTaxRate.getText());
            pstmt.setDouble(1, taxRate);
            pstmt.executeUpdate();
            
            lblStatusMessage.setText("Global Configuration Updated");
            showSimpleAlert(AlertType.INFORMATION, "Configuration Updated", "Global Tax and Social rates updated.");
        } catch (SQLException e) {
            showSimpleAlert(AlertType.ERROR, "Error", "Failed to update global settings.");
        }
    }

    @FXML
    private void showNotifications() {
        StringBuilder notes = new StringBuilder("--- Recent Payroll Alerts ---\n");
        String sql = "SELECT title, message FROM notifications WHERE user_id = ? AND status = 'Unread' LIMIT 5";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, SessionManager.getCurrentEmployee().getUserId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notes.append("• ").append(rs.getString("title")).append(": ")
                     .append(rs.getString("message")).append("\n");
            }
            
            if (notes.length() < 30) notes.append("No new notifications.");
            showSimpleAlert(AlertType.INFORMATION, "Payroll Alerts", notes.toString());
            
        } catch (SQLException e) {
            showSimpleAlert(AlertType.INFORMATION, "Notifications", "Unable to fetch alerts.");
        }
    }

    private void loadDashboardMetrics() {

        try (Connection conn = DBConnection.getConnection()) {

            // 1. Total active employees
            try (Statement stmt = conn.createStatement();
                 ResultSet rs1 = stmt.executeQuery(
                     "SELECT COUNT(*) FROM employees WHERE status='Active'")) {

                if (rs1.next()) {
                    lblTotalEmployees.setText(rs1.getString(1));
                }
            }

            // 2. Pending payroll count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs2 = stmt.executeQuery(
                     "SELECT COUNT(*) FROM payroll WHERE status='Pending'")) {

                if (rs2.next()) {
                    lblPendingCount.setText(rs2.getString(1));
                }
            }

            // 3. Attendance errors (Absent)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs3 = stmt.executeQuery(
                     "SELECT COUNT(*) FROM attendance WHERE status='Absent'")) {

                if (rs3.next()) {
                    lblAttendanceErrors.setText(rs3.getString(1));
                }
            }

            // 4. Total payout
            try (Statement stmt = conn.createStatement();
                 ResultSet rs4 = stmt.executeQuery(
                     "SELECT COALESCE(SUM(net_salary),0) FROM payroll")) {

                if (rs4.next()) {
                    lblTotalPayout.setText(
                        cfgCurrency.getText() + " " + rs4.getDouble(1)
                    );
                }
            }

            lblStatusMessage.setText("Dashboard metrics loaded");

        } catch (SQLException e) {
            e.printStackTrace();
            lblStatusMessage.setText("Dashboard load failed");
        }
    }



    // ⭐ NEW: Added Missing logAction method
    private void logAction(int userId, String action) {
        String sql = "INSERT INTO audit_log (user_id, action) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Audit Log Failed: " + e.getMessage());
        }
    }
    private void updateNavigationStyle(String activeText) {
        // Assuming your sidebar buttons are inside a VBox or similar
        // We update the status message as a breadcrumb
        lblStatusMessage.setText("System > " + activeText);
    }
    
    private void loadGlobalSettingsFromDB() {
        String sql = "SELECT tax_rate, social_rate, currency_symbol FROM settings LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                cfgTaxRate.setText(String.valueOf(rs.getDouble("tax_rate")));
                cfgSocialRate.setText(String.valueOf(rs.getDouble("social_rate")));
                cfgCurrency.setText(rs.getString("currency_symbol"));
            }

        } catch (SQLException e) {
            lblStatusMessage.setText("Failed to load global settings");
        }
    }
    
    private void loadAuditHistory() {
        // Match SQL: table 'audit_log' columns 'user_id', 'action', 'log_time'
        payrollData.clear(); // Reusing the list for display
        String sql = "SELECT a.action, a.log_time, u.username " +
                     "FROM audit_log a JOIN users u ON a.user_id = u.user_id " +
                     "ORDER BY log_time DESC LIMIT 50";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                // Here we adapt the audit data to your TableView columns
                // You might need a specific AuditRecord model, or use strings
                System.out.println("Log: " + rs.getString("action") + " at " + rs.getTimestamp("log_time"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @FXML 
    private void switchToRunTab() {
        // 1. Navigate to the Run Payroll tab
        payrollTabPane.getSelectionModel().select(0);
        
        // 2. Trigger data fetch automatically for current selection
        loadPayrollData();
        
        // 3. Update UI
        updateNavigationStyle("Run Payroll");
    }
    @FXML 
    private void showDashboard() {
        payrollTabPane.getSelectionModel().select(0);
        loadDashboardMetrics();
        loadPayrollData(); // ✅ THIS WAS MISSING
        updateNavigationStyle("Dashboard");
    }

    @FXML 
    private void showSalaryStructures() {
        // 1. Navigate to the Structure Editor tab (Index 1)
        payrollTabPane.getSelectionModel().select(1);
        loadEmployeeList();
        updateNavigationStyle("Salary Structures");
        if (listEmployees.getSelectionModel().getSelectedItem() == null) {
            lblStatusMessage.setText("Standing by: Select an employee from the list to edit.");
        }
    }
    @FXML 
    private void showTaxSettings() {
        payrollTabPane.getSelectionModel().select(2); 
        updateNavigationStyle("Tax & Global Rules");
    }
    @FXML 
    private void showAuditLogs() {
        // 1. Navigate to Archive tab (Index 3)
        payrollTabPane.getSelectionModel().select(3);
        
        // 2. Refresh the audit/history table
        loadPayslipArchive();

        
        // 3. Update UI
        updateNavigationStyle("Reports & Archive");
    }
    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        lblStatusMessage.setText("Logging out...");
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            // Fixed: If mainPane is null, we find the stage via comboMonth
            javafx.stage.Stage stage = (javafx.stage.Stage) comboMonth.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
        } catch (Exception e) {
            System.exit(0); 
        }
    }
 


    private void showSimpleAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void handleReset() {
        txtAllowances.setText("0"); 
        txtBonus.setText("0"); 
        txtDeductions.setText("0"); 
        txtInsurance.setText("0");
        lblStatusMessage.setText("Editor Fields Cleared");
        handleLiveUpdate();
    }

    @FXML private void exportExcel() { 
        // Implement Excel export functionality
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Payroll to Excel");
            fileChooser.setInitialFileName("Payroll_" + comboMonth.getValue() + "_" + comboYear.getValue() + ".csv");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            Stage stage = (Stage) comboMonth.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    // Write header
                    writer.println("Employee Name,Base Salary,Allowances,Deductions,Net Salary,Status");
                    
                    // Write data
                    for (PayrollRecord record : payrollData) {
                        writer.println(String.format("%s,%.2f,%.2f,%.2f,%.2f,%s",
                            record.getEmployeeName(),
                            record.getBaseSalary(),
                            record.getAllowances(),
                            record.getDeductions(),
                            record.getNetSalary(),
                            record.getStatus()
                        ));
                    }
                    
                    showSimpleAlert(AlertType.INFORMATION, "Export Successful", 
                        "Payroll data exported to: " + file.getAbsolutePath());
                } catch (Exception e) {
                    showSimpleAlert(AlertType.ERROR, "Export Failed", 
                        "Failed to write file: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            showSimpleAlert(AlertType.ERROR, "Export Failed", 
                "An error occurred: " + e.getMessage());
        }
    }
    
    @FXML private void exportPDF() { 
        showSimpleAlert(AlertType.INFORMATION, "Export", 
            "PDF export functionality will generate payroll reports. Coming soon!");
    }

}