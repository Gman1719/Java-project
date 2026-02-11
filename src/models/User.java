package models;

public class User {
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String role; // "Admin", "HR", "Employee"

    public User(int id, String username, String firstName, String lastName, String role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
}
