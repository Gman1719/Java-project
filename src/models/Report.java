package models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Report {
    
    // Using JavaFX StringProperty for automatic UI updates (essential for Tables)
    private final StringProperty col1;
    private final StringProperty col2;
    private final StringProperty col3;
    private final StringProperty col4;

    // Constructor to initialize the properties
    public Report(String col1, String col2, String col3, String col4) {
        this.col1 = new SimpleStringProperty(col1);
        this.col2 = new SimpleStringProperty(col2);
        this.col3 = new SimpleStringProperty(col3);
        this.col4 = new SimpleStringProperty(col4);
    }

    // --- Property Methods (Required by TableView PropertyValueFactory) ---

    // Getter for the StringProperty object
    public StringProperty col1Property() {
        return col1;
    }

    // Standard Getter for the plain String value
    public String getCol1() {
        return col1.get();
    }
    
    // Standard Setter (optional, but good practice)
    public void setCol1(String value) {
        col1.set(value);
    }

    // Repeat for col2, col3, and col4...

    public StringProperty col2Property() {
        return col2;
    }
    public String getCol2() {
        return col2.get();
    }
    public void setCol2(String value) {
        col2.set(value);
    }

    public StringProperty col3Property() {
        return col3;
    }
    public String getCol3() {
        return col3.get();
    }
    public void setCol3(String value) {
        col3.set(value);
    }

    public StringProperty col4Property() {
        return col4;
    }
    public String getCol4() {
        return col4.get();
    }
    public void setCol4(String value) {
        col4.set(value);
    }
}