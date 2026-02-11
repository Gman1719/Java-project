package models;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceRecord {

    private final StringProperty employeeName = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> clockInTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> clockOutTime = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty remarks = new SimpleStringProperty();

    // ✅ Updated Constructor to match your Controller's logic
    public AttendanceRecord(LocalDate date, LocalTime clockIn, LocalTime clockOut, String status, String remarks) {
        this.date.set(date);
        this.clockInTime.set(clockIn);
        this.clockOutTime.set(clockOut);
        this.status.set(status);
        this.remarks.set(remarks);
    }

    // ✅ Keep your original Constructor for EmployeeDAO compatibility
    public AttendanceRecord(String employeeName, LocalDate date, String status, String type, String remarks) {
        this.employeeName.set(employeeName);
        this.date.set(date);
        this.status.set(status);
        this.type.set(type);
        this.remarks.set(remarks);
    }

    /* ----------- Getters ----------- */

    public String getEmployeeName() { return employeeName.get(); }
    public LocalDate getDate() { return date.get(); }
    public LocalTime getClockInTime() { return clockInTime.get(); }
    public LocalTime getClockOutTime() { return clockOutTime.get(); }
    public String getStatus() { return status.get(); }
    public String getType() { return type.get(); }
    public String getRemarks() { return remarks.get(); }

    /* ----------- Property Methods ----------- */

    public StringProperty employeeNameProperty() { return employeeName; }
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public ObjectProperty<LocalTime> clockInTimeProperty() { return clockInTime; }
    public ObjectProperty<LocalTime> clockOutTimeProperty() { return clockOutTime; }
    public StringProperty statusProperty() { return status; }
    public StringProperty typeProperty() { return type; }
    public StringProperty remarksProperty() { return remarks; }
}