package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

// *** NOTE: You must create Department and Role classes/records for this to compile/run correctly ***
// public record Department(String name, String head, ObservableList<Role> roles) {}
// public record Role(String name) {}

public class DepartmentManagementController implements Initializable {

    // FXML Elements
    @FXML private ListView<String> listViewDepartments;
    @FXML private TextField txtNewDepartmentName;
    @FXML private Label lblDepartmentTitle;
    @FXML private TextField txtDepartmentHead;
    @FXML private TableView<String> tblRoles;
    @FXML private TableColumn<String, String> colRoleName;
    @FXML private TableColumn<String, String> colRoleActions;
    @FXML private TextField txtNewRoleName;
    @FXML private VBox vboxRolesManagement;
    
    // Data structures
    // Using String for simplicity; replace with ObservableList<Department> in real app
    private final ObservableList<String> departmentNames = FXCollections.observableArrayList();
    private String selectedDepartment;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Setup mock data (Replace with database loading)
        loadMockDepartmentData(); 
        
        // 2. Setup department list view
        listViewDepartments.setItems(departmentNames);
        listViewDepartments.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleDepartmentSelection(newVal);
            }
        });
        
        // 3. Initially select the first department, if any
        if (!departmentNames.isEmpty()) {
            listViewDepartments.getSelectionModel().selectFirst();
        } else {
            vboxRolesManagement.setDisable(true);
        }
        
        // 4. Setup table (only one column needs configuration)
        colRoleName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        
        // Set up the action column
        setupRoleActionColumn();
    }

    private void loadMockDepartmentData() {
        // Mocking department data load from a service layer
        departmentNames.addAll(Arrays.asList("Marketing", "Engineering", "Human Resources", "Finance"));
    }
    
    // --- Department Actions ---

    private void handleDepartmentSelection(String deptName) {
        selectedDepartment = deptName;
        vboxRolesManagement.setDisable(false);
        lblDepartmentTitle.setText("Roles for: " + deptName);
        
        // Placeholder: Load roles and head for the selected department
        txtDepartmentHead.setText("Dr. Jane Doe (Mock Head)");
        
        // Mock roles based on selected department (Replace with database fetch)
        ObservableList<String> mockRoles;
        if ("Engineering".equals(deptName)) {
            mockRoles = FXCollections.observableArrayList("Software Engineer", "QA Tester", "Tech Lead");
        } else {
            mockRoles = FXCollections.observableArrayList("General Associate", "Manager");
        }
        tblRoles.setItems(mockRoles);
    }

    @FXML
    private void addNewDepartment() {
        String name = txtNewDepartmentName.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Department name cannot be empty.");
            return;
        }
        if (departmentNames.contains(name)) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Department '" + name + "' already exists.");
            return;
        }
        
        // In a real application, call service.addDepartment(name);
        departmentNames.add(name);
        txtNewDepartmentName.clear();
        listViewDepartments.getSelectionModel().select(name); // Select the newly added department
        showAlert(Alert.AlertType.INFORMATION, "Success", "Department added successfully.");
    }
    
    @FXML
    private void selectNewHead() {
        // Placeholder for opening a search modal to find and select a new department head
        showAlert(Alert.AlertType.INFORMATION, "Action", "A modal to select a new Department Head would open here.");
    }

    // --- Role Actions ---
    
    @FXML
    private void addNewRole() {
        String roleName = txtNewRoleName.getText().trim();
        if (selectedDepartment == null || roleName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a department and enter a role name.");
            return;
        }
        
        // In a real application, call service.addRole(selectedDepartment, roleName);
        if (tblRoles.getItems().contains(roleName)) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Role already exists in this department.");
            return;
        }
        tblRoles.getItems().add(roleName);
        txtNewRoleName.clear();
        showAlert(Alert.AlertType.INFORMATION, "Success", "Role '" + roleName + "' added to " + selectedDepartment + ".");
    }
    
    private void setupRoleActionColumn() {
        colRoleActions.setCellFactory(param -> new TableCell<String, String>() {
            final Button deleteButton = new Button("Delete");
            
            {
                deleteButton.getStyleClass().add("button-danger-mini");
                deleteButton.setOnAction(event -> {
                    String role = getTableView().getItems().get(getIndex());
                    
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
                            "Are you sure you want to delete the role: " + role + "?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            // In a real application, call service.deleteRole(selectedDepartment, role);
                            getTableView().getItems().remove(role);
                            showAlert(Alert.AlertType.INFORMATION, "Deleted", "Role deleted successfully.");
                        }
                    });
                });
            }
            
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
    }

    // --- Modal Control ---
    
    @FXML
    private void closeModal() {
        Stage stage = (Stage) ((Button) tblRoles.getParent().getParent().getParent().lookup(".button-secondary")).getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}