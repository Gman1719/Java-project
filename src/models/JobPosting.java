package models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class JobPosting {
    
    // Properties used by TableView columns (RecruitmentController: setupJobPostingTable)
    private final SimpleStringProperty title;
    private final SimpleStringProperty department;
    private final SimpleStringProperty hiringManager;
    private final SimpleIntegerProperty applicantCount;
    private final SimpleStringProperty status;
    private final SimpleStringProperty postedDate; // Could also be SimpleObjectProperty<LocalDate>

    public JobPosting(String title, String department, String hiringManager, int applicantCount, String status, String postedDate) {
        this.title = new SimpleStringProperty(title);
        this.department = new SimpleStringProperty(department);
        this.hiringManager = new SimpleStringProperty(hiringManager);
        this.applicantCount = new SimpleIntegerProperty(applicantCount);
        this.status = new SimpleStringProperty(status);
        this.postedDate = new SimpleStringProperty(postedDate);
    }
    
    // --- Getters for PropertyValueFactory ---
    
    public String getTitle() {
        return title.get();
    }
    // Note: JavaFX can also use the Property objects, but standard POJO getters are safer.
    
    public String getDepartment() {
        return department.get();
    }

    public String getHiringManager() {
        return hiringManager.get();
    }

    public int getApplicantCount() {
        return applicantCount.get();
    }

    public String getStatus() {
        return status.get();
    }
    
    public String getPostedDate() {
        return postedDate.get();
    }
}