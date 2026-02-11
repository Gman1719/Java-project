package models;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // Added for 'requested_on' field

public class LeaveRequest {
    
    // 1. Properties (Matching the fields retrieved from the database)
    private final IntegerProperty requestId = new SimpleIntegerProperty();
    private final IntegerProperty empId = new SimpleIntegerProperty();
    private final StringProperty leaveType = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final DoubleProperty totalDays = new SimpleDoubleProperty(); // Added for total_days
    private final StringProperty reason = new SimpleStringProperty(); 
    private final StringProperty status = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> requestedOn = new SimpleObjectProperty<>(); // Added for requested_on

    // 2. CONSTRUCTOR (Primary constructor matching the 9 fields retrieved by the SQL query)
    public LeaveRequest(
        int requestId,
        int empId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        double totalDays,
        String reason,
        String status,
        LocalDateTime requestedOn) {
            
        this.requestId.set(requestId);
        this.empId.set(empId);
        this.leaveType.set(leaveType); 
        this.startDate.set(startDate); 
        this.endDate.set(endDate);
        this.totalDays.set(totalDays);
        this.reason.set(reason);
        this.status.set(status);
        this.requestedOn.set(requestedOn);
    }
    
    // 3. Optional: Keeping the 5-argument constructor if it's used elsewhere (e.g., for form submission)
    public LeaveRequest(String type, LocalDate startDate, LocalDate endDate, String reason, String status) {
        // Note: ID, EmpId, TotalDays, and RequestedOn will be default values (0/null)
        this.leaveType.set(type); 
        this.startDate.set(startDate); 
        this.endDate.set(endDate);
        this.reason.set(reason);
        this.status.set(status);
    }

    // 4. --- Property Methods (Required for strong binding) ---
    public IntegerProperty requestIdProperty() { return requestId; }
    public IntegerProperty empIdProperty() { return empId; }
    public StringProperty leaveTypeProperty() { return leaveType; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public DoubleProperty totalDaysProperty() { return totalDays; }
    public StringProperty reasonProperty() { return reason; }
    public StringProperty statusProperty() { return status; }
    public ObjectProperty<LocalDateTime> requestedOnProperty() { return requestedOn; }
    
    // 5. --- Getter Methods (For PropertyValueFactory in TableView and general use) ---
    public int getRequestId() { return requestId.get(); }
    public int getEmpId() { return empId.get(); }
    public String getLeaveType() { return leaveType.get(); }
    public LocalDate getStartDate() { return startDate.get(); }
    public LocalDate getEndDate() { return endDate.get(); }
    public double getTotalDays() { return totalDays.get(); }
    public String getReason() { return reason.get(); }
    public String getStatus() { return status.get(); }
    public LocalDateTime getRequestedOn() { return requestedOn.get(); }
    
    /**
     * Helper getter for displaying formatted dates in the table or details view.
     */
    public String getRequestedOnString() {
        if (requestedOn.get() != null) {
            return requestedOn.get().toLocalDate().toString();
        }
        return "N/A";
    }
}