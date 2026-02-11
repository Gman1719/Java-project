package models;

import java.time.LocalDate;
import javafx.beans.property.*;

public class EmployeeRequest {

    private final IntegerProperty requestId;
    private final IntegerProperty employeeId;
    private final StringProperty employeeName;
    private final StringProperty requestType;
    private final ObjectProperty<LocalDate> submittedDate;
    private final ObjectProperty<LocalDate> startDate;
    private final ObjectProperty<LocalDate> endDate;
    private final StringProperty status;
    private final StringProperty justification;
    private final StringProperty hrComment;
    private final StringProperty oldValue;
    private final StringProperty newValue;

    // --- ADDED: Default Constructor (Required for Service/DAO) ---
    public EmployeeRequest() {
        this.requestId = new SimpleIntegerProperty();
        this.employeeId = new SimpleIntegerProperty();
        this.employeeName = new SimpleStringProperty();
        this.requestType = new SimpleStringProperty();
        this.submittedDate = new SimpleObjectProperty<>();
        this.startDate = new SimpleObjectProperty<>();
        this.endDate = new SimpleObjectProperty<>();
        this.status = new SimpleStringProperty();
        this.justification = new SimpleStringProperty();
        this.hrComment = new SimpleStringProperty("");
        this.oldValue = new SimpleStringProperty("");
        this.newValue = new SimpleStringProperty("");
    }

    // Main constructor (Your previous code)
    public EmployeeRequest(int requestId, int employeeId, String employeeName, String requestType,
                           LocalDate submittedDate, LocalDate startDate, LocalDate endDate,
                           String status, String justification) {
        this(); // Initialize properties via default constructor
        this.requestId.set(requestId);
        this.employeeId.set(employeeId);
        this.employeeName.set(employeeName);
        this.requestType.set(requestType);
        this.submittedDate.set(submittedDate);
        this.startDate.set(startDate);
        this.endDate.set(endDate);
        this.status.set(status);
        this.justification.set(justification);
    }

    // Full DB constructor (Your previous code)
    public EmployeeRequest(int requestId, int employeeId, String employeeName, String requestType,
                           LocalDate submittedDate, LocalDate startDate, LocalDate endDate,
                           String status, String justification, String hrComment,
                           String oldVal, String newVal) {
        this(requestId, employeeId, employeeName, requestType,
             submittedDate, startDate, endDate, status, justification);
        this.hrComment.set(hrComment == null ? "" : hrComment);
        this.oldValue.set(oldVal == null ? "" : oldVal);
        this.newValue.set(newVal == null ? "" : newVal);
    }

    // --- ADDED: Setter Methods (Required for Service/DAO) ---
    public void setRequestId(int value) { this.requestId.set(value); }
    public void setEmployeeId(int value) { this.employeeId.set(value); }
    public void setEmployeeName(String value) { this.employeeName.set(value); }
    public void setRequestType(String value) { this.requestType.set(value); }
    public void setSubmittedDate(LocalDate value) { this.submittedDate.set(value); }
    public void setStartDate(LocalDate value) { this.startDate.set(value); }
    public void setEndDate(LocalDate value) { this.endDate.set(value); }
    public void setJustification(String value) { this.justification.set(value); }
    public void setOldValue(String value) { this.oldValue.set(value); }
    public void setNewValue(String value) { this.newValue.set(value); }

    // Getters (Your previous code)
    public int getRequestId() { return requestId.get(); }
    public IntegerProperty requestIdProperty() { return requestId; }
    public int getEmployeeId() { return employeeId.get(); }
    public String getEmployeeName() { return employeeName.get(); }
    public StringProperty employeeNameProperty() { return employeeName; }
    public String getRequestType() { return requestType.get(); }
    public StringProperty requestTypeProperty() { return requestType; }
    public LocalDate getSubmittedDate() { return submittedDate.get(); }
    public ObjectProperty<LocalDate> submittedDateProperty() { return submittedDate; }
    public LocalDate getStartDate() { return startDate.get(); }
    public LocalDate getEndDate() { return endDate.get(); }
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }
    public String getJustification() { return justification.get(); }
    public StringProperty justificationProperty() { return justification; }
    public String getHrComment() { return hrComment.get(); }
    public void setHrComment(String value) { hrComment.set(value); }
    public String getOldValue() { return oldValue.get(); }
    public String getNewValue() { return newValue.get(); }
}