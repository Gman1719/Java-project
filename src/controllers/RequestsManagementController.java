package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent; 
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;

import models.EmployeeRequest; 
import services.RequestService;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.List;
import java.awt.Desktop;
import java.io.File;

public class RequestsManagementController implements Initializable {

    @FXML private TableView<EmployeeRequest> tblRequestsList;
    @FXML private TableColumn<EmployeeRequest, Integer> colID; 
    @FXML private TableColumn<EmployeeRequest, String> colEmployee; 
    @FXML private TableColumn<EmployeeRequest, String> colType; 
    @FXML private TableColumn<EmployeeRequest, LocalDate> colDate; 
    @FXML private TableColumn<EmployeeRequest, String> colStatus;

    @FXML private ComboBox<String> cmbStatusFilter; 
    @FXML private ComboBox<String> cmbTypeFilter; 
    @FXML private Label lblRequestCount;
    
    @FXML private HBox paneDetails; 
    @FXML private Label lblDetailEmployee, lblDetailType, lblDetailDate, lblDetailStatus, lblDetailRelatedDates;
    @FXML private Hyperlink linkAttachment;
    @FXML private TextArea txtAreaJustification, txtAreaHRComment;

    private RequestService requestService = new RequestService();
    private ObservableList<EmployeeRequest> masterRequestList = FXCollections.observableArrayList();
    private FilteredList<EmployeeRequest> filteredRequestList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Initialize data from Database
        loadMasterRequestsData(); 
        filteredRequestList = new FilteredList<>(masterRequestList, p -> true);
        tblRequestsList.setItems(filteredRequestList);

        // 2. Setup UI Components
        setupTableColumns();
        setupFilters();
        
        if (paneDetails != null) paneDetails.setVisible(false);
        tblRequestsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // 3. Listeners
        filteredRequestList.addListener((javafx.collections.ListChangeListener.Change<? extends models.EmployeeRequest> c) -> {
            updateRequestCount();
        });

        applyFilters(null); 
    }

    private void setupTableColumns() {
        // These MUST match the property names in your EmployeeRequest model
        colID.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("submittedDate")); 
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Handle selection to show details
        tblRequestsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showRequestDetails(newVal);
            }
        });
        
        setupStatusStyling();
    }

    private void showRequestDetails(EmployeeRequest request) {
        paneDetails.setVisible(true);

        lblDetailEmployee.setText(request.getEmployeeName());
        lblDetailType.setText(request.getRequestType());
        lblDetailDate.setText(request.getSubmittedDate().toString());
        lblDetailStatus.setText(request.getStatus());
        txtAreaHRComment.setText(request.getHrComment());
        
        // Logical Date Display
        if (request.getStartDate() != null && request.getEndDate() != null) {
            String dateRange = request.getStartDate().equals(request.getEndDate()) 
                ? request.getStartDate().toString() 
                : request.getStartDate() + " to " + request.getEndDate();
            lblDetailRelatedDates.setText(dateRange);
        }

        // Logic for Bank Change vs Others
        if ("Bank Change".equals(request.getRequestType())) {
            txtAreaJustification.setText("Old Account: " + request.getOldValue() + "\nNew Account: " + request.getNewValue());
        } else {
            txtAreaJustification.setText(request.getJustification());
        }
        
        // HR Comment interactivity
        boolean isProcessed = "Approved".equals(request.getStatus()) || "Rejected".equals(request.getStatus());
        txtAreaHRComment.setEditable(!isProcessed);
        
        linkAttachment.setVisible("Reimbursement".equals(request.getRequestType()));
    }

    @FXML
    private void approveRequest(ActionEvent event) {
        EmployeeRequest selected = tblRequestsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("Approved");
            selected.setHrComment(txtAreaHRComment.getText().trim());
            
            if (requestService.updateRequest(selected)) {
                tblRequestsList.refresh();
                hideDetails();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to save approval to database.");
            }
        }
    }

    @FXML
    private void rejectRequest(ActionEvent event) {
        EmployeeRequest selected = tblRequestsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String comment = txtAreaHRComment.getText().trim();
            if (comment.isEmpty()) {
                showAlert(AlertType.WARNING, "Input Required", "Please provide a reason for rejection in HR comments.");
                return;
            }
            selected.setStatus("Rejected");
            selected.setHrComment(comment);
            
            if (requestService.updateRequest(selected)) {
                tblRequestsList.refresh();
                hideDetails();
            }
        }
    }

    @FXML
    private void requestClarification(ActionEvent event) {
        EmployeeRequest selected = tblRequestsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String comment = txtAreaHRComment.getText().trim();
            if (comment.isEmpty()) {
                showAlert(AlertType.WARNING, "Input Required", "Explain what needs clarification in the HR comments.");
                return;
            }
            selected.setStatus("Clarification Needed");
            selected.setHrComment(comment);
            
            if (requestService.updateRequest(selected)) {
                tblRequestsList.refresh();
                hideDetails();
            }
        }
    }

    private void loadMasterRequestsData() {
        masterRequestList.clear(); // Clear old data
        List<EmployeeRequest> dataFromDb = requestService.getAllRequests();
        
        if (dataFromDb.isEmpty()) {
            System.out.println("DEBUG: Database returned 0 rows. Check your connection.");
        } else {
            System.out.println("DEBUG: Loaded " + dataFromDb.size() + " requests.");
            masterRequestList.addAll(dataFromDb);
        }
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        String status = cmbStatusFilter.getValue();
        String type = cmbTypeFilter.getValue();

        filteredRequestList.setPredicate(req -> {
            boolean matchesStatus = (status == null || "All Statuses".equals(status)) || req.getStatus().equals(status);
            boolean matchesType = (type == null || "All Types".equals(type)) || req.getRequestType().equals(type);
            return matchesStatus && matchesType;
        });
        updateRequestCount();
    }

    @FXML
    private void bulkApproveSelected(ActionEvent event) {
        List<EmployeeRequest> selectedItems = tblRequestsList.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) return;

        int count = 0;
        for (EmployeeRequest req : selectedItems) {
            if ("Pending".equals(req.getStatus())) {
                req.setStatus("Approved");
                req.setHrComment("Bulk approved by HR");
                requestService.updateRequest(req);
                count++;
            }
        }
        tblRequestsList.refresh();
        showAlert(AlertType.INFORMATION, "Bulk Action", count + " requests were approved.");
    }

    private void setupStatusStyling() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Approved" -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        case "Rejected" -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        case "Pending" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    private void viewAttachment(ActionEvent event) {
        // Logic to open a directory or file - implementation depends on your file storage logic
        showAlert(AlertType.INFORMATION, "Attachment", "Fetching document from secure storage...");
    }

    @FXML private void hideDetails() { paneDetails.setVisible(false); tblRequestsList.getSelectionModel().clearSelection(); }
    
    private void setupFilters() {
        cmbTypeFilter.setItems(FXCollections.observableArrayList("All Types", "Leave", "Bank Change", "Salary Advance", "Reimbursement"));
        cmbStatusFilter.setItems(FXCollections.observableArrayList("All Statuses", "Pending", "Approved", "Rejected", "Clarification Needed"));
        cmbTypeFilter.getSelectionModel().selectFirst();
        cmbStatusFilter.getSelectionModel().selectFirst();
    }

    private void updateRequestCount() {
        lblRequestCount.setText("Total Visible Requests: " + filteredRequestList.size());
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}