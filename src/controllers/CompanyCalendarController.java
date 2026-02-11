package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import utils.DBConnection; 
import models.CalendarEvent;   

public class CompanyCalendarController implements Initializable {

    // FXML fields - Must be public for AddEventModalController access
    @FXML public DatePicker dpCalendarDate; 
    @FXML public ComboBox<String> cbEventTypeFilter; 
    @FXML public TableView<CalendarEvent> tblEvents; 
    
    @FXML private TableColumn<CalendarEvent, LocalDate> colDate;
    @FXML private TableColumn<CalendarEvent, String> colTime;
    @FXML private TableColumn<CalendarEvent, String> colEvent;
    @FXML private TableColumn<CalendarEvent, String> colType;
    @FXML private TableColumn<CalendarEvent, String> colDetails;
    @FXML private Button btnAddEvent;
    @FXML private Button btnEditEvent;
    @FXML private Button btnDeleteEvent;

    private ObservableList<CalendarEvent> allEvents = FXCollections.observableArrayList();
    private int employeeId = 3; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Setup Table Columns
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colEvent.setCellValueFactory(new PropertyValueFactory<>("eventDescription"));
        colType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        // 2. Setup Filters and Default Date
        setupFilters();
        
        // Start with no date selected to load ALL events initially.
        dpCalendarDate.setValue(null); 
        
        // 3. Initial Load
        loadEventsData(dpCalendarDate.getValue(), cbEventTypeFilter.getValue());
    }
    
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
        loadEventsData(dpCalendarDate.getValue(), cbEventTypeFilter.getValue());
    }

    private void setupFilters() {
        cbEventTypeFilter.getItems().addAll("All Events", "Holiday", "Company", "Personal");
        cbEventTypeFilter.setValue("All Events");
        
        // Both date picker and combo box call the same handler
        cbEventTypeFilter.setOnAction(this::handleFilterAndDateChange); 
    }

    @FXML
    private void handleFilterAndDateChange(ActionEvent event) {
        // This method triggers a reload of the data whenever date or filter changes
        loadEventsData(dpCalendarDate.getValue(), cbEventTypeFilter.getValue());
    }

    /**
     * Fetches events from the database based on dateFilter and typeFilter.
     * @param dateFilter The LocalDate to filter by (null for all dates).
     * @param typeFilter The event type string to filter by ("All Events" for all types).
     */
    public void loadEventsData(LocalDate dateFilter, String typeFilter) {
        allEvents.clear();

        // Base SQL: Company/Holiday events for everyone OR Personal events for the specific employee
        String sql = "SELECT event_id, emp_id, event_date, event_time, description, type, details " +
                     "FROM calendar_events " +
                     "WHERE (type IN ('Company', 'Holiday') OR (type = 'Personal' AND emp_id = ?))";

        // IMPORTANT: The date filter uses 'event_date = ?' which works correctly 
        // if event_date is a DATE type in the database.
        if (dateFilter != null) {
            sql += " AND event_date = ?";
        }
        
        if (typeFilter != null && !typeFilter.equals("All Events")) {
            sql += " AND type = ?";
        }

        sql += " ORDER BY event_date, event_time";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            // 1. Always set the employee ID for the WHERE clause
            pst.setInt(paramIndex++, employeeId);

            // 2. Conditionally set the date parameter
            if (dateFilter != null) {
                // Converting LocalDate to java.sql.Date
                pst.setDate(paramIndex++, java.sql.Date.valueOf(dateFilter));
            }

            // 3. Conditionally set the type parameter
            if (typeFilter != null && !typeFilter.equals("All Events")) {
                pst.setString(paramIndex++, typeFilter);
            }

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                allEvents.add(new CalendarEvent(
                    rs.getInt("event_id"),       
                    rs.getInt("emp_id"),         
                    rs.getDate("event_date").toLocalDate(),
                    rs.getString("event_time"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("details")
                ));
            }

            tblEvents.setItems(allEvents);
            if (allEvents.isEmpty()) {
                tblEvents.setPlaceholder(new Label("No calendar events found for this selection."));
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error", 
                      "Failed to retrieve events. Check if your database is running and tables exist.");
            e.printStackTrace();
        }
    }

    // --- CRUD and Modal Methods ---
    // (Rest of the CRUD and utility methods remain the same as the previous correct version)
    
    @FXML
    private void openAddEventModal(ActionEvent event) {
        showEventModal(null); 
    }

    @FXML
    private void handleEditEvent(ActionEvent event) {
        CalendarEvent selectedEvent = tblEvents.getSelectionModel().getSelectedItem();
        if (selectedEvent == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select an event to edit.");
            return;
        }

        if (!selectedEvent.getEventType().equals("Personal") || selectedEvent.getEmployeeId() != this.employeeId) {
            showAlert(AlertType.ERROR, "Access Denied", "You can only edit your own personal events.");
            return;
        }

        showEventModal(selectedEvent);
    }

    @FXML
    private void handleDeleteEvent(ActionEvent event) {
        CalendarEvent selectedEvent = tblEvents.getSelectionModel().getSelectedItem();
        if (selectedEvent == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select an event to delete.");
            return;
        }

        if (!selectedEvent.getEventType().equals("Personal") || selectedEvent.getEmployeeId() != this.employeeId) {
            showAlert(AlertType.ERROR, "Access Denied", "You can only delete your own personal events.");
            return;
        }

        Optional<ButtonType> result = new Alert(AlertType.CONFIRMATION, 
            "Are you sure you want to delete the event: " + selectedEvent.getEventDescription() + "?",
            ButtonType.YES, ButtonType.NO).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            deleteEventFromDatabase(selectedEvent);
        }
    }

    private void showEventModal(CalendarEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/views/AddEventModal.fxml");
            
            if (fxmlUrl == null) {
                 showAlert(AlertType.ERROR, "Configuration Error", 
                           "Cannot find AddEventModal.fxml. Check that the file is in 'src/main/resources/view/'.");
                 return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            AddEventModalController modalController = loader.getController();
            modalController.setMode(this, employeeId, event); 
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(event == null ? "Add Event" : "Edit Event");
            stage.initModality(Modality.APPLICATION_MODAL); 
            stage.initOwner(tblEvents.getScene().getWindow()); 
            stage.showAndWait();
            
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "System Error", "Error loading Add/Edit Event window FXML.");
            e.printStackTrace();
        }
    }
    
    private void deleteEventFromDatabase(CalendarEvent event) {
        String sql = "DELETE FROM calendar_events WHERE event_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, event.getEventId());
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                showAlert(AlertType.INFORMATION, "Success", "Event deleted successfully.");
                loadEventsData(dpCalendarDate.getValue(), cbEventTypeFilter.getValue()); 
            } else {
                 showAlert(AlertType.WARNING, "Error", "Event could not be found in the database.");
            }
            
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to delete event.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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