package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // CRITICAL: Import Initializable
import javafx.scene.control.Button;
import javafx.scene.control.ListView; // CRITICAL: Import ListView
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable { 
    
    @FXML private ListView<String> notificationList; // fx:id MUST match FXML
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // FIX: Data is now loaded programmatically using an ObservableList
        ObservableList<String> items = FXCollections.observableArrayList(
            "Payroll for December 2025 is ready for final approval.",
            "Employee Joe Smith submitted a change request for bank details.",
            "Reminder: Tax rates update scheduled for next month.",
            "System maintenance complete."
        );
        notificationList.setItems(items);
        
        // Optional: Set a custom cell factory if you still want the styling from the Labels
        // notificationList.setCellFactory(new Callback<ListView<String>, ListCell<String>>() { ... });
    }
    
    @FXML
    private void closeWindow() {
        // Use notificationList (or any FXML element) to get the Scene/Stage
        Stage stage = (Stage) notificationList.getScene().getWindow();
        stage.close();
    }
}