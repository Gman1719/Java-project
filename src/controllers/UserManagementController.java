package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Employee;
import utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

// IMPORTANT: Ensure EditEmployeeController is in the same package or imported correctly
// Assuming EditEmployeeController is in the controllers package or imported.

public class UserManagementController implements Initializable {

    @FXML private TableView<Employee> tblUsers;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colUsername;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colEmail;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, Void> colActions;

    @FXML private TextField txtSearch;

    /**
     * Initializes the controller, sets up table columns, and loads initial data.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupColumns();
            loadUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load user data on startup: " + e.getMessage(), Alert.AlertType.ERROR);
            tblUsers.setDisable(true);
        }
    }

    private void setupColumns() {
        // Configure column data mapping
        colId.setCellValueFactory(data -> data.getValue().employeeIdProperty().asObject());
        colName.setCellValueFactory(data -> data.getValue().fullNameProperty());
        colUsername.setCellValueFactory(data -> data.getValue().usernameProperty());
        colRole.setCellValueFactory(data -> data.getValue().roleNameProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());

        setupActionButtons();
    }

    private void setupActionButtons() {
        colActions.setCellFactory(column -> new TableCell<>() {

            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color:#0984e3; -fx-text-fill:white;");
                btnDelete.setStyle("-fx-background-color:#d63031; -fx-text-fill:white;");

                // Pass the Employee object for the selected row to the handlers
                btnEdit.setOnAction(e -> handleEditUser(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    /**
     * Fetches all users and their associated role names from the database.
     */
    private void loadUsers() throws SQLException {
        ObservableList<Employee> users = FXCollections.observableArrayList();

        String searchQuery = txtSearch.getText().toLowerCase().trim();
        
        // Base SQL query
        String sql = 
            "SELECT e.emp_id, u.user_id, u.username, u.first_name, u.last_name, u.email, u.phone, r.role_name " +
            "FROM employees e " +
            "JOIN users u ON e.user_id = u.user_id " +
            "JOIN roles r ON u.role_id = r.role_id";

        // Add filtering if a search term exists
        if (!searchQuery.isEmpty()) {
            sql += " WHERE LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? OR LOWER(u.username) LIKE ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            // Set search parameters if filtering
            if (!searchQuery.isEmpty()) {
                String searchParam = "%" + searchQuery + "%";
                pst.setString(1, searchParam);
                pst.setString(2, searchParam);
                pst.setString(3, searchParam);
            }
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Employee emp = new Employee();
                    emp.setEmployeeId(rs.getInt("emp_id"));
                    emp.setUserId(rs.getInt("user_id"));
                    emp.setUsername(rs.getString("username"));
                    emp.setFirstName(rs.getString("first_name"));
                    emp.setLastName(rs.getString("last_name"));
                    emp.setEmail(rs.getString("email"));
                    emp.setPhone(rs.getString("phone"));
                    emp.setRoleName(rs.getString("role_name"));

                    users.add(emp);
                }
            }

            tblUsers.setItems(users);
        }
    }

    // ------------------ EVENT HANDLERS ------------------

    @FXML
    private void handleSearch() {
        try {
            // Reloads users applying the filter from txtSearch
            loadUsers(); 
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to refresh user list: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Opens a modal window to create a new user.
     */
    @FXML
    private void handleAddUser() {
        try {
            // Load the FXML for the new user creation form
            // Assumes AddEmployee.fxml is correct and accessible
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddEmployee.fxml"));
            Parent root = loader.load();

            // Set up the stage
            Stage stage = new Stage();
            stage.setTitle("Add New User");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh table after modal is closed (assuming data insertion happened)
            loadUsers(); 

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Loading Error", "Could not load the Add User form. Ensure /views/AddEmployee.fxml exists.", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to refresh user list after operation.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Opens a modal window to edit the selected user.
     */
    private void handleEditUser(Employee emp) {
        try {
            // ⭐ CRITICAL FIX: Ensure the FXML path is correct and targets EditEmployeeView.fxml
            // NOTE: I am assuming the FXML file is named 'EditEmployeeView.fxml' based on the controller name.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EditEmployeeView.fxml"));
            
            // If the above line still throws the error, try loading from the current package or check the path:
            // FXMLLoader loader = new FXMLLoader(getClass().getResource("EditEmployeeView.fxml"));
            
            Parent root = loader.load();
            
            // Get the controller (EditEmployeeController) and pass the employee data to it
            EditEmployeeController controller = loader.getController();
            controller.setEmployeeToEdit(emp); 

            Stage stage = new Stage();
            stage.setTitle("Edit User: " + emp.getFullName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadUsers(); // Refresh the table

        } catch (IOException e) {
            e.printStackTrace();
            // Inform the user about the likely cause of the error (missing FXML file)
            showAlert("Loading Error", "Could not load the Edit User form. Ensure the FXML file is correctly named (e.g., EditEmployeeView.fxml) and is located at /views/EditEmployeeView.fxml.", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to refresh user list after operation.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Handles the deletion confirmation and process for a user.
     */
    private void handleDeleteUser(Employee emp) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete user: " + emp.getFullName() + "? This action is irreversible.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Deletion");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // Critical: Delete employee record first due to FK constraint, then user record.
                    deleteEmployeeRecord(emp.getEmployeeId());
                    deleteUserRecord(emp.getUserId());
                    loadUsers(); // Refresh the table
                    showAlert("Success", emp.getFullName() + " has been successfully deleted.", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Deletion Error", "Failed to delete user: " + e.getMessage() + ". Check database constraints.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ------------------ DAO OPERATIONS ------------------

    /** Deletes the user record from the 'users' table. */
    private void deleteUserRecord(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        }
    }
    
    /** Deletes the employee record from the 'employees' table. */
    private void deleteEmployeeRecord(int employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE emp_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, employeeId);
            pst.executeUpdate();
        }
    }

    // ------------------ UTILITY ------------------

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}