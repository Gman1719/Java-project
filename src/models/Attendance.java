package models;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Attendance {
    private final IntegerProperty id; 
    private final StringProperty employeeName;
    private final ObjectProperty<LocalDate> date;
    private final StringProperty status;
    private final StringProperty leaveType; // Maps to 'colLeaveType' in your TableView
    private final StringProperty remarks;

    // Updated Constructor to include ID and LeaveType (Entry Type)
    public Attendance(int id, String employeeName, LocalDate date, String status, String leaveType, String remarks) {
        this.id = new SimpleIntegerProperty(id); 
        this.employeeName = new SimpleStringProperty(employeeName);
        this.date = new SimpleObjectProperty<>(date);
        this.status = new SimpleStringProperty(status);
        this.leaveType = new SimpleStringProperty(leaveType);
        this.remarks = new SimpleStringProperty(remarks);
    }

    // --- ID Property ---
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // --- Employee Name Property ---
    public String getEmployeeName() { return employeeName.get(); }
    public void setEmployeeName(String employeeName) { this.employeeName.set(employeeName); }
    public StringProperty employeeNameProperty() { return employeeName; }

    // --- Date Property ---
    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate date) { this.date.set(date); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    // --- Status Property ---
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    // --- Leave Type (Entry Type) Property ---
    public String getLeaveType() { return leaveType.get(); }
    public void setLeaveType(String leaveType) { this.leaveType.set(leaveType); }
    public StringProperty leaveTypeProperty() { return leaveType; }

    // --- Remarks Property ---
    public String getRemarks() { return remarks.get(); }
    public void setRemarks(String remarks) { this.remarks.set(remarks); }
    public StringProperty remarksProperty() { return remarks; }
}