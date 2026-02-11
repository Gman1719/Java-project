package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Optional;
import java.util.ResourceBundle;

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

import models.Payslip;
import utils.DBConnection; 

public class PayslipsHistoryController implements Initializable {

    private int employeeId = -1;

    @FXML private ComboBox<String> cbYearFilter; 
    @FXML private TableView<Payslip> tblPayslips; 
    @FXML private TableColumn<Payslip, String> colMonth;
    @FXML private TableColumn<Payslip, String> colNetSalary;
    @FXML private TableColumn<Payslip, String> colGrossSalary;
    @FXML private TableColumn<Payslip, String> colDateProcessed;
    @FXML private TableColumn<Payslip, Void> colActions; 

    private ObservableList<Payslip> payslipList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        populateYearFilter();
    }
    
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
        loadPayslipsData(null); 
    }

    private void setupTableColumns() {
        colMonth.setCellValueFactory(new PropertyValueFactory<>("month")); 
        
        // Use custom formatting since getFormatted methods might not be in the model
        colGrossSalary.setCellValueFactory(data -> 
            new SimpleStringProperty("$" + String.format("%,.2f", data.getValue().getBaseSalary())));
        colNetSalary.setCellValueFactory(data -> 
            new SimpleStringProperty("$" + String.format("%,.2f", data.getValue().getNetSalary())));
        colDateProcessed.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getGeneratedOn().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        
        colActions.setCellFactory(param -> new TableCell<Payslip, Void>() {
            private final Button downloadBtn = new Button("Download PDF");
            {
                downloadBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white;");
                downloadBtn.setOnAction(event -> {
                    Payslip payslip = getTableView().getItems().get(getIndex()); 
                    handleDownload(payslip.getPayslipId()); 
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : downloadBtn);
            }
        });
        
        tblPayslips.setItems(payslipList);
    }

    private void populateYearFilter() {
        cbYearFilter.getItems().add("All Years"); 
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 6; i++) {
            cbYearFilter.getItems().add(String.valueOf(currentYear - i));
        }
        cbYearFilter.setValue("All Years");
    }

    private void loadPayslipsData(String year) {
        payslipList.clear();
        if (this.employeeId <= 0) return;

        String sql = "SELECT * FROM payroll WHERE emp_id = ?"; 
        if (year != null && !year.equals("All Years")) sql += " AND year = ?";
        sql += " ORDER BY generated_on DESC"; 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, employeeId);
            if (year != null && !year.equals("All Years")) pst.setInt(2, Integer.parseInt(year));

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                payslipList.add(mapResultSetToPayslip(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Payslip mapResultSetToPayslip(ResultSet rs) throws SQLException {
        // Correctly matches the 12-argument constructor in your model
        return new Payslip(
            rs.getInt("payroll_id"), 
            rs.getInt("payroll_id"), 
            rs.getInt("emp_id"), 
            getEmployeeName(rs.getInt("emp_id")), 
            rs.getString("month"), 
            rs.getInt("year"), 
            rs.getBigDecimal("base_salary"), 
            rs.getBigDecimal("allowances"), 
            rs.getBigDecimal("deductions"), 
            rs.getBigDecimal("tax"), 
            rs.getBigDecimal("net_salary"), 
            rs.getTimestamp("generated_on")
        );
    }

    private String getEmployeeName(int empId) {
        String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("first_name") + " " + rs.getString("last_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown";
    }

    private boolean generatePayslipPdf(Payslip payslip, File file) {
        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, fos);
            document.open();
            
            // Fixed the "new new Font" error here
            Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("COMPANY PAYROLL SERVICES", companyFont));
            document.add(new Paragraph("OFFICIAL PAYSLIP - " + payslip.getMonth() + " " + payslip.getYear(), titleFont));
            
            PdfPTable table = new PdfPTable(2);
            table.setSpacingBefore(20f);
            table.addCell("Employee Name:"); table.addCell(payslip.getEmployeeName());
            table.addCell("Base Salary:"); table.addCell("$" + payslip.getBaseSalary().toString());
            table.addCell("Net Salary:"); table.addCell("$" + payslip.getNetSalary().toString());
            
            document.add(table);
            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleDownload(int payslipId) {
        String sql = "SELECT * FROM payroll WHERE payroll_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, payslipId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Payslip p = mapResultSetToPayslip(rs);
                FileChooser fc = new FileChooser();
                fc.setInitialFileName("Payslip_" + p.getMonth() + ".pdf");
                File file = fc.showSaveDialog(tblPayslips.getScene().getWindow());
                if (file != null) generatePayslipPdf(p, file);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleFilter() { loadPayslipsData(cbYearFilter.getValue()); }
    @FXML private void handleClose() { ((Stage) tblPayslips.getScene().getWindow()).close(); }
}