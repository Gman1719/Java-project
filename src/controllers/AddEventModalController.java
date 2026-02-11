package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.regex.Pattern;

import utils.DBConnection; 
import models.CalendarEvent; 

public class AddEventModalController {

    @FXML private Label lblModalTitle;
    @FXML private DatePicker dpEventDate;
    @FXML private TextField txtEventTime;
    @FXML private TextField txtEventType;
    @FXML private TextField txtDescription;
    @FXML private TextArea txtDetails;
    @FXML private Label lblError;

    private int employeeId;
    private CalendarEvent eventToEdit;
    private boolean isEditMode = false;
    
    // Reference to the main calendar controller
    private CompanyCalendarController parentController; 
    
    // Regex for time validation (HH:MM or HH:MM:SS)
    private static final Pattern TIME_PATTERN = 
        Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?$");

    public void setMode(CompanyCalendarController parentController, int employeeId, CalendarEvent eventToEdit) {
        this.parentController = parentController;
        this.employeeId = employeeId;
        
        if (txtEventType != null) {
            // For Personal events, ensure the type field is disabled/fixed
            txtEventType.setText("Personal");
            txtEventType.setDisable(true); 
        }

        if (eventToEdit != null) {
            this.eventToEdit = eventToEdit;
            isEditMode = true;
            lblModalTitle.setText("Edit Personal Event (ID: " + eventToEdit.getEventId() + ")");
            populateFields(eventToEdit);
        } else {
             lblModalTitle.setText("Add Personal Event (Employee ID: " + employeeId + ")");
        }
        lblError.setText(""); // Clear initial errors
    }

    private void populateFields(CalendarEvent event) {
        dpEventDate.setValue(event.getDate());
        txtEventTime.setText(event.getTime());
        txtDescription.setText(event.getEventDescription());
        txtDetails.setText(event.getDetails());
    }

    @FXML
    private void handleSave(ActionEvent event) {
        lblError.setText("");

        LocalDate date = dpEventDate.getValue();
        String time = txtEventTime.getText().trim();
        String description = txtDescription.getText().trim();
        String details = txtDetails.getText().trim();

        // 1. INPUT VALIDATION 
        if (date == null) {
            lblError.setText("Event Date is required.");
            return;
        }
        if (time.isEmpty()) {
            lblError.setText("Event Time is required.");
            return;
        }
        if (description.isEmpty()) {
            lblError.setText("Event Description is required.");
            return;
        }
        if (description.length() > 255) { // Assuming DB column size limit
            lblError.setText("Description cannot exceed 255 characters.");
            return;
        }
        
        // Detailed time format check (e.g., 14:30 or 09:00:00)
        if (!TIME_PATTERN.matcher(time).matches()) {
            lblError.setText("Time must be in HH:MM or HH:MM:SS format (e.g., 14:30).");
            return;
        }

        // 2. Database Operation
        String sql;
        if (isEditMode) {
            sql = "UPDATE calendar_events SET event_date=?, event_time=?, description=?, details=? WHERE event_id=?";
        } else {
            // Inserting 'Personal' type and using the employeeId passed from the parent
            sql = "INSERT INTO calendar_events (emp_id, event_date, event_time, description, type, details) VALUES (?, ?, ?, ?, 'Personal', ?)";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            if (isEditMode) {
                // Update mode
                pst.setDate(1, java.sql.Date.valueOf(date));
                pst.setString(2, time);
                pst.setString(3, description);
                pst.setString(4, details);
                pst.setInt(5, eventToEdit.getEventId());
            } else {
                // Insert mode
                pst.setInt(1, employeeId); 
                pst.setDate(2, java.sql.Date.valueOf(date));
                pst.setString(3, time);
                pst.setString(4, description);
                // Type 'Personal' is hardcoded into the SQL
                pst.setString(5, details);
            }
            
            pst.executeUpdate();
            
            // 3. Close and Refresh Parent - This should now work without visibility errors
            // The visibility of dpCalendarDate and cbEventTypeFilter in the parent controller must be public.
            parentController.loadEventsData(parentController.dpCalendarDate.getValue(), parentController.cbEventTypeFilter.getValue()); 
            handleCancel(event);
            
        } catch (SQLException e) {
            String errorMsg = "Database Error: Failed to save event. Check logs.";

            // MySQL Error Code 1452: Foreign Key Constraint Violation
            if (e.getErrorCode() == 1452) {
                errorMsg = "Critical Error: The Employee ID (" + employeeId + ") used does not exist in the Employees table.";
            }

            lblError.setText(errorMsg);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }
}