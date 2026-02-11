package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import models.PayrollRecord;
import services.PayrollService;
import utils.DBConnection;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.Optional;

/**
 * Integrated Controller for Payroll Monitoring.
 * Features: Database fetching, dynamic filtering, summary statistics, and audit logging.
 */
public class PayrollValidationController implements Initializable {

    // --- Table Columns ---
    @FXML private TableView<PayrollRecord> tblPayroll;
    @FXML private TableColumn<PayrollRecord, Integer> colID;
    @FXML private TableColumn<PayrollRecord, String> colName;
    @FXML private TableColumn<PayrollRecord, String> colDept;
    @FXML private TableColumn<PayrollRecord, Integer> colAttendance;
    @FXML private TableColumn<PayrollRecord, Integer> colLeaves;
    @FXML private TableColumn<PayrollRecord, Double> colNetSalary;
    @FXML private TableColumn<PayrollRecord, String> colStatus;

    // --- Stat Labels ---
    @FXML private Label lblTotalPayroll, lblPendingCount, lblApprovedLeaves;

    // --- Filter Elements ---
    @FXML private ComboBox<String> cmbMonthFilter, cmbDeptFilter, cmbStatusFilter;
    @FXML private TextField txtSearchEmployee;

    // --- Detail Sidebar Elements ---
    @FXML private Label lblDetailName, lblDetailAttendance, lblDetailLeaves;
    @FXML private Label lblBasicSalary, lblAllowances, lblDeductions, lblNetPayable;

    private ObservableList<PayrollRecord> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFilters();
        refreshDataFromDatabase(); 
        
        // Listener for Detail Pane updates
        tblPayroll.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateDetailPane(newVal);
        });
    }

    /**
     * Pulls data directly from the enhanced PayrollService based on ComboBox selections.
     */
    @FXML
    public void refreshDataFromDatabase() {
        System.out.println("Fetching live data from Database...");
        
        String month = cmbMonthFilter.getValue();
        String dept = cmbDeptFilter.getValue();
        String status = cmbStatusFilter.getValue();

        // Use the Service to fetch data (handling cases where filters might be 'All')
        masterData.setAll(PayrollService.getFilteredPayroll(
            (month == null) ? "All Months" : month,
            (dept == null) ? "All Departments" : dept,
            (status == null) ? "All Status" : status
        ));
        
        tblPayroll.setItems(masterData);
        updateSummaryStats();
    }

    private void setupTable() {
        colID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colAttendance.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        colLeaves.setCellValueFactory(new PropertyValueFactory<>("leaves"));
        colNetSalary.setCellValueFactory(new PropertyValueFactory<>("netSalary"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Advanced cell factory for Currency formatting
        colNetSalary.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", price));
                }
            }
        });
    }

    /**
     * Populates filters with fixed month data and dynamic Department data from SQL.
     */
    private void setupFilters() {
        // 1. Populate Months
        cmbMonthFilter.setItems(FXCollections.observableArrayList(
            "All Months", "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        ));
        
        // 2. Populate Departments dynamically from DB
        ObservableList<String> depts = FXCollections.observableArrayList("All Departments");
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT dept_name FROM departments")) {
            while(rs.next()) {
                depts.add(rs.getString("dept_name"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading departments: " + e.getMessage());
        }
        cmbDeptFilter.setItems(depts);

        // 3. Populate Status
        cmbStatusFilter.setItems(FXCollections.observableArrayList("All Status", "Processed", "Pending", "Verified"));
        
        // Set Defaults
        cmbMonthFilter.getSelectionModel().select("All Months");
        cmbDeptFilter.getSelectionModel().selectFirst();
        cmbStatusFilter.getSelectionModel().selectFirst();

        // 4. Add listener for real-time search
        txtSearchEmployee.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void updateSummaryStats() {
        double total = masterData.stream().mapToDouble(PayrollRecord::getNetSalary).sum();
        long pending = masterData.stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        int leaves = masterData.stream().mapToInt(PayrollRecord::getLeaves).sum();

        lblTotalPayroll.setText(String.format("₱%,.2f", total));
        lblPendingCount.setText(pending + " Employees");
        lblApprovedLeaves.setText(leaves + " Days");
    }

    private void updateDetailPane(PayrollRecord record) {
        lblDetailName.setText(record.getName());
        lblDetailAttendance.setText(record.getAttendance() + " Days");
        lblDetailLeaves.setText(record.getLeaves() + " Days");
        lblBasicSalary.setText(String.format("₱%,.2f", record.getBasicSalary()));
        lblAllowances.setText(String.format("+ ₱%,.2f", record.getAllowances()));
        lblDeductions.setText(String.format("- ₱%,.2f", record.getDeductions()));
        lblNetPayable.setText(String.format("₱%,.2f", record.getNetSalary()));
    }

    @FXML
    private void applyFilters() {
        String searchText = txtSearchEmployee.getText().toLowerCase();
        String selectedDept = cmbDeptFilter.getValue();
        String selectedStatus = cmbStatusFilter.getValue();

        FilteredList<PayrollRecord> filteredData = new FilteredList<>(masterData, record -> {
            boolean matchesSearch = searchText.isEmpty() || record.getName().toLowerCase().contains(searchText);
            boolean matchesDept = selectedDept == null || selectedDept.equals("All Departments") || record.getDepartment().equals(selectedDept);
            boolean matchesStatus = selectedStatus == null || selectedStatus.equals("All Status") || record.getStatus().equals(selectedStatus);
            return matchesSearch && matchesDept && matchesStatus;
        });
        tblPayroll.setItems(filteredData);
    }

    @FXML
    private void onExportPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payroll Report");
        fileChooser.setInitialFileName("Payroll_Export.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        File file = fileChooser.showSaveDialog(tblPayroll.getScene().getWindow());
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Name,Dept,Attendance,Leaves,NetSalary,Status");
                for (PayrollRecord r : masterData) {
                    writer.println(String.format("%d,%s,%s,%d,%d,%.2f,%s",
                        r.getId(), r.getName(), r.getDepartment(), r.getAttendance(), 
                        r.getLeaves(), r.getNetSalary(), r.getStatus()));
                }
                showSimpleAlert("Export Success", "Report saved to: " + file.getPath());
            } catch (Exception e) {
                showSimpleAlert("Error", "Failed to export report.");
            }
        }
    }

    @FXML
    private void exportReport(ActionEvent event) {
        onExportPDF(event);
    }

    @FXML
    private void contactPayrollOfficer(ActionEvent event) {
        PayrollRecord selected = tblPayroll.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSimpleAlert("Selection Required", "Please select an employee row to flag.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Flag Discrepancy");
        dialog.setHeaderText("Issue for: " + selected.getName());
        dialog.setContentText("Explain the error:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(issue -> {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("INSERT INTO audit_log (user_id, action) VALUES (?, ?)")) {
                pst.setInt(1, 1); // Mock user ID
                pst.setString(2, "Flagged Payroll ID " + selected.getId() + ": " + issue);
                pst.executeUpdate();
                showSimpleAlert("System Notified", "Flag logged successfully.");
            } catch (SQLException e) {
                showSimpleAlert("Database Error", "Could not log flag.");
            }
        });
    }

    @FXML
    private void viewFullPayslip(ActionEvent event) {
        PayrollRecord selected = tblPayroll.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showSimpleAlert("Payslip View", "Loading secure preview for " + selected.getName());
        } else {
            showSimpleAlert("Selection Required", "Please select an employee.");
        }
    }

    private void showSimpleAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}