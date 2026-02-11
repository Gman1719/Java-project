package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.net.URL;
import java.util.ResourceBundle;

// This class must be created to resolve the compilation errors in PayrollOfficerController
public class HrPayrollSupportController implements Initializable {

    // You must ensure your hr_payroll_support.fxml file contains a TabPane with fx:id="mainTabPane"
    @FXML 
    private TabPane mainTabPane; 

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialization logic for the tabbed content goes here
        System.out.println("HrPayrollSupportController initialized.");
    }

    /**
     * Public method used by PayrollOfficerController to switch tabs efficiently.
     * @param tabId The fx:id of the Tab to switch to (e.g., "processing_tab").
     */
    public void switchToTab(String tabId) {
        if (mainTabPane == null) {
            System.err.println("Error: mainTabPane is not injected in HrPayrollSupportController.");
            return;
        }

        // Iterate through all tabs to find the one matching the requested fx:id
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(tabId)) {
                mainTabPane.getSelectionModel().select(tab);
                break;
            }
        }
    }
}