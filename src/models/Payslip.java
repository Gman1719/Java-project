package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model class representing a record from the 'payroll' table.
 * Synchronized with the MySQL schema: DECIMAL -> BigDecimal, TIMESTAMP -> Timestamp.
 */
public class Payslip {

    private int payslipId;
    private int payrollId;
    private int empId;
    private String employeeName; // Added to store name from Joined 'users' table query
    private String month;
    private int year;
    private BigDecimal baseSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal tax;
    private BigDecimal netSalary;
    private Timestamp generatedOn;

    // Default constructor
    public Payslip() {}

    // Full Parameterized constructor
    public Payslip(int payslipId, int payrollId, int empId, String employeeName, String month, int year, 
                   BigDecimal baseSalary, BigDecimal allowances, BigDecimal deductions, 
                   BigDecimal tax, BigDecimal netSalary, Timestamp generatedOn) {
        this.payslipId = payslipId;
        this.payrollId = payrollId;
        this.empId = empId;
        this.employeeName = employeeName;
        this.month = month;
        this.year = year;
        this.baseSalary = baseSalary;
        this.allowances = allowances;
        this.deductions = deductions;
        this.tax = tax;
        this.netSalary = netSalary;
        this.generatedOn = generatedOn;
    }

    // --- Getters and Setters ---

    public int getPayslipId() { return payslipId; }
    public void setPayslipId(int payslipId) { this.payslipId = payslipId; }

    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }

    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public Timestamp getGeneratedOn() { return generatedOn; }
    public void setGeneratedOn(Timestamp generatedOn) { this.generatedOn = generatedOn; }

    /**
     * Helper to get total earnings before deductions/tax
     */
    public BigDecimal getTotalEarnings() {
        return baseSalary.add(allowances);
    }

    /**
     * Helper to get total deductions (Tax + Deductions)
     */
    public BigDecimal getTotalDeductions() {
        return deductions.add(tax);
    }

    @Override
    public String toString() {
        return "Payslip{" +
                "payrollId=" + payrollId +
                ", empId=" + empId +
                ", name='" + employeeName + '\'' +
                ", period='" + month + " " + year + '\'' +
                ", netSalary=" + netSalary +
                ", generatedOn=" + generatedOn +
                '}';
    }
}