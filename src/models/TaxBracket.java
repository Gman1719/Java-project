package models;

// Simple JavaFX Model for TableView
public class TaxBracket {
    private double minSalary;
    private double maxSalary;
    private double rate; // percentage

    public TaxBracket(double minSalary, double maxSalary, double rate) {
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.rate = rate;
    }

    // --- Getters and Setters (REQUIRED for PropertyValueFactory) ---
    public double getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(double minSalary) {
        this.minSalary = minSalary;
    }

    public double getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(double maxSalary) {
        this.maxSalary = maxSalary;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}