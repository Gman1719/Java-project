package controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import models.Payslip;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

public class PdfUtils {

    public static boolean generatePayslipPdf(Payslip payslip, File file) {
        // Ensure directories exist before writing
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // --- 1. Define Fonts ---
            Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font netPayFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(3, 106, 3));

            // --- 2. Header ---
            Paragraph companyName = new Paragraph("COMPANY PAYROLL SERVICES", companyFont);
            companyName.setAlignment(Element.ALIGN_CENTER);
            companyName.setSpacingAfter(5f);
            document.add(companyName);

            String monthStr = (payslip.getMonth() != null) ? payslip.getMonth().toUpperCase() : "N/A";
            Paragraph payslipTitle = new Paragraph("OFFICIAL PAYSLIP - " + monthStr + " " + payslip.getYear(), titleFont);
            payslipTitle.setAlignment(Element.ALIGN_CENTER);
            payslipTitle.setSpacingAfter(15f);
            document.add(payslipTitle);

            // --- 3. Info Table ---
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);
            infoTable.setWidths(new float[]{1f, 1.5f, 1f, 1.5f});

            // Row 1: Employee and Date
            infoTable.addCell(createBoldLabelCell("Employee:"));
            // Since your model doesn't have name, we use ID or you must add getName() to model
            infoTable.addCell(createDataCell("Employee ID: " + payslip.getEmpId(), normalFont));

            infoTable.addCell(createBoldLabelCell("Date:"));
            String dateStr = "N/A";
            if (payslip.getGeneratedOn() != null) {
                dateStr = new SimpleDateFormat("yyyy-MM-dd").format(payslip.getGeneratedOn());
            }
            infoTable.addCell(createDataCell(dateStr, normalFont, Element.ALIGN_RIGHT));

            // Row 2: IDs
            infoTable.addCell(createBoldLabelCell("Payroll ID:"));
            infoTable.addCell(createDataCell(String.valueOf(payslip.getPayrollId()), normalFont));
            infoTable.addCell(createBoldLabelCell("Payslip ID:"));
            infoTable.addCell(createDataCell(String.valueOf(payslip.getPayslipId()), normalFont, Element.ALIGN_RIGHT));

            document.add(infoTable);

            // --- 4. Earnings & Deductions Section ---
            PdfPTable detailTable = new PdfPTable(2);
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingAfter(20f);

            BaseColor earningColor = new BaseColor(210, 230, 255);
            BaseColor deductionColor = new BaseColor(255, 230, 210);

            detailTable.addCell(createHeaderCell("EARNINGS", headerFont, earningColor));
            detailTable.addCell(createHeaderCell("DEDUCTIONS", headerFont, deductionColor));

            // Earnings Column
            PdfPTable earnings = new PdfPTable(2);
            earnings.setWidths(new float[]{2f, 1f});
            earnings.addCell(createDataCell("Base Salary:", normalFont));
            earnings.addCell(createDataCell(formatCurrency(payslip.getBaseSalary()), normalFont, Element.ALIGN_RIGHT));
            earnings.addCell(createDataCell("Allowances:", normalFont));
            earnings.addCell(createDataCell(formatCurrency(payslip.getAllowances()), normalFont, Element.ALIGN_RIGHT));

            // Deductions Column
            PdfPTable deductions = new PdfPTable(2);
            deductions.setWidths(new float[]{2f, 1f});
            deductions.addCell(createDataCell("Tax:", normalFont));
            deductions.addCell(createDataCell(formatCurrency(payslip.getTax()), normalFont, Element.ALIGN_RIGHT));
            deductions.addCell(createDataCell("Other Deductions:", normalFont));
            deductions.addCell(createDataCell(formatCurrency(payslip.getDeductions()), normalFont, Element.ALIGN_RIGHT));

            PdfPCell eCell = new PdfPCell(earnings); eCell.setBorder(Rectangle.NO_BORDER);
            PdfPCell dCell = new PdfPCell(deductions); dCell.setBorder(Rectangle.NO_BORDER);

            detailTable.addCell(eCell);
            detailTable.addCell(dCell);
            document.add(detailTable);

            // --- 5. Summary Footer ---
            Paragraph netPay = new Paragraph("NET PAY: " + formatCurrency(payslip.getNetSalary()), netPayFont);
            netPay.setAlignment(Element.ALIGN_RIGHT);
            document.add(netPay);

            document.add(new Paragraph("\n\n_____________________________", normalFont));
            document.add(new Paragraph("Payroll Department Signature", normalFont));

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper for BigDecimal formatting to avoid errors
    private static String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f", amount.doubleValue());
    }

    private static PdfPCell createHeaderCell(String content, Font font, BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(color);
        cell.setPadding(8f);
        cell.setBorder(Rectangle.BOTTOM);
        return cell;
    }

    private static PdfPCell createBoldLabelCell(String content) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3f);
        return cell;
    }

    private static PdfPCell createDataCell(String content, Font font) {
        return createDataCell(content, font, Element.ALIGN_LEFT);
    }

    private static PdfPCell createDataCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3f);
        return cell;
    }
}