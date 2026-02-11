package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import models.*;
import utils.DBConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

// iText 5 Imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class ReportsAnalyticsController implements Initializable {

    @FXML private Label lblReportTitle;
    @FXML private Label lblRecordCount;
    @FXML private StackPane reportContentArea;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblReportTitle.setText("Select a report to preview data");
    }

    // ================= 1. LEAVE REPORT FUNCTIONS =================

    @FXML
    private void loadLeaveReport() {
        TableView<LeaveRequest> table = buildLeaveTable();
        ObservableList<LeaveRequest> data = fetchLeaveData();
        table.setItems(data);
        updateUI("Detailed Leave Usage Report", table, data.size());
    }

    @FXML
    private void downloadLeavePDF() {
        exportToPDF("Leave_Usage_Report", new String[]{"ID", "Type", "Start", "End", "Status"}, 
                   fetchLeaveData(), "leave");
    }

    private TableView<LeaveRequest> buildLeaveTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getColumns().add(this.<LeaveRequest, Integer>createGenericCol("ID", "requestId", 60));
        table.getColumns().add(this.<LeaveRequest, String>createGenericCol("Type", "leaveType", 120));
        table.getColumns().add(this.<LeaveRequest, LocalDate>createGenericCol("Start", "startDate", 110));
        table.getColumns().add(this.<LeaveRequest, LocalDate>createGenericCol("End", "endDate", 110));
        table.getColumns().add(this.<LeaveRequest, String>createGenericCol("Status", "status", 100));
        return table;
    }

    // ================= 2. ATTENDANCE REPORT FUNCTIONS =================

    @FXML
    private void loadAttendanceReport() {
        TableView<AttendanceRecord> table = buildAttendanceTable();
        ObservableList<AttendanceRecord> data = fetchAttendanceData();
        table.setItems(data);
        updateUI("Staff Daily Attendance", table, data.size());
    }

    @FXML
    private void downloadAttendancePDF() {
        exportToPDF("Attendance_Report", new String[]{"Employee", "Date", "Status", "Type", "Remarks"}, 
                   fetchAttendanceData(), "attendance");
    }

    private TableView<AttendanceRecord> buildAttendanceTable() {
        TableView<AttendanceRecord> table = new TableView<>();
        table.getColumns().add(this.<AttendanceRecord, String>createGenericCol("Employee", "employeeName", 150));
        table.getColumns().add(this.<AttendanceRecord, LocalDate>createGenericCol("Date", "date", 110));
        table.getColumns().add(this.<AttendanceRecord, String>createGenericCol("Status", "status", 100));
        table.getColumns().add(this.<AttendanceRecord, String>createGenericCol("Type", "type", 100));
        table.getColumns().add(this.<AttendanceRecord, String>createGenericCol("Remarks", "remarks", 200));
        return table;
    }

    // ================= DATA FETCHING (FIXED SQL) =================

    private ObservableList<AttendanceRecord> fetchAttendanceData() {
        ObservableList<AttendanceRecord> list = FXCollections.observableArrayList();
        
        // SQL uses 'attendance_date' as renamed in your script:
        // ALTER TABLE attendance CHANGE COLUMN date attendance_date DATE NOT NULL;
        String sql = "SELECT u.first_name, u.last_name, a.attendance_date, a.status, a.attendance_type, a.remarks " +
                     "FROM attendance a " +
                     "JOIN employees e ON a.emp_id = e.emp_id " +
                     "JOIN users u ON e.user_id = u.user_id";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                
                // Fetching the renamed date column
                Date sqlDate = rs.getDate("attendance_date");
                LocalDate displayDate = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now();

                list.add(new AttendanceRecord(
                    fullName,
                    displayDate, 
                    rs.getString("status") != null ? rs.getString("status") : "N/A",
                    // Using attendance_type added via: ALTER TABLE attendance ADD COLUMN attendance_type...
                    rs.getString("attendance_type") != null ? rs.getString("attendance_type") : "Regular",
                    rs.getString("remarks") != null ? rs.getString("remarks") : ""
                ));
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
            showAlert("Database Error", "Attendance retrieval failed: " + e.getMessage());
        }
        return list;
    }

    private ObservableList<LeaveRequest> fetchLeaveData() {
        ObservableList<LeaveRequest> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM leave_requests";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new LeaveRequest(
                    rs.getInt("leave_id"),
                    rs.getInt("emp_id"),
                    rs.getString("leave_type"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate(),
                    rs.getDouble("total_days"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getTimestamp("requested_on").toLocalDateTime()
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    private ObservableList<Request> fetchRequestData() {
        ObservableList<Request> list = FXCollections.observableArrayList();
        String sql = "SELECT u.first_name, u.last_name, 'Bank Update' as type, b.request_date, b.status " +
                     "FROM bank_requests b " +
                     "JOIN employees e ON b.emp_id = e.emp_id " +
                     "JOIN users u ON e.user_id = u.user_id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Request(
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("type"),
                    rs.getTimestamp("request_date").toLocalDateTime().toLocalDate(),
                    rs.getString("status"),
                    "Account Change Request"
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    
    

    // ================= 3. REQUESTS REPORT FUNCTIONS =================

    @FXML
    private void loadRequestReport() {
        TableView<Request> table = buildRequestTable();
        ObservableList<Request> data = fetchRequestData();
        table.setItems(data);
        updateUI("Bank & System Requests Audit", table, data.size());
    }

    @FXML
    private void downloadRequestPDF() {
        exportToPDF("Requests_Report", new String[]{"Employee", "Type", "Date", "Status"}, 
                   fetchRequestData(), "request");
    }

    @FXML
    private void loadEmployeeReport() { 
        loadRequestReport(); 
        lblReportTitle.setText("Staff Directory List"); 
    }

    @FXML
    private void downloadEmployeePDF() { downloadRequestPDF(); }

    private TableView<Request> buildRequestTable() {
        TableView<Request> table = new TableView<>();
        table.getColumns().add(this.<Request, String>createGenericCol("Employee", "employeeName", 180));
        table.getColumns().add(this.<Request, String>createGenericCol("Type", "type", 140));
        table.getColumns().add(this.<Request, LocalDate>createGenericCol("Date", "date", 100));
        table.getColumns().add(this.<Request, String>createGenericCol("Status", "status", 100));
        return table;
    }

    // ================= PDF EXPORT & HELPERS =================

    private void exportToPDF(String fileName, String[] headers, ObservableList<?> data, String type) {
        if (data.isEmpty()) {
            showAlert("Warning", "No data found to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName + "_" + LocalDate.now() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(lblReportTitle.getScene().getWindow());

        if (file != null) {
            try {
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();
                doc.add(new Paragraph("PAYROLL SYSTEM - OFFICIAL REPORT", new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD)));
                doc.add(new Paragraph("Generated: " + LocalDate.now()));
                doc.add(new Paragraph(" ")); 

                PdfPTable pdfTable = new PdfPTable(headers.length);
                pdfTable.setWidthPercentage(100);

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE)));
                    cell.setBackgroundColor(new BaseColor(16, 185, 129));
                    pdfTable.addCell(cell);
                }

                for (Object obj : data) {
                    if (type.equals("attendance")) {
                        AttendanceRecord ar = (AttendanceRecord) obj;
                        pdfTable.addCell(ar.getEmployeeName());
                        pdfTable.addCell(ar.getDate().toString());
                        pdfTable.addCell(ar.getStatus());
                        pdfTable.addCell(ar.getType());
                        pdfTable.addCell(ar.getRemarks());
                    } else if (type.equals("leave")) {
                        LeaveRequest lr = (LeaveRequest) obj;
                        pdfTable.addCell(String.valueOf(lr.getRequestId()));
                        pdfTable.addCell(lr.getLeaveType());
                        pdfTable.addCell(lr.getStartDate().toString());
                        pdfTable.addCell(lr.getEndDate().toString());
                        pdfTable.addCell(lr.getStatus());
                    } else {
                        Request r = (Request) obj;
                        pdfTable.addCell(r.getEmployeeName());
                        pdfTable.addCell(r.getType());
                        pdfTable.addCell(r.getDate().toString());
                        pdfTable.addCell(r.getStatus());
                    }
                }
                doc.add(pdfTable);
                doc.close();
                showAlert("Success", "Exported successfully.");
            } catch (Exception e) {
                showAlert("Error", "PDF Export Failed: " + e.getMessage());
            }
        }
    }

    private void updateUI(String title, TableView<?> table, int count) {
        lblReportTitle.setText(title);
        if(lblRecordCount != null) lblRecordCount.setText("Records Found: " + count);
        reportContentArea.getChildren().setAll(table);
    }

    private <T, S> TableColumn<T, S> createGenericCol(String title, String property, double width) {
        TableColumn<T, S> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}