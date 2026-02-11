package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.TaxBracket;
import utils.DBConnection;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class SystemSettingsController implements Initializable {

    // --- FXML UI COMPONENTS ---
    @FXML private TextField txtCompanyName, txtGlobalTaxRate, txtWorkHoursPerDay;
    @FXML private CheckBox chkLockPayroll, chkApprovalRequired;
    @FXML private TextField txtOvertimeMultiplier, txtLateThreshold, txtAnnualLeaveLimit;
    @FXML private TextField txtMinPasswordLength, txtMaxLoginAttempts;
    @FXML private ComboBox<String> comboPayrollCycle;
    @FXML private Label lblDBStatus;

    // --- TAX TABLE COMPONENTS ---
    @FXML private TableView<TaxBracket> tblTaxBrackets;
    @FXML private TableColumn<TaxBracket, Double> colMinSalary;
    @FXML private TableColumn<TaxBracket, Double> colMaxSalary;
    @FXML private TableColumn<TaxBracket, Double> colTaxRate;
    @FXML private TableColumn<TaxBracket, Void> colTaxActions;
    
    private ObservableList<TaxBracket> taxBracketList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTaxTable();
        setupComboBoxes();
        loadSettingsFromDatabase();
    }

    private void setupComboBoxes() {
        if (comboPayrollCycle != null) {
            comboPayrollCycle.setItems(FXCollections.observableArrayList("Monthly", "Bi-Weekly", "Weekly"));
        }
    }

    /**
     * Re-implemented: Sets up the TableView with Edit/Delete buttons
     */
    private void setupTaxTable() {
        colMinSalary.setCellValueFactory(new PropertyValueFactory<>("minSalary"));
        colMaxSalary.setCellValueFactory(new PropertyValueFactory<>("maxSalary"));
        colTaxRate.setCellValueFactory(new PropertyValueFactory<>("rate"));
        
        colTaxActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            private final Button btnEdit = new Button("Edit");
            private final HBox pane = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color:#4F46E5; -fx-text-fill:white; -fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-cursor:hand;");

                btnDelete.setOnAction(event -> {
                    TaxBracket bracket = getTableView().getItems().get(getIndex());
                    handleDeleteTaxBracket(bracket);
                });
                btnEdit.setOnAction(event -> {
                    TaxBracket bracket = getTableView().getItems().get(getIndex());
                    handleEditTaxBracket(bracket);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tblTaxBrackets.setItems(taxBracketList);
    }

    private void loadSettingsFromDatabase() {
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Load General Settings
            String sql = "SELECT * FROM settings LIMIT 1";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            
            if (rs.next()) {
                txtCompanyName.setText(rs.getString("company_name"));
                txtGlobalTaxRate.setText(String.valueOf(rs.getDouble("tax_rate")));
                txtWorkHoursPerDay.setText(String.valueOf(rs.getInt("working_hours")));
                chkLockPayroll.setSelected(rs.getBoolean("payroll_locked"));
                txtOvertimeMultiplier.setText(String.valueOf(rs.getDouble("overtime_multiplier")));
                comboPayrollCycle.setValue(rs.getString("payroll_cycle"));
                txtLateThreshold.setText(String.valueOf(rs.getInt("late_threshold")));
                txtAnnualLeaveLimit.setText(String.valueOf(rs.getInt("annual_leave_limit")));
                chkApprovalRequired.setSelected(rs.getBoolean("auto_approval"));
                txtMinPasswordLength.setText(String.valueOf(rs.getInt("min_password_length")));
                txtMaxLoginAttempts.setText(String.valueOf(rs.getInt("max_login_attempts")));
            }

            if (lblDBStatus != null) lblDBStatus.setText("Database Status: Connected (" + conn.getCatalog() + ")");

            // 2. Load Tax Brackets (Placeholders for now, or fetch from your tax table)
            taxBracketList.setAll(
                new TaxBracket(0.0, 10000.0, 0.0),
                new TaxBracket(10000.01, 20000.0, 10.0),
                new TaxBracket(20000.01, Double.MAX_VALUE, 20.0)
            );

        } catch (SQLException e) {
            e.printStackTrace();
            if (lblDBStatus != null) lblDBStatus.setText("Database Status: Disconnected");
        }
    }
    
    @FXML
    private void handleSaveAllSettings(ActionEvent event) {
        String updateSQL = "UPDATE settings SET company_name=?, tax_rate=?, working_hours=?, " +
                           "payroll_locked=?, overtime_multiplier=?, payroll_cycle=?, " +
                           "late_threshold=?, annual_leave_limit=?, auto_approval=?, " +
                           "min_password_length=?, max_login_attempts=? WHERE id=1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setString(1, txtCompanyName.getText());
            pstmt.setDouble(2, Double.parseDouble(txtGlobalTaxRate.getText()));
            pstmt.setInt(3, Integer.parseInt(txtWorkHoursPerDay.getText()));
            pstmt.setBoolean(4, chkLockPayroll.isSelected());
            pstmt.setDouble(5, Double.parseDouble(txtOvertimeMultiplier.getText()));
            pstmt.setString(6, comboPayrollCycle.getValue());
            pstmt.setInt(7, Integer.parseInt(txtLateThreshold.getText()));
            pstmt.setInt(8, Integer.parseInt(txtAnnualLeaveLimit.getText()));
            pstmt.setBoolean(9, chkApprovalRequired.isSelected());
            pstmt.setInt(10, Integer.parseInt(txtMinPasswordLength.getText()));
            pstmt.setInt(11, Integer.parseInt(txtMaxLoginAttempts.getText()));

            pstmt.executeUpdate();
            logAction("System settings updated by Admin");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Configuration saved to database.");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Check your inputs: " + e.getMessage());
        }
    }

    // --- TAX BRACKET MODAL LOGIC ---

    @FXML
    private void handleAddTaxBracket(ActionEvent event) {
        openTaxBracketModal(null);
    }
    
    private void handleEditTaxBracket(TaxBracket bracket) {
        openTaxBracketModal(bracket);
    }
    
    private void openTaxBracketModal(TaxBracket bracketToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/TaxBracketModal.fxml"));
            Parent parent = loader.load();
            
            // Re-linked the controller and the list
            TaxBracketModalController controller = loader.getController();
            controller.setTaxBracketList(taxBracketList);
            if (bracketToEdit != null) controller.setBracket(bracketToEdit);
            
            Stage stage = new Stage();
            stage.setTitle(bracketToEdit == null ? "Add New Tax Bracket" : "Edit Tax Bracket");
            stage.setScene(new Scene(parent));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load TaxBracketModal.fxml");
        }
    }
    
    private void handleDeleteTaxBracket(TaxBracket bracket) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setContentText("Remove this tax bracket?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            taxBracketList.remove(bracket);
        }
    }

    private void logAction(String action) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO audit_log (user_id, action) VALUES (1, ?)")) {
            pstmt.setString(1, action);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}