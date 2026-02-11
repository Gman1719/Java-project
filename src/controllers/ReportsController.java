package controllers;

import dao.ReportDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Report;

public class ReportsController {

    @FXML private ComboBox<String> reportTypeFilter;
    @FXML private StackPane reportChartContainer;
    @FXML private TableView<Report> reportTable;
    @FXML private TableColumn<Report, String> colCol1, colCol2, colCol3, colCol4;
    @FXML private Label lblTotalCount, lblGrandTotal;

    private ObservableList<Report> reportList = FXCollections.observableArrayList();
    private final ReportDAO reportDAO = new ReportDAO();

    @FXML
    private void initialize() {
        setupTable();
        reportTypeFilter.getItems().addAll("Payroll Summary", "Attendance Summary", "Requests Summary");
        reportTypeFilter.setValue("Payroll Summary");
        onGenerateReport();
    }

    private void setupTable() {
        colCol1.setCellValueFactory(new PropertyValueFactory<>("col1"));
        colCol2.setCellValueFactory(new PropertyValueFactory<>("col2"));
        colCol3.setCellValueFactory(new PropertyValueFactory<>("col3"));
        colCol4.setCellValueFactory(new PropertyValueFactory<>("col4"));
        reportTable.setItems(reportList);
    }

    @FXML
    private void onGenerateReport() {
        String type = reportTypeFilter.getValue();
        if (type == null) return;

        reportList.clear();
        try {
            switch (type) {
                case "Payroll Summary":
                    updateUI("Department", "Total Net Salary", "Avg Net Salary", "Staff Count");
                    reportList.addAll(reportDAO.getPayrollSummaryTableData());
                    calculateTotals(true);
                    break;
                case "Attendance Summary":
                    updateUI("Employee", "Presents", "Absents", "Leave Days");
                    reportList.addAll(reportDAO.getAttendanceSummaryTableData());
                    calculateTotals(false);
                    break;
                case "Requests Summary":
                    updateUI("Type", "Total", "Approved", "Pending");
                    reportList.addAll(reportDAO.getRequestsSummaryTableData());
                    calculateTotals(false);
                    break;
            }
            loadChart(type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTotals(boolean isPayroll) {
        double grandTotal = 0;
        int count = 0;

        for (Report r : reportList) {
            try {
                if (isPayroll) {
                    // Strips non-numeric chars (like $) and parses the Total Salary from col2
                    grandTotal += Double.parseDouble(r.getCol2().replaceAll("[^\\d.]", ""));
                    count += Integer.parseInt(r.getCol4());
                } else {
                    count += reportList.size(); // Default count for other reports
                }
            } catch (Exception e) { /* Skip errors */ }
        }

        lblTotalCount.setText("Total Records: " + (isPayroll ? count : reportList.size()));
        lblGrandTotal.setText(isPayroll ? String.format("Net Disbursement: $%.2f", grandTotal) : "");
        lblGrandTotal.setVisible(isPayroll);
    }

    private void updateUI(String c1, String c2, String c3, String c4) {
        colCol1.setText(c1);
        colCol2.setText(c2);
        colCol3.setText(c3);
        colCol4.setText(c4);
    }

    private void loadChart(String type) {
        reportChartContainer.getChildren().clear();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(type + " Overview");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Report r : reportList) {
            try {
                // Charts the first column name vs the second column value
                double val = Double.parseDouble(r.getCol2().replaceAll("[^\\d.]", ""));
                series.getData().add(new XYChart.Data<>(r.getCol1(), val));
            } catch (Exception e) {}
        }
        chart.getData().add(series);
        reportChartContainer.getChildren().add(chart);
    }

    @FXML private void onExportPDF() { /* Logic for iText */ }
}