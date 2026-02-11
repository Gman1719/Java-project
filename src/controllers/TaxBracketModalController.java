package controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.TaxBracket;

public class TaxBracketModalController {

    @FXML private TextField txtMinSalary;
    @FXML private TextField txtMaxSalary;
    @FXML private TextField txtTaxRate;

    private ObservableList<TaxBracket> taxBracketList;
    private TaxBracket bracketToEdit;

    /**
     * Called by SystemSettingsController to pass the list reference
     */
    public void setTaxBracketList(ObservableList<TaxBracket> list) {
        this.taxBracketList = list;
    }

    /**
     * Called by SystemSettingsController if we are editing an existing bracket
     */
    public void setBracket(TaxBracket bracket) {
        this.bracketToEdit = bracket;
        txtMinSalary.setText(String.valueOf(bracket.getMinSalary()));
        txtMaxSalary.setText(String.valueOf(bracket.getMaxSalary()));
        txtTaxRate.setText(String.valueOf(bracket.getRate()));
    }

    @FXML
    private void handleSave() {
        try {
            double min = Double.parseDouble(txtMinSalary.getText());
            double max = Double.parseDouble(txtMaxSalary.getText());
            double rate = Double.parseDouble(txtTaxRate.getText());

            if (bracketToEdit == null) {
                // ADD NEW
                taxBracketList.add(new TaxBracket(min, max, rate));
            } else {
                // UPDATE EXISTING
                bracketToEdit.setMinSalary(min);
                bracketToEdit.setMaxSalary(max);
                bracketToEdit.setRate(rate);
                // Refresh the list to update TableView
                int index = taxBracketList.indexOf(bracketToEdit);
                taxBracketList.set(index, bracketToEdit);
            }
            closeStage();
        } catch (NumberFormatException e) {
            showError("Invalid input. Please enter numeric values.");
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        ((Stage) txtMinSalary.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}