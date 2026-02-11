package models;

import java.time.LocalDateTime;

public class Notification {
    private int notificationId;
    private int userId;         // Added for DB logic
    private String title;
    private String message;
    private String target;
    private String status;      // Added for DB logic
    private LocalDateTime createdAt;

    public Notification() {}

    // Your ORIGINAL constructor (Keep this for your other files)
    public Notification(int notificationId, String title, String message, String target, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.target = target;
        this.createdAt = createdAt;
    }

    // The DATABASE constructor (This fixes the error in your while loop)
    public Notification(int notificationId, int userId, String title, String message, String target, String status, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.target = target;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- Getters & Setters ---
    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isUnread() {
        return "Unread".equalsIgnoreCase(this.status);
    }
}