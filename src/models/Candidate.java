package models;

// The Candidate model could hold detailed applicant information
public class Candidate {
    private int id;
    private String name;
    private String currentStage; // e.g., "New Applications", "HR Screen"
    
    public Candidate(int id, String name, String currentStage) {
        this.id = id;
        this.name = name;
        this.currentStage = currentStage;
    }
    
    // --- Getters ---
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCurrentStage() {
        return currentStage;
    }
}