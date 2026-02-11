package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Department; 
import utils.DBConnection; 

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class DepartmentController implements Initializable {

    // --- FXML Fields ---
    @FXML private TableView<Department> tblDepartments;
    @FXML private TableColumn<Department, Integer> colId;
    @FXML private TableColumn<Department, String> colName;
    @FXML private TableColumn<Department, String> colDescription; 
    @FXML private TableColumn<Department, Void> colActions; 

    // Input Fields for Add/Edit Form (on the same page)
    @FXML private TextField txtSearch; 
    @FXML private TextField txtDepartmentName; 
    @FXML private TextArea txtDescription; 
    @FXML private Button btnSave; 

    private ObservableList<Department> departmentList = FXCollections.observableArrayList();
    private FilteredList<Department> filteredDepartmentList; 
    private Department selectedDepartment = null;

    // --- Initialization ---
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        setupTable();
        // The loadDepartments call is safe because it now handles its own exception
        loadDepartments();
            
        // Setup Filtering
        filteredDepartmentList = new FilteredList<>(departmentList, p -> true);
        tblDepartments.setItems(filteredDepartmentList);
        
        // Link search field to the filter handler
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
    }

    private void setupTable() {
        // Map data columns (Must match the property names/getters in the Department model)
        colId.setCellValueFactory(new PropertyValueFactory<>("deptId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description")); 

        // CRITICAL: Set CellValueFactory for the actions column
        colActions.setCellValueFactory(param -> new SimpleObjectProperty<>(null));

        // Set up the actions column with Edit and Delete buttons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                // Styling
                editButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-background-radius: 4;");
                deleteButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 4;");
                
                // --- EDIT Functionality (Loads data into form) ---
                editButton.setOnAction(event -> {
                    Department deptToEdit = getTableView().getItems().get(getIndex());
                    // 1. Set the selected object reference
                    selectedDepartment = deptToEdit;
                    // 2. Load data into the form fields
                    txtDepartmentName.setText(deptToEdit.getName());
                    txtDescription.setText(deptToEdit.getDescription()); 
                    // 3. Change button text to indicate update mode
                    btnSave.setText("Update Department");
                });

                // --- DELETE Functionality ---
                deleteButton.setOnAction(event -> {
                    Department deptToDelete = getTableView().getItems().get(getIndex());
                    if (confirmDelete(deptToDelete.getName())) {
                        handleDelete(deptToDelete);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionsPane = new HBox(5, editButton, deleteButton);
                    setGraphic(actionsPane);
                }
            }
        });
    }

    // --- FXML Action Handlers ---

    @FXML
    private void handleSearch() {
        // Filters the list based on search text in name or description
        String searchText = txtSearch.getText().toLowerCase();
        
        filteredDepartmentList.setPredicate(department -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            if (department.getName().toLowerCase().contains(searchText)) {
                return true; 
            } else if (department.getDescription() != null && department.getDescription().toLowerCase().contains(searchText)) {
                return true; 
            }
            return false;
        });
    }
    
    @FXML
    private void handleClear() {
        // Resets form to Create mode
        txtDepartmentName.clear();
        txtDescription.clear();
        btnSave.setText("Save Department");
        selectedDepartment = null;
        // Optionally clear search filter as well
        txtSearch.clear();
        handleSearch(); 
        
        showAlert(Alert.AlertType.INFORMATION, "Form Reset", "Input fields have been cleared. Ready to save a new department.");
    }

    @FXML
    private void handleSave() {
        // Dispatcher for ADD/CREATE or UPDATE
        String name = txtDepartmentName.getText().trim();
        String description = txtDescription.getText().trim();
        
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Department name cannot be empty.");
            return;
        }

        if (selectedDepartment == null) {
            handleAdd(name, description); // Create
        } else {
            handleUpdate(name, description); // Update
        }
        
        handleClear();
    }

    // --- Database Operations (CRUD) ---

    /** Fetches all departments from the DB (MADE PUBLIC for external call) */
    public void loadDepartments() {
        departmentList.clear();
        String query = "SELECT dept_id, dept_name, description FROM departments";

        // ⭐ FIXED: The method now catches the SQLException internally
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Department dept = new Department();
                dept.setDeptId(rs.getInt("dept_id"));
                dept.setName(rs.getString("dept_name"));
                String description = rs.getString("description"); 
                // Ensure description is not null for display safety
                dept.setDescription(description != null ? description : "No Description"); 
                departmentList.add(dept);
            }
        } catch (SQLException e) {
             System.err.println("Database error loading departments: " + e.getMessage());
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load departments: " + e.getMessage());
        }
    }

    /** Inserts a new department */
    private void handleAdd(String name, String description) {
        String query = "INSERT INTO departments (dept_name, description) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, name);
            ps.setString(2, description);
            if (ps.executeUpdate() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Department added successfully.");
                loadDepartments();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add department: " + e.getMessage());
        }
    }

    /** Updates an existing department */
    private void handleUpdate(String name, String description) {
        String query = "UPDATE departments SET dept_name = ?, description = ? WHERE dept_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, selectedDepartment.getDeptId());
            if (ps.executeUpdate() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Department updated successfully.");
                loadDepartments();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update department: " + e.getMessage());
        }
    }

    /** Deletes a department */
    private void handleDelete(Department dept) {
        String query = "DELETE FROM departments WHERE dept_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, dept.getDeptId());
            if (ps.executeUpdate() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Department deleted successfully.");
                loadDepartments();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete department: " + e.getMessage());
        }
    }

    // --- Utility Methods ---

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirmDelete(String departmentName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Department: " + departmentName);
        alert.setContentText("Are you sure you want to delete this department? This will set the department_id to NULL for any linked employees.");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}