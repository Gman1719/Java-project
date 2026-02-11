package models;

public class BankDetails {
    private int bankId;
    private int employeeId;
    private String bankName;
    private String accountNumber;
    private String ifscCode;

    public BankDetails() {}

    public BankDetails(int bankId, int employeeId, String bankName, String accountNumber, String ifscCode) {
        this.bankId = bankId;
        this.employeeId = employeeId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
    }

    // Getters & Setters
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
}
