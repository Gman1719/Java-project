package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane; 
import javafx.scene.layout.VBox; 
import javafx.scene.layout.HBox; 

import java.net.URL;
import java.util.ResourceBundle;

public class ModalController implements Initializable {
    
    // --- FXML Elements ---
    @FXML private Label lblModalTitle;
    @FXML private Button btnPrimaryAction; 
    @FXML private Button btnSecondaryAction;
    @FXML private TextField txtJobTitle;
    @FXML private TextArea txtJobDescription;
    @FXML private ComboBox<String> cmbDepartment; 
    @FXML private TextField txtHiringManager;
    @FXML private TextField txtSalaryMin;
    @FXML private TextField txtSalaryMax;
    @FXML private ComboBox<String> cmbStatus; 
    @FXML private Label lblDatePosted;
    @FXML private Label lblApplicantCount;
    @FXML private GridPane jobDetailsPane; // Corrected type for Job Details modal

    // --- State Management ---
    private String currentMode;
    private RecruitmentController parentController; // Assuming this is your parent controller
    
    public void setParentController(RecruitmentController controller) {
        this.parentController = controller;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialization for ComboBoxes (Resolves previous FXML errors)
        if (cmbStatus != null) {
            cmbStatus.setItems(FXCollections.observableArrayList("Active", "Draft", "Closed"));
        }
        if (cmbDepartment != null) {
            cmbDepartment.setItems(FXCollections.observableArrayList("IT", "HR", "Sales", "Marketing"));
        }
    }

    /**
     * Set up for the NEW JOB REQUISITION modal.
     */
    public void setupJobRequisition() {
        this.currentMode = "REQUISITION";
        
        if (lblModalTitle != null) {
            lblModalTitle.setText("New Job Requisition");
        }
        
        // Dynamic assignment is no longer necessary, but we can set the text:
        if (btnPrimaryAction != null) {
            btnPrimaryAction.setText("Submit Requisition");
            // We REMOVE the setOnAction line here because the action is now in the FXML
        }
        
        // Hide/disable elements specific to the Details modal
        if (cmbStatus != null) cmbStatus.setVisible(false);
        if (lblDatePosted != null) lblDatePosted.setVisible(false);
        if (lblApplicantCount != null) lblApplicantCount.setVisible(false);
    }

    /**
     * Set up for the JOB DETAILS (VIEW/EDIT) modal.
     */
    public void setupJobDetails(JobPosting job) {
        this.currentMode = "DETAILS";
        // ... (existing logic for setupJobDetails remains the same, must handle JobPosting model)
    }
    
    // --- Action Methods ---

    @FXML
    public void closeModal(ActionEvent event) {
        // Logic to close the modal window
        ((Button) event.getSource()).getScene().getWindow().hide();
    }

    // THIS METHOD IS NOW CALLED DIRECTLY BY THE FXML
    @FXML
    public void submitRequisition(ActionEvent event) {
        System.out.println("Submitting new job requisition...");
        
        // Placeholder Logic: Implement actual data gathering and API call here
        String jobTitle = txtJobTitle.getText();
        String department = cmbDepartment.getValue();
        System.out.println("Submitted Job: " + jobTitle + " for " + department);

        // Notify parent controller and close
        closeModal(event);
    }

    // FXML defined action for Job Details Modal
    @FXML
    public void closePosting(ActionEvent event) {
        // ... (existing logic)
    }

    // Method for DETAILS mode 
    @FXML // Ensure this is @FXML if called from the Job Details FXML, or ensure dynamic linking if not.
    public void saveJobChanges(ActionEvent event) {
        // ... (existing logic)
    }
}

// NOTE: Placeholder classes needed for compilation
class JobPosting {
    public String getTitle() { return "Senior Software Engineer"; }
    public String getDescription() { return "Responsible for core services..."; }
    public String getStatus() { return "Active"; }
    public String getDatePosted() { return "Dec 01, 2025"; }
    public int getApplicantCount() { return 15; }
}
class RecruitmentController { 
    // Class assumed to exist for ModalController to compile
}