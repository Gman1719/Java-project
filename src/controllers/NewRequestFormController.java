package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DBConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.scene.control.Alert.AlertType;

public class NewRequestFormController implements Initializable {

    private int employeeId; 

    @FXML private ComboBox<String> cbRequestType;
    @FXML private TextField txtSubject;
    @FXML private TextField txtAmount;
    @FXML private TextField txtOldAccount;
    @FXML private TextField txtNewAccount;
    @FXML private DatePicker dpRequiredDate;
    @FXML private TextArea txtDetails;
    @FXML private Label lblAmountLabel;
    @FXML private Label lblOldAccountLabel;
    @FXML private Label lblNewAccountLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Updated: Removed "Reimbursement" from the dropdown
        cbRequestType.getItems().addAll("Salary Advance", "Bank Account Change");
        
        cbRequestType.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFieldVisibility(newVal);
        });
        
        updateFieldVisibility(null);
        
        if (dpRequiredDate != null) {
            dpRequiredDate.setValue(LocalDate.now());
        }
    }
    
    private void updateFieldVisibility(String requestType) {
        // Reset visibility/management
        boolean isSalaryAdvance = "Salary Advance".equals(requestType);
        boolean isBankChange = "Bank Account Change".equals(requestType);

        // Amount fields (Only for Salary Advance now)
        if (txtAmount != null) {
            txtAmount.setVisible(isSalaryAdvance);
            txtAmount.setManaged(isSalaryAdvance);
        }
        if (lblAmountLabel != null) {
            lblAmountLabel.setVisible(isSalaryAdvance);
            lblAmountLabel.setManaged(isSalaryAdvance);
        }
        
        // Bank fields
        if (txtOldAccount != null) {
            txtOldAccount.setVisible(isBankChange);
            txtOldAccount.setManaged(isBankChange);
        }
        if (lblOldAccountLabel != null) {
            lblOldAccountLabel.setVisible(isBankChange);
            lblOldAccountLabel.setManaged(isBankChange);
        }
        if (txtNewAccount != null) {
            txtNewAccount.setVisible(isBankChange);
            txtNewAccount.setManaged(isBankChange);
        }
        if (lblNewAccountLabel != null) {
            lblNewAccountLabel.setVisible(isBankChange);
            lblNewAccountLabel.setManaged(isBankChange);
        }
    }
    
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }
    
    @FXML
    private void handleSubmitRequest() {
        if (!validateInput()) return;
        
        String type = cbRequestType.getValue();
        String reason = txtDetails.getText().trim();
        
        try {
            switch (type) {
                case "Salary Advance":
                    submitSalaryAdvance(reason);
                    break;
                case "Bank Account Change":
                    submitBankChange(reason);
                    break;
                default:
                    showAlert(AlertType.ERROR, "Error", "Unknown request type.");
                    return;
            }
            
            showAlert(AlertType.INFORMATION, "Success", "Your " + type + " request has been submitted for approval!");
            handleClose();
            
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Submission Failed", "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void submitSalaryAdvance(String reason) throws SQLException {
        String sql = "INSERT INTO salary_advance_requests (emp_id, amount, reason, status) " +
                     "VALUES (?, ?, ?, 'Pending')";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, employeeId);
            pst.setDouble(2, Double.parseDouble(txtAmount.getText().trim()));
            pst.setString(3, reason);
            pst.executeUpdate();
        }
    }
    
    private void submitBankChange(String reason) throws SQLException {
        String sql = "INSERT INTO bank_requests (emp_id, old_account, new_account, status) " +
                     "VALUES (?, ?, ?, 'Pending')";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, employeeId);
            pst.setString(2, txtOldAccount.getText().trim());
            pst.setString(3, txtNewAccount.getText().trim());
            pst.executeUpdate();
        }
    }

    private boolean validateInput() {
        if (cbRequestType.getValue() == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a request type.");
            return false;
        }
        
        if (txtDetails.getText().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Data", "Please provide details/reason for your request.");
            return false;
        }
        
        String type = cbRequestType.getValue();
        
        if ("Salary Advance".equals(type)) {
            if (txtAmount == null || txtAmount.getText().trim().isEmpty()) {
                showAlert(AlertType.WARNING, "Missing Data", "Please enter the amount.");
                return false;
            }
            try {
                double amount = Double.parseDouble(txtAmount.getText().trim());
                if (amount <= 0) {
                    showAlert(AlertType.WARNING, "Invalid Amount", "Amount must be greater than 0.");
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(AlertType.WARNING, "Invalid Amount", "Please enter a valid numeric amount.");
                return false;
            }
        } else if ("Bank Account Change".equals(type)) {
            if (txtOldAccount == null || txtOldAccount.getText().trim().isEmpty() ||
                txtNewAccount == null || txtNewAccount.getText().trim().isEmpty()) {
                showAlert(AlertType.WARNING, "Missing Data", "Please enter both old and new account numbers.");
                return false;
            }
        }
        
        return true;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtDetails.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}