package models;

import java.time.LocalDate;

public class CalendarEvent {
    // NEW: Fields needed for DB interaction and security checks
    private int eventId;
    private int employeeId; 
    
    private LocalDate date;
    private String time;
    private String eventDescription;
    private String eventType;
    private String details;

    // NEW Constructor to handle data loaded from the database
    public CalendarEvent(int eventId, int employeeId, LocalDate date, String time, String eventDescription, String eventType, String details) {
        this.eventId = eventId;
        this.employeeId = employeeId;
        this.date = date;
        this.time = time;
        this.eventDescription = eventDescription;
        this.eventType = eventType;
        this.details = details;
    }
    
    // Fallback Constructor (used for data not yet in DB, or testing)
    public CalendarEvent(LocalDate date, String time, String eventDescription, String eventType, String details) {
        this(-1, -1, date, time, eventDescription, eventType, details); // Calls the main constructor with defaults
    }

    // Getters (MANDATORY for TableView PropertyValueFactory and security checks)
    public int getEventId() { return eventId; }
    public int getEmployeeId() { return employeeId; }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public String getEventDescription() { return eventDescription; }
    public String getEventType() { return eventType; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "id=" + eventId +
                ", date=" + date +
                ", event='" + eventDescription + '\'' +
                ", type='" + eventType + '\'' +
                ", empId=" + employeeId +
                '}';
    }
}