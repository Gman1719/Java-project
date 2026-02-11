package utils;

import models.PayrollRecord;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.util.List;

public class PDFGenerator {
    public static void generatePayrollReport(List<PayrollRecord> records, String filePath) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            document.add(new Paragraph("Company Payroll Summary Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6); // 6 columns
            table.addCell("ID");
            table.addCell("Name");
            table.addCell("Dept");
            table.addCell("Attendance");
            table.addCell("Net Salary");
            table.addCell("Status");

            for (PayrollRecord r : records) {
                table.addCell(String.valueOf(r.getId()));
                table.addCell(r.getName());
                table.addCell(r.getDepartment());
                table.addCell(String.valueOf(r.getAttendance()));
                table.addCell(String.format("₱%,.2f", r.getNetSalary()));
                table.addCell(r.getStatus());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}