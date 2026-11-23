package org.example.medistock.controller.inventory;
import org.example.medistock.util.DBConnection;
import java.sql.Connection;
import java.net.URL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.medistock.config.DatabaseConfig;

public class inventoryController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button clearBtn;

    @FXML
    private Button delBtn;

    @FXML
    private Button fetchBtn;

    @FXML
    private ComboBox<String> getCategory;

    @FXML
    private TextArea getDescription;

    @FXML
    private ComboBox<String> getDosage;

    @FXML
    private TextField getGenName;

    @FXML
    private TextField getManName;

    @FXML
    private TextField getMedName;

    @FXML
    private TextField getStrength;

    @FXML
    private Button saveBtn;

    @FXML
    void doClear(ActionEvent event) {
        getMedName.clear();
        getGenName.clear();
        getManName.clear();
        getStrength.clear();
        getDescription.clear();

        getCategory.getSelectionModel().clearSelection();
        getDosage.getSelectionModel().clearSelection();
    }

    @FXML
    void doDelete(ActionEvent event) {
        try {
            String medName = getMedName.getText();

            if (medName.isEmpty()) {
                System.out.println("Enter medicine name first");
                return;
            }

            String sql = "DELETE FROM medicine WHERE medicine_name = ?";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, medName);

            int count = pst.executeUpdate();

            if (count > 0) {
                System.out.println("Medicine Deleted Successfully");
                doClear(event);
            } else {
                System.out.println("Medicine Not Found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    void doFetch(ActionEvent event) {
        try {
            String medName = getMedName.getText();

            if (medName.isEmpty()) {
                System.out.println("Enter medicine name first");
                return;
            }

            String sql = "SELECT * FROM medicine WHERE medicine_name = ?";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, medName);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                getGenName.setText(rs.getString("generic_name"));
                getManName.setText(rs.getString("manufacturer_name"));
                getCategory.setValue(rs.getString("category"));
                getStrength.setText(rs.getString("strength"));
                getDosage.setValue(rs.getString("dosage_form"));
                getDescription.setText(rs.getString("description"));

                System.out.println("Medicine Found");
            } else {
                System.out.println("Medicine Not Found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    void doSave(ActionEvent event) {

            String medName = getMedName.getText();
            String genName = getGenName.getText();
            String manName = getManName.getText();
            String category = String.valueOf(getCategory.getValue());
            String strength = getStrength.getText();
            String dosage = String.valueOf(getDosage.getValue());
            String description = getDescription.getText();
      try{
            if (medName.isEmpty() || manName.isEmpty()
                    || getCategory.getValue() == null
                    || getDosage.getValue() == null) {

                System.out.println("Please fill all required fields");
                return;
            }

            String sql = "INSERT INTO medicine "
                    + "(medicine_name, generic_name, manufacturer_name, "
                    + "category, strength, dosage_form, description) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, medName);
            pst.setString(2, genName);
            pst.setString(3, manName);
            pst.setString(4, category);
            pst.setString(5, strength);
            pst.setString(6, dosage);
            pst.setString(7, description);

            int count = pst.executeUpdate();

            if (count > 0) {
                System.out.println("Medicine Saved Successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    Connection con;
    @FXML
    void initialize() {
        con= DBConnection.connectToMySql();
        getCategory.getItems().addAll(
                "Pain Relief",
                "Antibiotic",
                "Antacid",
                "Antihistamine",
                "Antidiabetic",
                "Antihypertensive",
                "Antiseptic",
                "Vitamins & Supplements",
                "Cough & Cold"
        );

        getDosage.getItems().addAll(
                "Tablet",
                "Capsule",
                "Syrup",
                "Suspension",
                "Injection",
                "Drops",
                "Cream",
                "Ointment",
                "Inhaler",
                "Powder"
        );
        assert clearBtn != null : "fx:id=\"clearBtn\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert delBtn != null : "fx:id=\"delBtn\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert fetchBtn != null : "fx:id=\"fetchBtn\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getCategory != null : "fx:id=\"getCategory\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getDescription != null : "fx:id=\"getDescription\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getDosage != null : "fx:id=\"getDosage\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getGenName != null : "fx:id=\"getGenName\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getManName != null : "fx:id=\"getManName\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getMedName != null : "fx:id=\"getMedName\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert getStrength != null : "fx:id=\"getStrength\" was not injected: check your FXML file 'InventoryView.fxml'.";
        assert saveBtn != null : "fx:id=\"saveBtn\" was not injected: check your FXML file 'InventoryView.fxml'.";

    }

}
