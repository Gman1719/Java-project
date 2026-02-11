package models;
import java.time.LocalDate;

public class OtherRequest {
    private final String subject;
    private final LocalDate submittedDate;
    private final String status; 

    public OtherRequest(String subject, LocalDate submittedDate, String status) {
        this.subject = subject; 
        this.submittedDate = submittedDate; 
        this.status = status;
    }

    // Getters for TableView PropertyValueFactory
    public String getSubject() { return subject; }
    public LocalDate getSubmittedDate() { return submittedDate; }
    public String getStatus() { return status; }
}