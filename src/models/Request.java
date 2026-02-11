package models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDate;

public class Request {
    
    // 1. Define JavaFX Properties for TableView binding
    private final StringProperty employeeName;
    private final StringProperty type;
    private final ObjectProperty<LocalDate> date;
    private final StringProperty status;
    private final StringProperty remarks;

    /**
     * Constructor for creating a new Request instance.
     */
    public Request(String employeeName, String type, LocalDate date, String status, String remarks) {
        this.employeeName = new SimpleStringProperty(employeeName);
        this.type = new SimpleStringProperty(type);
        this.date = new SimpleObjectProperty<>(date);
        this.status = new SimpleStringProperty(status);
        this.remarks = new SimpleStringProperty(remarks);
    }
    
    // 2. REQUIRED JavaFX Property Getters (Used by TableView for binding)

    public StringProperty employeeNameProperty() {
        return employeeName;
    }

    public StringProperty typeProperty() {
        return type;
    }
    
    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty remarksProperty() {
        return remarks;
    }
    
    // 3. REQUIRED Standard Getters and Setter (Used by Controller logic for reading/updating data)
    
    // Standard Getters (Used by onFilter and showInfo logic)
    public String getEmployeeName() {
        return employeeName.get();
    }

    public String getType() {
        return type.get();
    }
    
    public LocalDate getDate() {
        return date.get();
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public String getRemarks() {
        return remarks.get();
    }

    // Standard Setter (CRITICAL: Used by approveRequest and rejectRequest to change status)
    public void setStatus(String newStatus) {
        this.status.set(newStatus);
    }
}