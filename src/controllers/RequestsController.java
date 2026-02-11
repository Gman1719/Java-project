package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import models.Request;
import utils.DBConnection;

import java.sql.*;
import java.time.LocalDate;

public class RequestsController {

    @FXML private TableView<Request> requestsTable;
    @FXML private TableColumn<Request, String> colEmployee;
    @FXML private TableColumn<Request, String> colType;
    @FXML private TableColumn<Request, LocalDate> colDate;
    @FXML private TableColumn<Request, String> colStatus;
    @FXML private TableColumn<Request, String> colRemarks;
    @FXML private TableColumn<Request, Void> colActions;

    @FXML private ComboBox<String> employeeFilter;
    @FXML private ComboBox<String> requestTypeFilter;

    private ObservableList<Request> requestList = FXCollections.observableArrayList();
    private ObservableList<Request> tableItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        loadRequests(); 
        loadFilters(); 
        setupTable();
    }

    private void setupTable() {
        // CellValueFactory setup is correct
        colEmployee.setCellValueFactory(data -> data.getValue().employeeNameProperty());
        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colDate.setCellValueFactory(data -> data.getValue().dateProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colRemarks.setCellValueFactory(data -> data.getValue().remarksProperty());

        // Actions column setup is correct
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Request, Void> call(TableColumn<Request, Void> param) {
                return new TableCell<>() {
                    private final Button btnApprove = new Button("Approve");
                    private final Button btnReject = new Button("Reject");
                    private final HBox pane = new HBox(5, btnApprove, btnReject);

                    {
                        btnApprove.getStyleClass().add("button-success"); // Assuming CSS classes exist
                        btnReject.getStyleClass().add("button-danger");

                        // Use a lambda or method reference for cleaner action setting
                        btnApprove.setOnAction(e -> approveRequest(getTableView().getItems().get(getIndex())));
                        btnReject.setOnAction(e -> rejectRequest(getTableView().getItems().get(getIndex())));
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Only show buttons if the request is Pending
                            Request currentRequest = getTableView().getItems().get(getIndex());
                            if ("Pending".equals(currentRequest.getStatus())) {
                                setGraphic(pane);
                            } else {
                                // Display status text instead of buttons if already actioned
                                setGraphic(new Label(currentRequest.getStatus())); 
                            }
                        }
                    }
                };
            }
        });

        // CRITICAL: Initialize the table items
        tableItems.addAll(requestList);
        requestsTable.setItems(tableItems);
    }

    private void loadFilters() {
        // Load employee names from database
        ObservableList<String> employees = FXCollections.observableArrayList("All");
        String empSql = "SELECT DISTINCT CONCAT(u.first_name, ' ', u.last_name) AS full_name FROM users u ORDER BY full_name";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(empSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employees.add(rs.getString("full_name"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading employee filter: " + e.getMessage());
        }
        
        employeeFilter.setItems(employees);
        employeeFilter.setValue("All");

        // Populate request types
        requestTypeFilter.getItems().addAll("All", "Leave", "Salary Advance", "Bank Account Change", "Reimbursement");
        requestTypeFilter.setValue("All");
    }

    private void loadRequests() {
        requestList.clear();
        
        try (Connection conn = DBConnection.getConnection()) {
            // Load Leave Requests
            String leaveSql = "SELECT lr.leave_id, CONCAT(u.first_name, ' ', u.last_name) AS employee_name, " +
                            "lr.leave_type, lr.requested_on, lr.status, lr.reason " +
                            "FROM leave_requests lr " +
                            "JOIN employees e ON lr.emp_id = e.emp_id " +
                            "JOIN users u ON e.user_id = u.user_id " +
                            "ORDER BY lr.requested_on DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(leaveSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requestList.add(new Request(
                        rs.getString("employee_name"),
                        "Leave - " + rs.getString("leave_type"),
                        rs.getTimestamp("requested_on").toLocalDateTime().toLocalDate(),
                        rs.getString("status"),
                        rs.getString("reason")
                    ));
                }
            }
            
            // Load Salary Advance Requests
            String advanceSql = "SELECT sar.advance_id, CONCAT(u.first_name, ' ', u.last_name) AS employee_name, " +
                              "sar.amount, sar.request_date, sar.status, sar.reason " +
                              "FROM salary_advance_requests sar " +
                              "JOIN employees e ON sar.emp_id = e.emp_id " +
                              "JOIN users u ON e.user_id = u.user_id " +
                              "ORDER BY sar.request_date DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(advanceSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requestList.add(new Request(
                        rs.getString("employee_name"),
                        "Salary Advance",
                        rs.getTimestamp("request_date").toLocalDateTime().toLocalDate(),
                        rs.getString("status"),
                        "Amount: $" + rs.getDouble("amount") + " - " + rs.getString("reason")
                    ));
                }
            }
            
            // Load Bank Account Change Requests
            String bankSql = "SELECT br.request_id, CONCAT(u.first_name, ' ', u.last_name) AS employee_name, " +
                           "br.old_account, br.new_account, br.request_date, br.status " +
                           "FROM bank_requests br " +
                           "JOIN employees e ON br.emp_id = e.emp_id " +
                           "JOIN users u ON e.user_id = u.user_id " +
                           "ORDER BY br.request_date DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(bankSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requestList.add(new Request(
                        rs.getString("employee_name"),
                        "Bank Account Change",
                        rs.getTimestamp("request_date").toLocalDateTime().toLocalDate(),
                        rs.getString("status"),
                        "From: " + rs.getString("old_account") + " To: " + rs.getString("new_account")
                    ));
                }
            }
            
            // Load Reimbursement Requests
            String reimbSql = "SELECT r.reimb_id, CONCAT(u.first_name, ' ', u.last_name) AS employee_name, " +
                            "r.amount, r.request_date, r.status, r.reason " +
                            "FROM reimbursements r " +
                            "JOIN employees e ON r.emp_id = e.emp_id " +
                            "JOIN users u ON e.user_id = u.user_id " +
                            "ORDER BY r.request_date DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(reimbSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requestList.add(new Request(
                        rs.getString("employee_name"),
                        "Reimbursement",
                        rs.getTimestamp("request_date").toLocalDateTime().toLocalDate(),
                        rs.getString("status"),
                        "Amount: $" + rs.getDouble("amount") + " - " + rs.getString("reason")
                    ));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading requests: " + e.getMessage());
            e.printStackTrace();
            showInfo("Database Error: " + e.getMessage());
        }
    }

    @FXML
    private void onFilter() {
        String selectedEmployee = employeeFilter.getValue();
        String selectedType = requestTypeFilter.getValue();
        
        // Use a stream filter for efficiency and clarity
        ObservableList<Request> filteredData = requestList.stream()
                .filter(request -> {
                    boolean matchesEmployee = "All".equals(selectedEmployee) || request.getEmployeeName().equals(selectedEmployee);
                    boolean matchesType = "All".equals(selectedType) || request.getType().equals(selectedType);
                    return matchesEmployee && matchesType;
                })
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);

        // Update the TableView's items
        requestsTable.setItems(filteredData);
        
        showInfo("Filter applied for Employee: " + selectedEmployee + ", Type: " + selectedType);
    }

    @FXML
    private void approveRequest(Request req) {
        updateRequestStatus(req, "Approved");
    }

    @FXML
    private void rejectRequest(Request req) {
        updateRequestStatus(req, "Rejected");
    }
    
    private void updateRequestStatus(Request req, String newStatus) {
        try (Connection conn = DBConnection.getConnection()) {
            String tableName = null;
            String idColumn = null;
            
            // Determine which table to update based on request type
            if (req.getType().startsWith("Leave")) {
                tableName = "leave_requests";
                idColumn = "leave_id";
            } else if (req.getType().equals("Salary Advance")) {
                tableName = "salary_advance_requests";
                idColumn = "advance_id";
            } else if (req.getType().equals("Bank Account Change")) {
                tableName = "bank_requests";
                idColumn = "request_id";
            } else if (req.getType().equals("Reimbursement")) {
                tableName = "reimbursements";
                idColumn = "reimb_id";
            }
            
            if (tableName != null) {
                String sql = "UPDATE " + tableName + " SET status = ? " +
                           "WHERE " + idColumn + " IN (" +
                           "  SELECT sub.id FROM (" +
                           "    SELECT " + idColumn + " AS id FROM " + tableName + " " +
                           "    WHERE emp_id = (SELECT e.emp_id FROM employees e " +
                           "                    JOIN users u ON e.user_id = u.user_id " +
                           "                    WHERE CONCAT(u.first_name, ' ', u.last_name) = ?) " +
                           "    ORDER BY request_date DESC LIMIT 1" +
                           "  ) AS sub" +
                           ")";
                
                // For leave_requests, use requested_on instead of request_date
                if (tableName.equals("leave_requests")) {
                    sql = sql.replace("request_date", "requested_on");
                }
                
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, req.getEmployeeName());
                    int updated = ps.executeUpdate();
                    
                    if (updated > 0) {
                        req.setStatus(newStatus);
                        requestsTable.refresh();
                        showInfo(req.getEmployeeName() + "'s request " + newStatus.toLowerCase() + ".");
                        loadRequests(); // Reload to get fresh data
                    } else {
                        showInfo("Failed to update request status.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating request: " + e.getMessage());
            e.printStackTrace();
            showInfo("Database Error: " + e.getMessage());
        }
    }

    @FXML
    private void onApproveAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Approve All Requests");
        confirm.setHeaderText("Are you sure you want to approve ALL pending requests?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateAllRequestsStatus("Approved");
            }
        });
    }

    @FXML
    private void onRejectAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject All Requests");
        confirm.setHeaderText("Are you sure you want to reject ALL pending requests?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateAllRequestsStatus("Rejected");
            }
        });
    }
    
    private void updateAllRequestsStatus(String newStatus) {
        try (Connection conn = DBConnection.getConnection()) {
            String[] queries = {
                "UPDATE leave_requests SET status = ? WHERE status = 'Pending'",
                "UPDATE salary_advance_requests SET status = ? WHERE status = 'Pending'",
                "UPDATE bank_requests SET status = ? WHERE status = 'Pending'",
                "UPDATE reimbursements SET status = ? WHERE status = 'Pending'"
            };
            
            int totalUpdated = 0;
            for (String query : queries) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, newStatus);
                    totalUpdated += ps.executeUpdate();
                }
            }
            
            if (totalUpdated > 0) {
                showInfo("All " + totalUpdated + " pending requests " + newStatus.toLowerCase() + ".");
                loadRequests();
            } else {
                showInfo("No pending requests found.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating all requests: " + e.getMessage());
            e.printStackTrace();
            showInfo("Database Error: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Management");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}