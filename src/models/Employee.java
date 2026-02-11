package models;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * Full implementation of Employee model.
 * Synchronized with SQL Schema: payroll_system
 */
public class Employee {
    // Properties for UI Binding
    private final IntegerProperty employeeId = new SimpleIntegerProperty();  
    private final IntegerProperty userId = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty department = new SimpleStringProperty();
    private final StringProperty position = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dateJoined = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty roleName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final IntegerProperty roleId = new SimpleIntegerProperty();
    private final DoubleProperty salary = new SimpleDoubleProperty(); 
    private final StringProperty gender = new SimpleStringProperty();
    private final StringProperty bankAccount = new SimpleStringProperty();
    private final StringProperty employmentType = new SimpleStringProperty(); 
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty profilePicturePath = new SimpleStringProperty();
    
    // Legacy ID field from previous version
    private int id;

    public Employee() {}

    // Constructor 5: CRITICAL CONSTRUCTOR FOR EmployeeService (Main Fetch)
    public Employee(int employeeId, int userId, String username, String firstName, String lastName, 
                    String phone, String email, String department, String position, String status, LocalDate dateJoined, 
                    String gender, String bankAccount) {
        setEmployeeId(employeeId);
        setUserId(userId);
        setUsername(username);
        setFirstName(firstName);
        setLastName(lastName);
        setPhone(phone);
        setEmail(email);
        setDepartment(department);
        setPosition(position);
        setStatus(status);
        setDateJoined(dateJoined);
        setGender(gender);
        setBankAccount(bankAccount);
        // Business Logic: Assign employment type based on position
        setEmploymentType((position != null && position.toLowerCase().contains("manager")) ? "Full-Time" : "Contract");
        setSalary(0.0);
        setRoleId(0); 
    }
    
    // Constructor 1: Basic Admin View
    public Employee(int employeeId, int userId, String username, String firstName,
                    String lastName, String email, String phone, String roleName) {
        setEmployeeId(employeeId);
        setUserId(userId);
        setUsername(username);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setPhone(phone);
        setRoleName(roleName);
    }
    
    // Constructor 2: Role Management View
    public Employee(int employeeId, int userId, String username, String firstName,
                    String lastName, String email, String phone, String roleName, int roleId) {
        this(employeeId, userId, username, firstName, lastName, email, phone, roleName);
        setRoleId(roleId);
    }

    // Constructor 3: Payroll Calculation Basic
    public Employee(int employeeId, int userId, String username, String firstName,
                    String lastName, double salary) {
        setEmployeeId(employeeId);
        setUserId(userId);
        setUsername(username);
        setFirstName(firstName);
        setLastName(lastName);
        setSalary(salary);
    }
    
    // Constructor 4: Extended Payroll View
    public Employee(int employeeId, int userId, String username, String firstName,
                    String lastName, String department, String position, double salary) {
        this(employeeId, userId, username, firstName, lastName, salary);
        setDepartment(department);
        setPosition(position);
    }

    // --- HELPER METHODS FOR UI COMPATIBILITY ---
    
    @Override
    public String toString() {
        return getFirstName() + " " + getLastName();
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public double getBaseSalary() { return getSalary(); }

    // --- PROPERTY GETTERS (For TableView Binding) ---
    public IntegerProperty employeeIdProperty() { return employeeId; }
    public IntegerProperty userIdProperty() { return userId; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty firstNameProperty() { return firstName; }
    public StringProperty lastNameProperty() { return lastName; }
    public StringProperty departmentProperty() { return department; }
    public StringProperty positionProperty() { return position; }
    public ObjectProperty<LocalDate> dateJoinedProperty() { return dateJoined; }
    public StringProperty statusProperty() { return status; }
    public StringProperty roleNameProperty() { return roleName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public IntegerProperty roleIdProperty() { return roleId; }
    public DoubleProperty salaryProperty() { return salary; }
    public StringProperty genderProperty() { return gender; }
    public StringProperty bankAccountProperty() { return bankAccount; }
    public StringProperty employmentTypeProperty() { return employmentType; } 
    public StringProperty addressProperty() { return address; }
    public StringProperty profilePicturePathProperty() { return profilePicturePath; }
    public StringProperty fullNameProperty() { return new SimpleStringProperty(getFullName()); }

    // --- STANDARD GETTERS AND SETTERS ---
    public int getEmployeeId() { return employeeId.get(); }
    public void setEmployeeId(int id) { this.employeeId.set(id); }

    public int getUserId() { return userId.get(); }
    public void setUserId(int id) { this.userId.set(id); }

    public String getUsername() { return username.get(); }
    public void setUsername(String u) { username.set(u != null ? u : ""); }

    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String f) { firstName.set(f != null ? f : ""); }

    public String getLastName() { return lastName.get(); }
    public void setLastName(String l) { lastName.set(l != null ? l : ""); }

    public String getDepartment() { return department.get(); }
    public void setDepartment(String d) { department.set(d != null ? d : ""); }

    public String getPosition() { return position.get(); }
    public void setPosition(String p) { position.set(p != null ? p : ""); }

    public LocalDate getDateJoined() { return dateJoined.get(); }
    public void setDateJoined(LocalDate d) { dateJoined.set(d); }

    public String getStatus() { return status.get(); }
    public void setStatus(String s) { status.set(s != null ? s : "Unknown"); }

    public String getRoleName() { return roleName.get(); }
    public void setRoleName(String r) { roleName.set(r != null ? r : ""); }

    public String getEmail() { return email.get(); }
    public void setEmail(String e) { email.set(e != null ? e : ""); }

    public String getPhone() { return phone.get(); }
    public void setPhone(String p) { phone.set(p != null ? p : ""); }

    public int getRoleId() { return roleId.get(); }
    public void setRoleId(int r) { roleId.set(r); }

    public double getSalary() { return salary.get(); }
    public void setSalary(double s) { this.salary.set(s); }

    public String getGender() { return gender.get(); }
    public void setGender(String g) { this.gender.set(g != null ? g : ""); }

    public String getBankAccount() { return bankAccount.get(); }
    public void setBankAccount(String b) { this.bankAccount.set(b != null ? b : ""); }

    public String getEmploymentType() { return employmentType.get(); }
    public void setEmploymentType(String e) { this.employmentType.set(e != null ? e : ""); }

    public String getAddress() { return address.get(); }
    public void setAddress(String a) { this.address.set(a != null ? a : ""); }

    public String getProfilePicturePath() { return profilePicturePath.get(); }
    public void setProfilePicturePath(String p) { this.profilePicturePath.set(p); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}