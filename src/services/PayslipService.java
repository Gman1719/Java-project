package services;

import controllers.Payslip;
import utils.DBConnection;
import utils.SessionManager;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Optional;

public class PayslipService {

    private final Stage stage;

    // Constructor required to show Save Dialogs
    public PayslipService(Stage stage) {
        this.stage = stage;
    }

    /**
     * Handles the UI logic for downloading the latest payslip.
     */
    public void downloadLatestPayslip() {
        int empId = SessionManager.getCurrentEmployeeId();
        Optional<Payslip> payslipOpt = getLatestPayslip(empId);

        if (payslipOpt.isEmpty()) {
            showAlert("No Record", "No payslip record found for the current period.");
            return;
        }

        Payslip payslip = payslipOpt.get();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payslip PDF");
        fileChooser.setInitialFileName("Payslip_" + payslip.getMonth() + "_" + empId + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            if (generatePayslipPdf(payslip, file)) {
                showAlert("Success", "Payslip has been saved successfully to: " + file.getAbsolutePath());
            } else {
                showAlert("Error", "Failed to generate PDF. Please check if the file is open in another program.");
            }
        }
    }

    /**
     * Generate PDF for a given Payslip object.
     */
    public boolean generatePayslipPdf(Payslip payslip, File file) {
        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Fonts
            Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Font netPayFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(3, 106, 3));

            // Header Section
            Paragraph companyName = new Paragraph("COMPANY PAYROLL SERVICES", companyFont);
            companyName.setAlignment(Element.ALIGN_CENTER);
            companyName.setSpacingAfter(5f);
            document.add(companyName);

            Paragraph payslipTitle = new Paragraph("OFFICIAL PAYSLIP - " 
                    + payslip.getMonth().toUpperCase() + " " 
                    + payslip.getDateProcessed().getYear(), titleFont);
            payslipTitle.setAlignment(Element.ALIGN_CENTER);
            payslipTitle.setSpacingAfter(15f);
            document.add(payslipTitle);

            // Employee Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);
            infoTable.setWidths(new float[]{1f, 1.5f, 1f, 1.5f});

            infoTable.addCell(createBoldLabelCell("Employee:"));
            infoTable.addCell(createDataCell(payslip.getEmployeeName(), normalFont));
            infoTable.addCell(createBoldLabelCell("Date:"));
            infoTable.addCell(createDataCell(payslip.getDateProcessedString(), normalFont, Element.ALIGN_RIGHT));

            infoTable.addCell(createBoldLabelCell("ID:"));
            infoTable.addCell(createDataCell(String.valueOf(payslip.getEmpId()), normalFont));
            infoTable.addCell(createBoldLabelCell("Payslip ID:"));
            infoTable.addCell(createDataCell(String.valueOf(payslip.getPayslipId()), normalFont, Element.ALIGN_RIGHT));
            document.add(infoTable);

            // Detailed Earnings/Deductions Table
            PdfPTable detailTable = new PdfPTable(2);
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingAfter(20f);
            
            BaseColor earningColor = new BaseColor(210, 230, 255);
            BaseColor deductionColor = new BaseColor(255, 230, 210);

            detailTable.addCell(createHeaderCell("EARNINGS", headerFont, earningColor));
            detailTable.addCell(createHeaderCell("DEDUCTIONS", headerFont, deductionColor));

            // Nested Earnings
            PdfPTable earningsSub = new PdfPTable(2);
            earningsSub.addCell(createDataCell("Gross Base Salary:", normalFont));
            earningsSub.addCell(createDataCell(payslip.getFormattedGrossSalary(), normalFont, Element.ALIGN_RIGHT));
            earningsSub.addCell(createDataCell("Allowances:", normalFont));
            earningsSub.addCell(createDataCell("$" + String.format("%.2f", payslip.getAllowances()), normalFont, Element.ALIGN_RIGHT));
            
            PdfPCell earnWrapper = new PdfPCell(earningsSub);
            earnWrapper.setBorder(Rectangle.NO_BORDER);
            detailTable.addCell(earnWrapper);

            // Nested Deductions
            PdfPTable deductSub = new PdfPTable(2);
            deductSub.addCell(createDataCell("Tax Withheld:", normalFont));
            deductSub.addCell(createDataCell("$" + String.format("%.2f", payslip.getTax()), normalFont, Element.ALIGN_RIGHT));
            deductSub.addCell(createDataCell("Other Deductions:", normalFont));
            deductSub.addCell(createDataCell("$" + String.format("%.2f", payslip.getDeductions()), normalFont, Element.ALIGN_RIGHT));

            PdfPCell deductWrapper = new PdfPCell(deductSub);
            deductWrapper.setBorder(Rectangle.NO_BORDER);
            detailTable.addCell(deductWrapper);

            document.add(detailTable);

            // Net Pay Total
            Paragraph netPay = new Paragraph("NET PAY: " + payslip.getFormattedNetSalary(), netPayFont);
            netPay.setAlignment(Element.ALIGN_RIGHT);
            document.add(netPay);

            document.add(new Paragraph("\n\n_____________________________", normalFont));
            document.add(new Paragraph("Payroll Manager Signature", normalFont));

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Payslip> getLatestPayslip(int empId) {
        String sql = "SELECT * FROM payroll WHERE emp_id = ? ORDER BY generated_on DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(new Payslip(
                        rs.getString("month"),
                        rs.getDouble("net_salary"),
                        rs.getDouble("base_salary"), // Assumed mapping to gross_salary
                        rs.getDate("generated_on").toLocalDate(),
                        rs.getInt("payroll_id"),
                        rs.getDouble("allowances"),
                        rs.getDouble("deductions"),
                        rs.getDouble("tax"),
                        empId,
                        getEmployeeName(empId)
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    private String getEmployeeName(int empId) {
        String sql = "SELECT first_name, last_name FROM users u JOIN employees e ON u.user_id = e.user_id WHERE e.emp_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, empId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("first_name") + " " + rs.getString("last_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Employee #" + empId;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // PDF Helpers
    private PdfPCell createCell(String content, Font font, int align, int border, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(border);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell createHeaderCell(String content, Font font, BaseColor bg) {
        return createCell(content, font, Element.ALIGN_CENTER, Rectangle.BOTTOM, bg);
    }

    private PdfPCell createBoldLabelCell(String content) {
        return createCell(content, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD), Element.ALIGN_LEFT, Rectangle.NO_BORDER, BaseColor.WHITE);
    }

    private PdfPCell createDataCell(String content, Font font, int alignment) {
        return createCell(content, font, alignment, Rectangle.NO_BORDER, BaseColor.WHITE);
    }

    private PdfPCell createDataCell(String content, Font font) {
        return createDataCell(content, font, Element.ALIGN_LEFT);
    }
    
    public void showPayslipHistory() {
        // Implementation for a history view if needed
    }
}