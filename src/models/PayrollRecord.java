package models;

import javafx.beans.property.*;

public class PayrollRecord {
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty netSalary = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty payPeriod = new SimpleStringProperty();
    
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final DoubleProperty basicSalary = new SimpleDoubleProperty();
    private final DoubleProperty allowances = new SimpleDoubleProperty();
    private final DoubleProperty deductions = new SimpleDoubleProperty();

    // Constructor: Matches fetchPayrollData() (4 arguments)
    public PayrollRecord(String employeeName, String period, double netSalary, String status) {
        this.name.set(employeeName);
        this.payPeriod.set(period);
        this.netSalary.set(netSalary);
        this.status.set(status);
    }

    // Constructor for Dashboard/Detailed view (9 arguments)
    public PayrollRecord(int id, String name, String dept, int attendance, int leaves, 
                         double basic, double allowances, double deductions, String status) {
        this.id.set(id);
        this.name.set(name);
        this.basicSalary.set(basic);
        this.allowances.set(allowances);
        this.deductions.set(deductions);
        this.netSalary.set(basic + allowances - deductions);
        this.status.set(status);
    }

    // Getters for TableView PropertyValueFactory
    public String getEmployeeName() { return name.get(); }
    public String getPeriod() { return payPeriod.get(); }
    public double getNetSalary() { return netSalary.get(); }
    public String getStatus() { return status.get(); }

    // Property methods
    public StringProperty employeeNameProperty() { return name; }
    public StringProperty periodProperty() { return payPeriod; }
    public DoubleProperty netSalaryProperty() { return netSalary; }
    public StringProperty statusProperty() { return status; }
}