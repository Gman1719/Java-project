package controllers;

import dao.EmployeeDAO;
import models.Employee;
import models.AttendanceRecord;
import models.LeaveRequest;
import utils.DBConnection; 
import utils.SessionManager; 
import utils.ProfileUpdateListener; 
import models.Payslip;
import utils.PDFGenerator;
import controllers.PayslipsHistoryController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType; 
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ScrollPane; // Explicitly import ScrollPane
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.beans.property.SimpleStringProperty; 
import javafx.stage.Stage;
import javafx.stage.Modality; 
import javafx.stage.FileChooser; // Import FileChooser

import java.io.File; // Import File
import java.io.FileOutputStream; // Import FileOutputStream
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional; // Import Optional
import java.util.ResourceBundle;

// iText imports (CRITICAL: These must be present for PDF generation)
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


public class EmployeeDashboardController implements Initializable, ProfileUpdateListener { 

    // --- FXML INJECTIONS ---
	@FXML private ImageView imgProfile;
	@FXML private Label lblName;
	@FXML private Label lblRole;
	@FXML private Label lblNetSalary;
	@FXML private Label lblAvailableLeaves;
	@FXML private Label lblPayDay;
	@FXML private StackPane chartContainer;
	@FXML private DatePicker dpAttendanceMonth;
	@FXML private TabPane historyTabPane;
	@FXML private BorderPane mainBorderPane;
	@FXML private ScrollPane centerScrollPane;
	@FXML private VBox mainContent;
	@FXML private ListView<String> listNotifications;

	@FXML private TableView<AttendanceRecord> tblAttendance;
	@FXML private TableView<LeaveRequest> tblLeaveRequests;
	@FXML private TableView<String> tblOtherRequests;


    // --- DEPENDENCY & DATA ---
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    
    // ------------------ INITIALIZATION AND REFRESH ------------------
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeAttendanceTable();
        initializeLeaveTable();
        initializeOtherRequestsTable();
        dpAttendanceMonth.setValue(LocalDate.now().withDayOfMonth(1)); 
        
        dpAttendanceMonth.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                loadAttendanceData();
            }
        });

        loadAllData();
    }
    
    @Override
    public void onProfileUpdated() {
        System.out.println("Profile updated successfully. Reloading dashboard profile data.");
        loadProfile(); 
        loadSalaryAndLeaveMetrics(); 
    }
    
    private void loadAllData() {
        loadProfile();
        loadSalaryAndLeaveMetrics();
     
        loadAttendanceData(); 
        loadLeavesData();
        loadRequestsData();
      
    }
    
    // ------------------ SCENE/WINDOW MANAGEMENT (ROBUST LOADING) ------------------
    
    /**
     * Helper method to load a new FXML scene and replace the current *window* content (for logout/full nav).
     */
    private void loadNewScene(String fxmlPath, String title) {
        try {
            Stage currentStage = (Stage) mainBorderPane.getScene().getWindow(); 
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Resource not found: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(resource);
            Scene scene = new Scene(root);
            currentStage.setTitle(title);
            currentStage.setScene(scene);
            currentStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
            new Alert(AlertType.ERROR, "Could not load the requested page. FXML file missing or invalid: " + fxmlPath).showAndWait();
        }
    }
    
    /**
     * Helper method to load an FXML as a modal dialog/pop-up window.
     */
    private FXMLLoader loadModalDialog(String fxmlPath, String title) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Resource not found: " + fxmlPath);
        }
        
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(mainBorderPane.getScene().getWindow());
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show(); // Note: This should be showAndWait() if you need to block the parent window
        
        return loader;
    }
    
    
    /**
     * Loads an FXML and replaces the content of the main dashboard center area.
     */
    private void loadCenterContent(String fxmlPath, String title) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Resource not found: " + fxmlPath);
            }
            
            Parent newContent = FXMLLoader.load(resource);
            
            // Check if the ScrollPane is injected (best case)
            if (centerScrollPane != null) {
                centerScrollPane.setContent(newContent);
            } else {
                // Fallback: Check the BorderPane center
                ScrollPane scrollPane = (ScrollPane) mainBorderPane.getCenter();
                if (scrollPane != null) {
                    scrollPane.setContent(newContent);
                } else {
                    mainBorderPane.setCenter(newContent);
                }
            }
            mainBorderPane.getScene().getWindow().setTitle("Employee Dashboard - " + title);
            
        } catch (IOException e) {
            System.err.println("Failed to load center content: " + fxmlPath);
            e.printStackTrace();
            new Alert(AlertType.ERROR, "Could not load the requested page. FXML file missing or invalid: " + fxmlPath).showAndWait();
        }
    }


    // ------------------ NAVIGATION HANDLERS (UPDATED LOGIC) ------------------
    
    @FXML public void showHome() { 
        System.out.println("Navigating to Dashboard Home. Refreshing all data..."); 
        loadAllData();
    }
    
    @FXML private void openProfile() { 
        // Logic remains the same (assumes EditProfileController exists)
        try {
            FXMLLoader loader = loadModalDialog("/views/EditProfile.fxml", "Edit My Profile"); 
            Object controller = loader.getController();
            
            int currentUserId = SessionManager.getCurrentEmployeeId(); 
            if (currentUserId <= 0) {
                 new Alert(AlertType.ERROR, "Data Error: Invalid User ID in session.").showAndWait();
                 return; 
            }

            if (controller instanceof EditProfileController) {
                EditProfileController editController = (EditProfileController) controller;
                editController.setUserId(currentUserId);
                editController.setProfileUpdateListener(this);
            } 
        } catch (Exception e) { 
            System.err.println("Error opening profile window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * FIX: Correctly implements the modal opening and passes the employee ID
     */

    
    @FXML private void openAttendance() { 
        if (historyTabPane != null && historyTabPane.getTabs().size() > 0) {
            historyTabPane.getSelectionModel().select(0); 
            
            ScrollPane scrollPane = (ScrollPane) mainBorderPane.getCenter();
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0); 
            }
        }
    }
    
    @FXML private void openRequests() { 
        historyTabPane.getSelectionModel().select(2); // Select "Other" tab (index 2)
        
        ScrollPane scrollPane = (ScrollPane) mainBorderPane.getCenter();
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
    }
    
    @FXML private void openPayslips() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PayslipsHistory.fxml"));
            Parent root = loader.load();
            
            PayslipsHistoryController controller = loader.getController();
            int currentEmployeeId = SessionManager.getCurrentEmployeeId();
            
            if (currentEmployeeId > 0) {
                controller.setEmployeeId(currentEmployeeId);
            } else {
                showAlert(AlertType.ERROR, "Session Error", "Employee session data is missing.");
                return;
            }
            
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            Stage modalStage = new Stage();
            modalStage.setTitle("Payslip History");
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(parentStage);
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load Payslips History: " + e.getMessage());
        }
    }
    


    @FXML private void changePassword() { 
        try {
            FXMLLoader loader = loadModalDialog("/views/ChangePassword.fxml", "Change Password"); 
            
            Object controller = loader.getController();
            int currentUserId = SessionManager.getCurrentEmployeeId(); 
            
            if (currentUserId <= 0) {
                 new Alert(AlertType.ERROR, "Session Error: Invalid User ID in session.").showAndWait();
                 return; 
            }
            
            if (controller instanceof ChangePasswordController) {
                ChangePasswordController changePassController = (ChangePasswordController) controller;
                changePassController.setUserId(currentUserId);
            } 
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(AlertType.ERROR, "Failed to load Change Password Form.").showAndWait();
        }
    }
    
    @FXML private void openNotifications() { 
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Notifications.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the user ID
            NotificationsController controller = loader.getController();
            
            // Create and show the modal stage
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            Stage modalStage = new Stage();
            modalStage.setTitle("Notifications");
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(parentStage);
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load Notifications: " + e.getMessage());
        }
    }
    
    @FXML public void handleLogout() { 
        SessionManager.clearSession();
        loadNewScene("/views/login.fxml", "Employee Login"); 
    }
    
    @FXML private void openLeaveRequest() { 
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LeaveRequestForm.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the employee ID
            LeaveRequestFormController controller = loader.getController();
            int currentEmployeeId = SessionManager.getCurrentEmployeeId();
            
            if (currentEmployeeId > 0) {
                controller.setEmployeeId(currentEmployeeId);
            } else {
                showAlert(AlertType.ERROR, "Session Error", "Employee session data is missing.");
                return;
            }
            
            // Create and show the modal stage
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            Stage modalStage = new Stage();
            modalStage.setTitle("Submit Leave Request");
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(parentStage);
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            
            // Refresh leave requests table after closing the form
            loadLeavesData();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load Leave Request Form: " + e.getMessage());
        }
    }

    @FXML private void openCalendar() { 
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CompanyCalendar.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the employee ID if needed
            CompanyCalendarController controller = loader.getController();
            int currentEmployeeId = SessionManager.getCurrentEmployeeId();
            
            if (currentEmployeeId > 0) {
                controller.setEmployeeId(currentEmployeeId);
            }
            
            // Create and show the modal stage
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            Stage modalStage = new Stage();
            modalStage.setTitle("Company Calendar");
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(parentStage);
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load Company Calendar: " + e.getMessage());
        }
    }
    
    @FXML private void openNewRequest() {
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewRequestForm.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the employee ID
            NewRequestFormController controller = loader.getController();
            int currentEmployeeId = SessionManager.getCurrentEmployeeId();
            
            if (currentEmployeeId > 0) {
                controller.setEmployeeId(currentEmployeeId);
            } else {
                showAlert(AlertType.ERROR, "Session Error", "Employee session data is missing.");
                return;
            }
            
            // Create and show the modal stage
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            Stage modalStage = new Stage();
            modalStage.setTitle("Submit New Request");
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(parentStage);
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            
            // Refresh requests table after closing the form
            loadRequestsData();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load New Request Form: " + e.getMessage());
        }
    }
    
    // ------------------ DASHBOARD CARD ACTIONS (FIXED: Download Payslip) ------------------

    /**
     * FIX: Implements the full logic to find the latest payslip, prompt for file saving, and execute the PDF generation.
     */
    @FXML 
    private void downloadPayslip() { 
        try {
            // 1. Identify the current user from the session
            int currentEmployeeId = SessionManager.getCurrentEmployeeId();
            if (currentEmployeeId <= 0) {
                showAlert(AlertType.ERROR, "Session Error", "No active employee session found.");
                return;
            }

            // 2. Fetch the latest payslip details for the current employee
            // This calls the helper method that queries the 'payroll' table
            Optional<Payslip> payslipOptional = getLatestPayslipDetails(currentEmployeeId);

            if (!payslipOptional.isPresent()) {
                showAlert(AlertType.INFORMATION, "Download Info", "No recent payslip found in the database.");
                return;
            }
            
            Payslip payslip = payslipOptional.get();

            // 3. Configure the FileChooser to select save location
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Latest Payslip PDF");
            
            // Construct a safe filename: Payslip_John_Doe_December_2025.pdf
            String safeEmployeeName = payslip.getEmployeeName().replaceAll("[^a-zA-Z0-9]", "_");
            String filename = String.format("Payslip_%s_%s_%d.pdf", 
                                        safeEmployeeName, 
                                        payslip.getMonth(), 
                                        payslip.getYear()); // Uses 'int year' from your model
            
            fileChooser.setInitialFileName(filename);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            // Show the dialog
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                // 4. Execute the PDF generation (iText logic)
                boolean success = generatePayslipPdf(payslip, file);
                
                if (success) {
                    showAlert(AlertType.INFORMATION, "Success", 
                              "Your payslip for " + payslip.getMonth() + " has been saved to:\n" + file.getAbsolutePath());
                } else {
                    showAlert(AlertType.ERROR, "Generation Error", "The file was created but the PDF content failed to generate.");
                }
            }

        } catch (Exception e) {
            // Catch-all for unexpected UI or I/O issues
            System.err.println("Critical error in downloadPayslip: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "System Error", "An unexpected error occurred: " + e.getMessage());
        }
    }
    
    // ------------------ Payslip Download Helper Methods (INTEGRATED) ------------------
    
    private Optional<Payslip> getLatestPayslipDetails(int empId) {
        String sql = "SELECT * FROM payroll WHERE emp_id = ? ORDER BY generated_on DESC LIMIT 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                // Use the Full Parameterized constructor from your Payslip model
                // Order: payslipId, payrollId, empId, name, month, year, base, allow, deduct, tax, net, timestamp
                return Optional.of(new Payslip(
                    rs.getInt("payroll_id"),       // payslipId (mapping to your payroll_id)
                    rs.getInt("payroll_id"),       // payrollId
                    empId,                         // empId
                    getEmployeeName(empId),        // employeeName
                    rs.getString("month"),         // month
                    rs.getInt("year"),             // year (Ensure this column exists in DB)
                    rs.getBigDecimal("base_salary"), 
                    rs.getBigDecimal("allowances"),
                    rs.getBigDecimal("deductions"),
                    rs.getBigDecimal("tax"),
                    rs.getBigDecimal("net_salary"),
                    rs.getTimestamp("generated_on")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error fetching latest payslip details: " + e.getMessage()); 
        }
        return Optional.empty();
    }
    
    private String getEmployeeName(int empId) {
        String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name"); 
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL SQL Error fetching employee name: " + e.getMessage());
        }
        return "Unknown Employee";
    }

    private boolean generatePayslipPdf(Payslip payslip, File file) {
        Document document = new Document();
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, fos);
            document.open();
            
            // --- 1. Define Fonts and Styles ---
            Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Font netPayFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(3, 106, 3)); 

            // --- 2. Company Header ---
            Paragraph companyName = new Paragraph("COMPANY PAYROLL SERVICES", companyFont);
            companyName.setAlignment(Element.ALIGN_CENTER);
            companyName.setSpacingAfter(5f); 
            document.add(companyName);

            // Accessing month and year directly from the model
            String period = payslip.getMonth().toUpperCase() + " " + payslip.getYear();
            Paragraph payslipTitle = new Paragraph("OFFICIAL PAYSLIP - " + period, titleFont);
            payslipTitle.setAlignment(Element.ALIGN_CENTER);
            payslipTitle.setSpacingAfter(15f); 
            document.add(payslipTitle);
            
            // --- 3. Employee and Period Details Table ---
            PdfPTable infoTable = new PdfPTable(4); 
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(15f);
            infoTable.setWidths(new float[]{1f, 1.5f, 1.2f, 1.3f});

            infoTable.addCell(createBoldLabelCell("Employee:"));
            infoTable.addCell(createDataCell(payslip.getEmployeeName(), normalFont));
            infoTable.addCell(createBoldLabelCell("Generated On:"));
            // Formatting the Timestamp to a simple string
            String dateStr = payslip.getGeneratedOn().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            infoTable.addCell(createDataCell(dateStr, normalFont, Element.ALIGN_RIGHT));
            
            infoTable.addCell(createBoldLabelCell("Emp ID:"));
            infoTable.addCell(createDataCell(String.valueOf(payslip.getEmpId()), normalFont));
            infoTable.addCell(createBoldLabelCell("Payslip Reference:"));
            infoTable.addCell(createDataCell("#" + payslip.getPayslipId(), normalFont, Element.ALIGN_RIGHT));

            document.add(infoTable);
            
            // --- 4. Financial Summary Table ---
            document.add(new Paragraph("Financial Summary", titleFont));
            
            PdfPTable detailTable = new PdfPTable(2); 
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingBefore(5f);
            detailTable.setSpacingAfter(20f);

            BaseColor earningColor = new BaseColor(210, 230, 255);
            BaseColor deductionColor = new BaseColor(255, 230, 210);

            detailTable.addCell(createHeaderCell("EARNINGS", headerFont, earningColor));
            detailTable.addCell(createHeaderCell("DEDUCTIONS", headerFont, deductionColor));

            // Nested EARNINGS table
            PdfPTable earnings = new PdfPTable(2);
            earnings.setWidths(new float[]{3f, 1.5f});
            earnings.addCell(createDataCell("Gross Base Salary:", normalFont));
            earnings.addCell(createDataCell(formatCurrency(payslip.getBaseSalary()), normalFont, Element.ALIGN_RIGHT));
            earnings.addCell(createDataCell("Allowances:", normalFont));
            earnings.addCell(createDataCell(formatCurrency(payslip.getAllowances()), normalFont, Element.ALIGN_RIGHT));

            // Nested DEDUCTIONS table
            PdfPTable deductionsTable = new PdfPTable(2);
            deductionsTable.setWidths(new float[]{3f, 1.5f});
            deductionsTable.addCell(createDataCell("Tax Withheld:", normalFont));
            deductionsTable.addCell(createDataCell(formatCurrency(payslip.getTax()), normalFont, Element.ALIGN_RIGHT));
            deductionsTable.addCell(createDataCell("Other Deductions:", normalFont));
            deductionsTable.addCell(createDataCell(formatCurrency(payslip.getDeductions()), normalFont, Element.ALIGN_RIGHT));
            
            PdfPCell earningCell = new PdfPCell(earnings);
            earningCell.setBorder(Rectangle.NO_BORDER);
            PdfPCell deductionCell = new PdfPCell(deductionsTable);
            deductionCell.setBorder(Rectangle.NO_BORDER);

            detailTable.addCell(earningCell);
            detailTable.addCell(deductionCell);
            document.add(detailTable);

            // --- 5. Net Pay Footer ---
            Paragraph netPay = new Paragraph("NET PAY: " + formatCurrency(payslip.getNetSalary()), netPayFont);
            netPay.setAlignment(Element.ALIGN_RIGHT);
            netPay.setSpacingBefore(15f);
            document.add(netPay);
            
            // --- 6. Signature Line ---
            document.add(new Paragraph("\n\n_____________________________", normalFont));
            document.add(new Paragraph("Payroll Manager Signature", normalFont));

            document.close();
            return true;
            
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Utility to format BigDecimal to Currency String
     */
    private String formatCurrency(java.math.BigDecimal amount) {
    	if (amount == null) {
            return "ETB 0.00";
        }
        return String.format("ETB %,.2f", amount);}

    // --- PDF Helper Methods ---

    private PdfPCell createCell(String content, Font font, int alignment, int border, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(border);
        cell.setPadding(5f);
        cell.setBackgroundColor(backgroundColor);
        return cell;
    }

    private PdfPCell createHeaderCell(String content, Font font, BaseColor backgroundColor) {
        PdfPCell cell = createCell(content, font, Element.ALIGN_CENTER, Rectangle.BOTTOM, backgroundColor);
        cell.setBorderWidthBottom(1f);
        cell.setPadding(8f);
        return cell;
    }
    
    private PdfPCell createBoldLabelCell(String content) {
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        return createCell(content, boldFont, Element.ALIGN_LEFT, Rectangle.NO_BORDER, BaseColor.WHITE);
    }
    
    private PdfPCell createDataCell(String content, Font font) {
        return createDataCell(content, font, Element.ALIGN_LEFT);
    }
    
    private PdfPCell createDataCell(String content, Font font, int alignment) {
        PdfPCell cell = createCell(content, font, alignment, Rectangle.NO_BORDER, BaseColor.WHITE);
        cell.setPadding(3f);
        return cell;
    }
    
    // --- Alert Helper ---

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ------------------ DATA LOADING METHODS (Rest unchanged) ------------------

    private void loadProfile() {
        // ... (existing loadProfile logic) ...
        Employee emp = SessionManager.getCurrentEmployee(); 
        
        if (emp != null) {
            lblName.setText(emp.getFirstName() + " " + emp.getLastName());
            lblRole.setText(emp.getRoleName());
            
            try {
                Image profileImage = new Image(getClass().getResource("/images/default_profile.png").toExternalForm());
                imgProfile.setImage(profileImage);
            } catch (Exception e) {
                System.err.println("Profile image not found or failed to load. Using fallback. Error: " + e.getMessage());
            }
        } else {
            lblName.setText("N/A User");
            lblRole.setText("N/A Role");
        }
    }

    private void loadSalaryAndLeaveMetrics() {
        Employee emp = SessionManager.getCurrentEmployee();
        if (emp == null) return;

        try (Connection conn = DBConnection.getConnection()) {

            // NET SALARY
            PreparedStatement psSalary = conn.prepareStatement(
                "SELECT net_salary FROM payroll WHERE emp_id=? ORDER BY generated_on DESC LIMIT 1"
            );
            psSalary.setInt(1, emp.getEmployeeId());
            ResultSet rsSalary = psSalary.executeQuery();

            lblNetSalary.setText(
                rsSalary.next()
                ? "ETB " + rsSalary.getBigDecimal("net_salary")
                : "ETB 0.00"
            );

            // LEAVE BALANCE
            PreparedStatement psLeave = conn.prepareStatement(
                "SELECT remaining_days FROM leave_balance WHERE emp_id=?"
            );
            psLeave.setInt(1, emp.getEmployeeId());
            ResultSet rsLeave = psLeave.executeQuery();

            lblAvailableLeaves.setText(
                rsLeave.next()
                ? rsLeave.getInt("remaining_days") + " Days"
                : "0 Days"
            );

            // PAY DAY (FIXED LOGIC – 25th)
            int payday = 25;
            LocalDate today = LocalDate.now();
            LocalDate nextPay = today.withDayOfMonth(payday);
            if (!nextPay.isAfter(today)) {
                nextPay = nextPay.plusMonths(1);
            }

            lblPayDay.setText(nextPay.format(DATE_FORMAT));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    
    @FXML
    private void loadAttendanceData() {
        int empId = SessionManager.getCurrentEmployeeId(); // Get logged-in employee
        if (empId <= 0) return;

        // Fetch all records dynamically from DB
        ObservableList<AttendanceRecord> attendanceList = employeeDAO.getAllAttendance(empId);

        tblAttendance.setItems(attendanceList);
    }


    private void loadLeavesData() {
        // ... (existing loadLeavesData logic) ...
        int employeeId = SessionManager.getCurrentEmployeeId();
        tblLeaveRequests.setItems(employeeDAO.getEmployeeLeaveRequests(employeeId));
    }

    private void loadRequestsData() {
        // ... (existing loadRequestsData logic) ...
        int employeeId = SessionManager.getCurrentEmployeeId();
        tblOtherRequests.setItems(employeeDAO.getEmployeeOtherRequestsStatus(employeeId));
    }
    
    // ------------------ TABLE INITIALIZATION (Rest unchanged) ------------------
    
    private void initializeAttendanceTable() {
        TableColumn<AttendanceRecord, String> empCol = new TableColumn<>("Employee");
        empCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        TableColumn<AttendanceRecord, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<AttendanceRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toUpperCase()) {
                        case "PRESENT" -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        case "ABSENT" -> setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        case "LEAVE" -> setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        TableColumn<AttendanceRecord, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<AttendanceRecord, String> remarksCol = new TableColumn<>("Remarks");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        tblAttendance.getColumns().setAll(empCol, dateCol, statusCol, typeCol, remarksCol);
        tblAttendance.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }




    private void initializeLeaveTable() {
        // ... (existing initializeLeaveTable logic) ...
        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, LocalDate> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<LeaveRequest, LocalDate> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        
        TableColumn<LeaveRequest, String> reasonCol = new TableColumn<>("Reason Summary");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(250);

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblLeaveRequests.getColumns().addAll(typeCol, startCol, endCol, reasonCol, statusCol);
        tblLeaveRequests.setPlaceholder(new Label("No recent leave requests found."));
    }
    
    private void initializeOtherRequestsTable() {
        // ... (existing initializeOtherRequestsTable logic) ...
        TableColumn<String, String> requestCol = new TableColumn<>("Recent Request Status");
        requestCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        requestCol.setPrefWidth(500);

        tblOtherRequests.getColumns().add(requestCol);
        tblOtherRequests.setPlaceholder(new Label("No recent other requests (e.g., Bank/Advance) found."));
    }
}