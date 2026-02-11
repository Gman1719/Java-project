package models;

import javafx.beans.property.*;
import java.time.LocalDate;

public class PaycheckPreview {
    
    // Properties for TableView binding
    private final IntegerProperty employeeId = new SimpleIntegerProperty();
    private final StringProperty employeeName = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> payPeriodStart = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> payPeriodEnd = new SimpleObjectProperty<>();
    private final DoubleProperty hoursWorked = new SimpleDoubleProperty();
    private final DoubleProperty grossPay = new SimpleDoubleProperty();
    private final DoubleProperty totalTaxes = new SimpleDoubleProperty();      // Mapped to colTax
    private final DoubleProperty totalDeductions = new SimpleDoubleProperty(); // Mapped to colDeductions
    private final DoubleProperty netPay = new SimpleDoubleProperty();

    public PaycheckPreview() {}

    /**
     * Constructor: Matches the 8 arguments used in HRPayrollController.loadPayrollData().
     * (id, name, start, end, hours, gross, taxes, net)
     */
    public PaycheckPreview(int id, String name, LocalDate start, LocalDate end, double hours, double gross, double taxes, double net) {
        setEmployeeId(id);
        setEmployeeName(name);
        setPayPeriodStart(start);
        setPayPeriodEnd(end);
        setHoursWorked(hours);
        setGrossPay(gross);
        setTotalTaxes(taxes);
        setNetPay(net);
        
        // Calculate the value for totalDeductions based on the other values,
        // since the FXML expects separate tax and deduction columns.
        // Assuming TotalDeductions = Gross - Taxes - Net
        double calculatedDeductions = gross - taxes - net; 
        setTotalDeductions(calculatedDeductions); 
    }
    
    // --- Getters and Setters (CRITICAL for TableView PropertyValueFactory) ---
    
    public int getEmployeeId() { return employeeId.get(); }
    public IntegerProperty employeeIdProperty() { return employeeId; }
    public void setEmployeeId(int id) { this.employeeId.set(id); }

    public String getEmployeeName() { return employeeName.get(); }
    public StringProperty employeeNameProperty() { return employeeName; }
    public void setEmployeeName(String name) { this.employeeName.set(name); }
    
    public LocalDate getPayPeriodStart() { return payPeriodStart.get(); }
    public ObjectProperty<LocalDate> payPeriodStartProperty() { return payPeriodStart; }
    public void setPayPeriodStart(LocalDate date) { this.payPeriodStart.set(date); }

    public LocalDate getPayPeriodEnd() { return payPeriodEnd.get(); }
    public ObjectProperty<LocalDate> payPeriodEndProperty() { return payPeriodEnd; }
    public void setPayPeriodEnd(LocalDate date) { this.payPeriodEnd.set(date); }
    
    public double getHoursWorked() { return hoursWorked.get(); }
    public DoubleProperty hoursWorkedProperty() { return hoursWorked; }
    public void setHoursWorked(double hours) { this.hoursWorked.set(hours); }
    
    public double getGrossPay() { return grossPay.get(); }
    public DoubleProperty grossPayProperty() { return grossPay; }
    public void setGrossPay(double pay) { this.grossPay.set(pay); }

    public double getTotalTaxes() { return totalTaxes.get(); }
    public DoubleProperty totalTaxesProperty() { return totalTaxes; }
    public void setTotalTaxes(double taxes) { this.totalTaxes.set(taxes); }
    
    public double getTotalDeductions() { return totalDeductions.get(); }
    public DoubleProperty totalDeductionsProperty() { return totalDeductions; }
    public void setTotalDeductions(double deductions) { this.totalDeductions.set(deductions); }
    
    public double getNetPay() { return netPay.get(); }
    public DoubleProperty netPayProperty() { return netPay; }
    public void setNetPay(double net) { this.netPay.set(net); }
}