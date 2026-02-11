package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ProfileController {
    
    @FXML private TextField txtEmployeeId;
    @FXML private TextField txtFullName;
    @FXML private TextField txtRole;
    @FXML private TextField txtEmail;
    @FXML private TextField txtContact;

    @FXML
    public void saveProfile() {
        // Implement save logic here
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Profile changes saved successfully!");
        alert.showAndWait();
    }
    
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) txtFullName.getScene().getWindow();
        stage.close();
    }
}