package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Employee;
import services.EmployeeService;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class EmployeeRecordsController implements Initializable {

    @FXML private TableView<Employee> tblEmployees;
    @FXML private TableColumn<Employee, Integer> colEmployeeID;
    @FXML private TableColumn<Employee, String> colName, colDepartment, colPosition, colContact, colStatus;
    @FXML private TableColumn<Employee, Void> colActions;

    @FXML private ComboBox<String> cmbDepartmentFilter, cmbEmploymentTypeFilter, cmbStatusFilter;
    @FXML private DatePicker dpFromDate, dpToDate;
    @FXML private TextField txtSearchEmployee;
    @FXML private Label lblSelectedEmployee;

    private final EmployeeService employeeService = new EmployeeService(); 
    private final ObservableList<Employee> masterEmployeeList = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredEmployeeList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        loadEmployeeData();
        
        filteredEmployeeList = new FilteredList<>(masterEmployeeList, p -> true);
        tblEmployees.setItems(filteredEmployeeList);
        
        setupFilters();
        setupRowSelectionListener();
        setupSearchListener(); 
    }

    private void setupTableColumns() {
        colEmployeeID.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            {
                editButton.getStyleClass().add("action-button-mini");
                editButton.setOnAction(event -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    openModalWindow("/views/EditEmployeeView.fxml", "Edit Employee", emp); 
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editButton);
            }
        });
    }

    private void loadEmployeeData() {
        try {
            masterEmployeeList.setAll(employeeService.getAllEmployees()); 
        } catch (SQLException e) {
            showInlineFeedback("❌ Database Error: " + e.getMessage(), "error");
        }
    }

    // --- FXML EVENT HANDLERS ---

    @FXML
    public void generatePayrollForSelected(ActionEvent event) {
        Employee selected = tblEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInlineFeedback("⚠️ Select an employee first.", "warning");
            return;
        }
        // Open the new modal
        openModalWindow("/views/GeneratePayrollModal.fxml", "Generate Payroll", selected);
    }

    @FXML
    public void makeDepartmentHead(ActionEvent event) {
        Employee selected = tblEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInlineFeedback("⚠️ Select an employee first.", "warning");
            return;
        }

        try {
            showInlineFeedback("⭐ " + selected.getFullName() + " set as Department Head.", "success");
            loadEmployeeData(); 
        } catch (Exception e) {
            showInlineFeedback("❌ Failed to update role.", "error");
        }
    }

    @FXML
    public void changeEmployeeDepartment(ActionEvent event) {
        Employee selected = tblEmployees.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showInlineFeedback("Select an employee to transfer.", "warning");
            return;
        }

        try {
            ObservableList<String> departments = employeeService.getAllDepartmentNames();
            ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getDepartment(), departments);
            dialog.setTitle("Department Transfer");
            dialog.setHeaderText("Transferring: " + selected.getFullName());
            dialog.setContentText("Select new department:");

            dialog.showAndWait().ifPresent(newDept -> {
                try {
                    // This call now works because we added it to EmployeeService!
                    employeeService.updateEmployeeDepartment(selected.getEmployeeId(), newDept);
                    
                    showInlineFeedback("Transferred to " + newDept, "success");
                    loadEmployeeData(); // Refreshes the table to show the change
                } catch (SQLException e) {
                    showInlineFeedback("Database error during transfer.", "error");
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            showInlineFeedback("Could not load departments.", "error");
        }
    }

    @FXML
    public void refreshTable(ActionEvent event) {
        loadEmployeeData();
        showInlineFeedback("🔄 Data refreshed.", "success");
    }

    @FXML
    private void terminateEmployee(ActionEvent event) {
        Employee selected = tblEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInlineFeedback("⚠️ Select an employee first.", "warning");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Terminate " + selected.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    employeeService.terminateEmployee(selected.getEmployeeId());
                    loadEmployeeData();
                    showInlineFeedback("✅ Employee terminated.", "success");
                } catch (SQLException e) {
                    showInlineFeedback("❌ Database error.", "error");
                }
            }
        });
    }

    // --- NAVIGATION & MODALS ---

    public void openModalWindow(String fxmlPath, String title, Employee employee) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (employee != null) {
                // Check the path to see which controller to initialize
                if (fxmlPath.contains("GeneratePayrollModal")) {
                    GeneratePayrollModalController controller = loader.getController();
                    controller.initData(employee);
                } 
                else if (fxmlPath.contains("EditEmployeeView")) {
                    // FIXED: Correctly cast and call the specific method for EditEmployeeController
                    EditEmployeeController controller = loader.getController();
                    controller.setEmployeeToEdit(employee); 
                }
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the main table after the modal closes
            loadEmployeeData(); 
            
        } catch (IOException e) {
            showInlineFeedback("❌ System Error: Could not load the window.", "error");
            e.printStackTrace();
        }
    }

    @FXML private void handleSearch(ActionEvent event) { applyFilters(); }
    @FXML private void openAddEmployeeModal(ActionEvent event) { openModalWindow("/views/AddEmployee.fxml", "Add New", null); }
    @FXML private void manageDepartmentsAndRoles(ActionEvent event) { openModalWindow("/views/DeptRoles.fxml", "Management", null); }
    
    @FXML
    private void editSelectedEmployee(ActionEvent event) {
        Employee selected = tblEmployees.getSelectionModel().getSelectedItem();
        if (selected != null) openModalWindow("/views/EditEmployeeView.fxml", "Edit Profile", selected);
    }

    // --- FILTER LOGIC ---

    @FXML
    public void applyFilters() {
        String dept = cmbDepartmentFilter.getValue();
        String type = cmbEmploymentTypeFilter.getValue();
        String status = cmbStatusFilter.getValue();
        LocalDate from = dpFromDate.getValue();
        LocalDate to = dpToDate.getValue();
        String search = txtSearchEmployee.getText() == null ? "" : txtSearchEmployee.getText().toLowerCase();

        filteredEmployeeList.setPredicate(emp -> {
            boolean matchesDept = dept == null || (emp.getDepartment() != null && emp.getDepartment().equals(dept));
            boolean matchesStatus = status == null || (emp.getStatus() != null && emp.getStatus().equalsIgnoreCase(status));
            boolean matchesType = type == null || (emp.getEmploymentType() != null && emp.getEmploymentType().equals(type));
            
            boolean matchesDate = true;
            if (emp.getDateJoined() != null) {
                if (from != null && emp.getDateJoined().isBefore(from)) matchesDate = false;
                if (to != null && emp.getDateJoined().isAfter(to)) matchesDate = false;
            }

            boolean matchesSearch = search.isEmpty() || 
                                    emp.getFullName().toLowerCase().contains(search) ||
                                    String.valueOf(emp.getEmployeeId()).contains(search);

            return matchesDept && matchesStatus && matchesType && matchesDate && matchesSearch;
        });
    }

    @FXML
    public void clearFilters(ActionEvent event) {
        cmbDepartmentFilter.setValue(null);
        cmbEmploymentTypeFilter.setValue(null);
        cmbStatusFilter.setValue(null);
        dpFromDate.setValue(null);
        dpToDate.setValue(null);
        txtSearchEmployee.clear();
        applyFilters();
    }

    private void setupFilters() {
        try {
            cmbDepartmentFilter.setItems(employeeService.getAllDepartmentNames());
            cmbEmploymentTypeFilter.setItems(employeeService.getAllEmploymentTypes());
            cmbStatusFilter.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        } catch (SQLException e) {
            showInlineFeedback("Filter load failed.", "error");
        }

        cmbDepartmentFilter.valueProperty().addListener((o, old, newVal) -> applyFilters());
        cmbEmploymentTypeFilter.valueProperty().addListener((o, old, newVal) -> applyFilters());
        cmbStatusFilter.valueProperty().addListener((o, old, newVal) -> applyFilters());
        dpFromDate.valueProperty().addListener((o, old, newVal) -> applyFilters());
        dpToDate.valueProperty().addListener((o, old, newVal) -> applyFilters());
    }

    private void setupSearchListener() {
        txtSearchEmployee.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void setupRowSelectionListener() {
        tblEmployees.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) lblSelectedEmployee.setText("Selected: " + newVal.getFullName());
        });
    }

    private void showInlineFeedback(String message, String style) {
        lblSelectedEmployee.setText(message);
        String color = style.equals("error") ? "red" : (style.equals("warning") ? "orange" : "green");
        lblSelectedEmployee.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}