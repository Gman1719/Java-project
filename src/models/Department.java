package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Department {
    
    private IntegerProperty deptId;
    private StringProperty name;
    private StringProperty description;

    // Constructors
    public Department() {
        this.deptId = new SimpleIntegerProperty();
        // ⭐ FIX: Removed the extra 'new' keyword here:
        this.name = new SimpleStringProperty(); // CORRECTED LINE (was: this.name = new new SimpleStringProperty();)
        this.description = new SimpleStringProperty();
    }

    public Department(int deptId, String name, String description) {
        this.deptId = new SimpleIntegerProperty(deptId);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
    }

    // Getters
    public int getDeptId() { return deptId.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }

    // Property Accessors (Crucial for JavaFX TableView binding)
    public IntegerProperty deptIdProperty() { return deptId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }

    // Setters
    public void setDeptId(int deptId) { this.deptId.set(deptId); }
    public void setName(String name) { this.name.set(name); }
    public void setDescription(String description) { this.description.set(description); }
}