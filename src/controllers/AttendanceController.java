package controllers;

import dao.AttendanceDAO;
import models.Attendance;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AttendanceController implements Initializable {

    @FXML private TableView<Attendance> attendanceTable;
    @FXML private TableColumn<Attendance, String> colEmployee, colStatus, colLeaveType, colRemarks;
    @FXML private TableColumn<Attendance, LocalDate> colDate;
    
    @FXML private TextField searchBar;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> leaveTypeFilter;
    @FXML private Label lblPresentCount, lblLeaveCount, lblAbsentCount;

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final ObservableList<Attendance> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colEmployee.setCellValueFactory(cd -> cd.getValue().employeeNameProperty());
        colDate.setCellValueFactory(cd -> cd.getValue().dateProperty());
        colStatus.setCellValueFactory(cd -> cd.getValue().statusProperty());
        colLeaveType.setCellValueFactory(cd -> cd.getValue().leaveTypeProperty());
        colRemarks.setCellValueFactory(cd -> cd.getValue().remarksProperty());

        setupStatusTextColors();
        setupRowClickListener();

        if (leaveTypeFilter != null) {
            leaveTypeFilter.setItems(FXCollections.observableArrayList("All Statuses", "Present", "Absent", "Leave"));
            leaveTypeFilter.getSelectionModel().selectFirst();
        }

        onFilter();
    }

    @FXML
    private void onFilter() {
        String name = (searchBar != null) ? searchBar.getText().trim() : "";
        LocalDate date = (dateFilter != null) ? dateFilter.getValue() : null;
        String status = (leaveTypeFilter != null) ? leaveTypeFilter.getValue() : "All Statuses";

        List<Attendance> results = attendanceDAO.getFilteredRecords(date, name, status);
        masterData.setAll(results);
        attendanceTable.setItems(masterData);
        updateKPIs();
    }

    @FXML
    private void onMarkAttendance() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mark Attendance");
        dialog.setHeaderText("Enter Daily Attendance Details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(20));

        ComboBox<String> cbEmployee = new ComboBox<>(attendanceDAO.getAllEmployeeNames());
        cbEmployee.setPromptText("Select Employee");
        cbEmployee.setPrefWidth(200);

        DatePicker dp = new DatePicker(LocalDate.now());
        
        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("Present", "Absent", "Leave"));
        cbStatus.setValue("Present");

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Regular", "Overtime", "Half-Day", "Sick", "Casual"));
        cbType.setValue("Regular");

        TextField txtRemarks = new TextField();
        txtRemarks.setPromptText("Optional remarks");

        grid.add(new Label("Employee: *"), 0, 0); grid.add(cbEmployee, 1, 0);
        grid.add(new Label("Date: *"), 0, 1);     grid.add(dp, 1, 1);
        grid.add(new Label("Status: *"), 0, 2);   grid.add(cbStatus, 1, 2);
        grid.add(new Label("Entry Type:"), 0, 3); grid.add(cbType, 1, 3);
        grid.add(new Label("Remarks:"), 0, 4);    grid.add(txtRemarks, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (cbEmployee.getValue() == null) {
                    showAlert("Validation Error", "Please select an employee.");
                } else if (dp.getValue() == null) {
                    showAlert("Validation Error", "Please select a valid date.");
                } else if (dp.getValue().isAfter(LocalDate.now())) {
                    showAlert("Validation Error", "Cannot mark attendance for future dates.");
                } else {
                    // Logic updated: Passes cbType.getValue() as its own argument for the 'Type' column
                    attendanceDAO.saveAttendance(
                        cbEmployee.getValue(), 
                        dp.getValue(), 
                        cbStatus.getValue(), 
                        cbType.getValue(), 
                        txtRemarks.getText()
                    );
                    onFilter();
                }
            }
        });
    }

    private void handleEditAttendance(Attendance attendance) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Record");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("Present", "Absent", "Leave"));
        cbStatus.setValue(attendance.getStatus());
        
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Regular", "Overtime", "Half-Day", "Sick", "Casual"));
        cbType.setValue(attendance.getLeaveType());

        DatePicker dp = new DatePicker(attendance.getDate());
        TextField txtRem = new TextField(attendance.getRemarks());

        grid.add(new Label("Status:"), 0, 0);   grid.add(cbStatus, 1, 0);
        grid.add(new Label("Entry Type:"), 0, 1); grid.add(cbType, 1, 1);
        grid.add(new Label("Date:"), 0, 2);     grid.add(dp, 1, 2);
        grid.add(new Label("Remarks:"), 0, 3);  grid.add(txtRem, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                if (dp.getValue() == null) {
                    showAlert("Validation Error", "Date cannot be empty.");
                } else {
                    // Logic updated: Passes cbType.getValue() as its own argument
                    attendanceDAO.updateAttendance(
                        attendance.getId(), 
                        dp.getValue(), 
                        cbStatus.getValue(), 
                        cbType.getValue(), 
                        txtRem.getText()
                    );
                    onFilter();
                }
            }
        });
    }

    private void updateKPIs() {
        long p = masterData.stream().filter(a -> a.getStatus().equalsIgnoreCase("Present")).count();
        long ab = masterData.stream().filter(a -> a.getStatus().equalsIgnoreCase("Absent")).count();
        long l = masterData.stream().filter(a -> a.getStatus().equalsIgnoreCase("Leave")).count();
        
        lblPresentCount.setText(String.format("%02d", p));
        lblAbsentCount.setText(String.format("%02d", ab));
        lblLeaveCount.setText(String.format("%02d", l));
    }

    private void setupStatusTextColors() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    String color = switch (item.toLowerCase()) {
                        case "present" -> "#10b981"; 
                        case "absent" -> "#ef4444";  
                        default -> "#3b82f6";        
                    };
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupRowClickListener() {
        attendanceTable.setRowFactory(tv -> {
            TableRow<Attendance> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditAttendance(row.getItem());
                }
            });
            return row;
        });
    }

    @FXML
    private void onDownloadReport() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("Attendance_Report_" + LocalDate.now() + ".csv");
        File file = chooser.showSaveDialog(attendanceTable.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Employee,Date,Status,Type,Remarks");
                for (Attendance a : masterData) {
                    writer.println(a.getEmployeeName() + "," + a.getDate() + "," + a.getStatus() + "," + a.getLeaveType() + "," + a.getRemarks());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onResetFilters() {
        if(searchBar != null) searchBar.clear();
        if(dateFilter != null) dateFilter.setValue(null);
        if(leaveTypeFilter != null) leaveTypeFilter.getSelectionModel().selectFirst();
        onFilter();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}