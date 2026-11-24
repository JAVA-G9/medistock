
package org.example.medistock.controller.inventory;

import org.example.medistock.util.DBConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class inventoryController {

    // ---------------- INVENTORY TABLE ----------------

    @FXML
    private TableView<showBean> inventoryTable;

    private ObservableList<showBean> inventoryList =
            FXCollections.observableArrayList();


    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;


    // ---------------- MEDICINE FIELDS ----------------

    @FXML
    private Button clearBtn;

    @FXML
    private Button delBtn;

    @FXML
    private Button fetchBtn;

    @FXML
    private Button saveBtn;

    @FXML
    private Button updateBtn;

    @FXML
    private ComboBox<String> getCategory;

    @FXML
    private ComboBox<String> getDosage;

    @FXML
    private TextArea getDescription;

    @FXML
    private TextField getGenName;

    @FXML
    private TextField getManName;

    @FXML
    private TextField getMedName;

    @FXML
    private TextField getStrength;


    // ---------------- BATCH FIELDS ----------------

    @FXML
    private ComboBox<String> batchMedicine;

    @FXML
    private TextField batchNumber;

    @FXML
    private DatePicker manufacturingDate;

    @FXML
    private DatePicker expiryDate;

    @FXML
    private TextField quantityReceived;

    @FXML
    private TextField quantityAvailable;

    @FXML
    private TextField purchasePrice;

    @FXML
    private TextField sellingPrice;

    @FXML
    private TextField pharmacyId;

    @FXML
    private ComboBox<String> batchStatus;

    @FXML
    private DatePicker receivedDate;

    @FXML
    private Button saveBatchBtn;

    @FXML
    private Button fetchBatchBtn;

    @FXML
    private Button updateBatchBtn;

    @FXML
    private Button deleteBatchBtn;

    @FXML
    private Button clearBatchBtn;


    // ---------------- DATABASE CONNECTION ----------------

    Connection con;

    private int fetchedMedicineId = -1;


    // =====================================================
    //                  INVENTORY TABLE
    // =====================================================

    private void setupInventoryTable() {

        TableColumn<showBean, String> medicineNameCol =
                new TableColumn<>("Medicine");

        medicineNameCol.setCellValueFactory(
                new PropertyValueFactory<>("medicineName"));

        medicineNameCol.setMinWidth(130);


        TableColumn<showBean, String> batchNumberCol =
                new TableColumn<>("Batch Number");

        batchNumberCol.setCellValueFactory(
                new PropertyValueFactory<>("batchNumber"));

        batchNumberCol.setMinWidth(110);


        TableColumn<showBean, Integer> quantityReceivedCol =
                new TableColumn<>("Received");

        quantityReceivedCol.setCellValueFactory(
                new PropertyValueFactory<>("quantityReceived"));

        quantityReceivedCol.setMinWidth(90);


        TableColumn<showBean, Integer> quantityAvailableCol =
                new TableColumn<>("Available");

        quantityAvailableCol.setCellValueFactory(
                new PropertyValueFactory<>("quantityAvailable"));

        quantityAvailableCol.setMinWidth(90);


        TableColumn<showBean, Double> purchasePriceCol =
                new TableColumn<>("Purchase Price");

        purchasePriceCol.setCellValueFactory(
                new PropertyValueFactory<>("purchasePrice"));

        purchasePriceCol.setMinWidth(110);


        TableColumn<showBean, Double> sellingPriceCol =
                new TableColumn<>("Selling Price");

        sellingPriceCol.setCellValueFactory(
                new PropertyValueFactory<>("sellingPrice"));

        sellingPriceCol.setMinWidth(110);


        TableColumn<showBean, LocalDate> expiryDateCol =
                new TableColumn<>("Expiry Date");

        expiryDateCol.setCellValueFactory(
                new PropertyValueFactory<>("expiryDate"));

        expiryDateCol.setMinWidth(110);


        inventoryTable.getColumns().clear();

        inventoryTable.getColumns().addAll(
                medicineNameCol,
                batchNumberCol,
                quantityReceivedCol,
                quantityAvailableCol,
                purchasePriceCol,
                sellingPriceCol,
                expiryDateCol
        );
    }


    private ObservableList<showBean> getAllInventoryRecords() {

        ObservableList<showBean> list =
                FXCollections.observableArrayList();

        String sql = "SELECT m.medicine_name, b.batch_number, "
                + "b.quantity_received, b.quantity_available, "
                + "b.purchase_price, b.selling_price, b.expiry_date "
                + "FROM medicine m "
                + "INNER JOIN medicine_batch b "
                + "ON m.medicine_id = b.medicine_id "
                + "WHERE b.pharmacy_id = ? "
                + "ORDER BY m.medicine_name";

        try (PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, pharmacyId.getText().trim());

            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {

                    String medicineName =
                            rs.getString("medicine_name");

                    String batchNo =
                            rs.getString("batch_number");

                    int received =
                            rs.getInt("quantity_received");

                    int available =
                            rs.getInt("quantity_available");

                    double purchase =
                            rs.getDouble("purchase_price");

                    double selling =
                            rs.getDouble("selling_price");

                    LocalDate expiry =
                            rs.getDate("expiry_date").toLocalDate();

                    showBean obj = new showBean(
                            medicineName,
                            batchNo,
                            received,
                            available,
                            purchase,
                            selling,
                            expiry
                    );

                    list.add(obj);
                }
            }

        } catch (SQLException e) {

            System.out.println("Error fetching inventory records");
            e.printStackTrace();
        }

        return list;
    }


    private void loadInventoryTable() {

        inventoryTable.setItems(null);

        inventoryList = getAllInventoryRecords();

        inventoryTable.setItems(inventoryList);
    }


    // =====================================================
    //                  MEDICINE METHODS
    // =====================================================

    @FXML
    void doClear(ActionEvent event) {

        getMedName.clear();
        getGenName.clear();
        getManName.clear();
        getStrength.clear();
        getDescription.clear();

        getCategory.getSelectionModel().clearSelection();
        getDosage.getSelectionModel().clearSelection();

        fetchedMedicineId = -1;

        System.out.println("Medicine form cleared");
    }


    @FXML
    void doDelete(ActionEvent event) {

        try {

            String medName = getMedName.getText().trim();

            if (medName.isEmpty()) {
                System.out.println("Enter medicine name first");
                return;
            }

            String sql =
                    "DELETE FROM medicine WHERE medicine_name = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, medName);

                int count = pst.executeUpdate();

                if (count > 0) {

                    System.out.println("Medicine Deleted Successfully");

                    fetchedMedicineId = -1;
                    doClear(event);

                    loadBatchMedicines();
                    loadInventoryTable();

                } else {

                    System.out.println("Medicine Not Found");
                }
            }

        } catch (SQLException e) {

            System.out.println("Could not delete medicine.");
            System.out.println("Check whether batches are linked to this medicine.");

            e.printStackTrace();
        }
    }


    @FXML
    void doFetch(ActionEvent event) {

        try {

            String medName = getMedName.getText().trim();

            if (medName.isEmpty()) {
                System.out.println("Enter medicine name first");
                return;
            }

            fetchedMedicineId = -1;

            String sql =
                    "SELECT * FROM medicine WHERE medicine_name = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, medName);

                try (ResultSet rs = pst.executeQuery()) {

                    if (rs.next()) {

                        fetchedMedicineId =
                                rs.getInt("medicine_id");

                        getGenName.setText(
                                rs.getString("generic_name"));

                        getManName.setText(
                                rs.getString("manufacturer_name"));

                        getCategory.setValue(
                                rs.getString("category"));

                        getStrength.setText(
                                rs.getString("strength"));

                        getDosage.setValue(
                                rs.getString("dosage_form"));

                        getDescription.setText(
                                rs.getString("description"));

                        System.out.println("Medicine Found");

                    } else {

                        System.out.println("Medicine Not Found");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    void doSave(ActionEvent event) {

        String medName = getMedName.getText().trim();
        String genName = getGenName.getText().trim();
        String manName = getManName.getText().trim();

        String category = getCategory.getValue();
        String strength = getStrength.getText().trim();
        String dosage = getDosage.getValue();
        String description = getDescription.getText().trim();

        try {

            if (medName.isEmpty() || manName.isEmpty()
                    || category == null || dosage == null) {

                System.out.println(
                        "Please fill all required medicine fields");

                return;
            }

            String sql = "INSERT INTO medicine "
                    + "(medicine_name, generic_name, manufacturer_name, "
                    + "category, strength, dosage_form, description) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

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

                    loadBatchMedicines();
                }
            }

        } catch (SQLException e) {

            System.out.println("Could not save medicine.");
            e.printStackTrace();
        }
    }


    @FXML
    void doUpdate(ActionEvent event) {

        if (fetchedMedicineId == -1) {
            System.out.println(
                    "Fetch a medicine first before updating");

            return;
        }

        String medName = getMedName.getText().trim();
        String genName = getGenName.getText().trim();
        String manName = getManName.getText().trim();

        String category = getCategory.getValue();
        String strength = getStrength.getText().trim();
        String dosage = getDosage.getValue();
        String description = getDescription.getText().trim();

        if (medName.isEmpty() || manName.isEmpty()
                || category == null || dosage == null) {

            System.out.println(
                    "Please fill all required medicine fields");

            return;
        }

        String sql = "UPDATE medicine SET "
                + "medicine_name = ?, "
                + "generic_name = ?, "
                + "manufacturer_name = ?, "
                + "category = ?, "
                + "strength = ?, "
                + "dosage_form = ?, "
                + "description = ? "
                + "WHERE medicine_id = ?";

        try (PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, medName);
            pst.setString(2, genName);
            pst.setString(3, manName);
            pst.setString(4, category);
            pst.setString(5, strength);
            pst.setString(6, dosage);
            pst.setString(7, description);
            pst.setInt(8, fetchedMedicineId);

            int count = pst.executeUpdate();

            if (count > 0) {

                System.out.println("Medicine Updated Successfully");

                fetchedMedicineId = -1;

                loadBatchMedicines();
                loadInventoryTable();

            } else {

                System.out.println("Medicine Not Found");
            }

        } catch (SQLException e) {

            System.out.println("Could not update medicine");
            e.printStackTrace();
        }
    }


    // =====================================================
    //                BATCH HELPER METHODS
    // =====================================================

    private void loadBatchMedicines() {

        batchMedicine.getItems().clear();

        String sql = "SELECT medicine_id, medicine_name "
                + "FROM medicine ORDER BY medicine_name";

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                int id = rs.getInt("medicine_id");
                String name = rs.getString("medicine_name");

                batchMedicine.getItems().add(id + " - " + name);
            }

            System.out.println("Medicine dropdown loaded");

        } catch (SQLException e) {

            System.out.println("Error loading medicines");
            e.printStackTrace();
        }
    }


    private int getSelectedMedicineId() {

        String selected = batchMedicine.getValue();

        if (selected == null || !selected.contains(" - ")) {
            return -1;
        }

        try {

            return Integer.parseInt(
                    selected.substring(
                            0, selected.indexOf(" - ")).trim()
            );

        } catch (NumberFormatException e) {

            return -1;
        }
    }


    private String getSelectedMedicineName() {

        String selected = batchMedicine.getValue();

        if (selected == null || !selected.contains(" - ")) {
            return null;
        }

        return selected.substring(
                selected.indexOf(" - ") + 3).trim();
    }


    private boolean validateBatchFields() {

        if (batchMedicine.getValue() == null
                || batchNumber.getText().trim().isEmpty()
                || manufacturingDate.getValue() == null
                || expiryDate.getValue() == null
                || quantityReceived.getText().trim().isEmpty()
                || purchasePrice.getText().trim().isEmpty()
                || sellingPrice.getText().trim().isEmpty()
                || pharmacyId.getText().trim().isEmpty()
                || batchStatus.getValue() == null) {

            System.out.println(
                    "Please fill all required batch fields");

            return false;
        }

        if (manufacturingDate.getValue()
                .isAfter(expiryDate.getValue())) {

            System.out.println(
                    "Manufacturing date cannot be after expiry date");

            return false;
        }

        if (receivedDate.getValue() != null
                && manufacturingDate.getValue()
                .isAfter(receivedDate.getValue())) {

            System.out.println(
                    "Received date cannot be before manufacturing date");

            return false;
        }

        try {

            int received = Integer.parseInt(
                    quantityReceived.getText().trim());

            int available;

            if (quantityAvailable.getText().trim().isEmpty()) {
                available = received;
            } else {
                available = Integer.parseInt(
                        quantityAvailable.getText().trim());
            }

            double purchase = Double.parseDouble(
                    purchasePrice.getText().trim());

            double selling = Double.parseDouble(
                    sellingPrice.getText().trim());

            if (received < 0 || available < 0
                    || available > received) {

                System.out.println(
                        "Check received and available quantities");

                return false;
            }

            if (purchase < 0 || selling < 0) {

                System.out.println("Prices cannot be negative");
                return false;
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Enter valid numbers for quantities and prices");

            return false;
        }

        if (getSelectedMedicineId() == -1) {

            System.out.println("Please select a valid medicine");
            return false;
        }

        return true;
    }


    // =====================================================
    //                 SAVE MEDICINE BATCH
    // =====================================================

    @FXML
    void doSaveBatch(ActionEvent event) {

        try {

            if (!validateBatchFields()) {
                return;
            }

            int medicineId = getSelectedMedicineId();
            String medicineName = getSelectedMedicineName();

            int received = Integer.parseInt(
                    quantityReceived.getText().trim());

            int available;

            if (quantityAvailable.getText().trim().isEmpty()) {
                available = received;
            } else {
                available = Integer.parseInt(
                        quantityAvailable.getText().trim());
            }

            double purchase = Double.parseDouble(
                    purchasePrice.getText().trim());

            double selling = Double.parseDouble(
                    sellingPrice.getText().trim());

            LocalDate receivedOn = receivedDate.getValue();

            if (receivedOn == null) {
                receivedOn = LocalDate.now();
            }

            String sql = "INSERT INTO medicine_batch "
                    + "(medicine_id, batch_number, manufacturing_date, "
                    + "expiry_date, quantity_received, purchase_price, "
                    + "pharmacy_id, status, quantity_available, "
                    + "selling_price, received_date, medicine_name) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setInt(1, medicineId);
                pst.setString(2, batchNumber.getText().trim());

                pst.setDate(3,
                        java.sql.Date.valueOf(
                                manufacturingDate.getValue()));

                pst.setDate(4,
                        java.sql.Date.valueOf(
                                expiryDate.getValue()));

                pst.setInt(5, received);
                pst.setDouble(6, purchase);
                pst.setString(7, pharmacyId.getText().trim());
                pst.setString(8, batchStatus.getValue());
                pst.setInt(9, available);
                pst.setDouble(10, selling);

                pst.setDate(11,
                        java.sql.Date.valueOf(receivedOn));

                pst.setString(12, medicineName);

                int count = pst.executeUpdate();

                if (count > 0) {

                    System.out.println(
                            "Medicine Batch Saved Successfully");

                    loadInventoryTable();

                    doClearBatch(event);
                }
            }

        } catch (SQLException e) {

            System.out.println("Could not save batch.");
            System.out.println(
                    "Check whether the batch number already exists.");

            e.printStackTrace();

        } catch (Exception e) {

            System.out.println(
                    "Something went wrong while saving the batch.");

            e.printStackTrace();
        }
    }


    // =====================================================
    //                 FETCH MEDICINE BATCH
    // =====================================================

    @FXML
    void doFetchBatch(ActionEvent event) {

        try {

            String number = batchNumber.getText().trim();
            String pharmacy = pharmacyId.getText().trim();

            if (number.isEmpty() || pharmacy.isEmpty()) {

                System.out.println(
                        "Enter batch number and pharmacy ID first");

                return;
            }

            String sql = "SELECT * FROM medicine_batch "
                    + "WHERE batch_number = ? AND pharmacy_id = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, number);
                pst.setString(2, pharmacy);

                try (ResultSet rs = pst.executeQuery()) {

                    if (rs.next()) {

                        int medicineId = rs.getInt("medicine_id");

                        for (String item : batchMedicine.getItems()) {

                            if (item.startsWith(medicineId + " - ")) {

                                batchMedicine.setValue(item);
                                break;
                            }
                        }

                        manufacturingDate.setValue(
                                rs.getDate("manufacturing_date")
                                        .toLocalDate());

                        expiryDate.setValue(
                                rs.getDate("expiry_date")
                                        .toLocalDate());

                        quantityReceived.setText(
                                String.valueOf(
                                        rs.getInt("quantity_received")));

                        quantityAvailable.setText(
                                String.valueOf(
                                        rs.getInt("quantity_available")));

                        purchasePrice.setText(
                                String.valueOf(
                                        rs.getBigDecimal("purchase_price")));

                        sellingPrice.setText(
                                String.valueOf(
                                        rs.getBigDecimal("selling_price")));

                        batchStatus.setValue(
                                rs.getString("status"));

                        if (rs.getDate("received_date") != null) {

                            receivedDate.setValue(
                                    rs.getDate("received_date")
                                            .toLocalDate());

                        } else {

                            receivedDate.setValue(null);
                        }

                        System.out.println("Batch Found Successfully");

                    } else {

                        System.out.println("Batch Not Found");
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =====================================================
    //                 UPDATE MEDICINE BATCH
    // =====================================================

    @FXML
    void doUpdateBatch(ActionEvent event) {

        try {

            if (!validateBatchFields()) {
                return;
            }

            String number = batchNumber.getText().trim();
            String pharmacy = pharmacyId.getText().trim();

            int medicineId = getSelectedMedicineId();
            String medicineName = getSelectedMedicineName();

            int received = Integer.parseInt(
                    quantityReceived.getText().trim());

            int available;

            if (quantityAvailable.getText().trim().isEmpty()) {
                available = received;
            } else {
                available = Integer.parseInt(
                        quantityAvailable.getText().trim());
            }

            double purchase = Double.parseDouble(
                    purchasePrice.getText().trim());

            double selling = Double.parseDouble(
                    sellingPrice.getText().trim());

            LocalDate receivedOn = receivedDate.getValue();

            if (receivedOn == null) {
                receivedOn = LocalDate.now();
            }

            String sql = "UPDATE medicine_batch SET "
                    + "medicine_id = ?, "
                    + "manufacturing_date = ?, "
                    + "expiry_date = ?, "
                    + "quantity_received = ?, "
                    + "purchase_price = ?, "
                    + "status = ?, "
                    + "quantity_available = ?, "
                    + "selling_price = ?, "
                    + "received_date = ?, "
                    + "medicine_name = ? "
                    + "WHERE batch_number = ? AND pharmacy_id = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setInt(1, medicineId);

                pst.setDate(2,
                        java.sql.Date.valueOf(
                                manufacturingDate.getValue()));

                pst.setDate(3,
                        java.sql.Date.valueOf(
                                expiryDate.getValue()));

                pst.setInt(4, received);
                pst.setDouble(5, purchase);
                pst.setString(6, batchStatus.getValue());
                pst.setInt(7, available);
                pst.setDouble(8, selling);

                pst.setDate(9,
                        java.sql.Date.valueOf(receivedOn));

                pst.setString(10, medicineName);
                pst.setString(11, number);
                pst.setString(12, pharmacy);

                int count = pst.executeUpdate();

                if (count > 0) {

                    System.out.println("Batch Updated Successfully");

                    loadInventoryTable();

                } else {

                    System.out.println("Batch Not Found");
                }
            }

        } catch (Exception e) {

            System.out.println("Could not update batch");
            e.printStackTrace();
        }
    }


    // =====================================================
    //                 DELETE MEDICINE BATCH
    // =====================================================

    @FXML
    void doDeleteBatch(ActionEvent event) {

        try {

            String number = batchNumber.getText().trim();
            String pharmacy = pharmacyId.getText().trim();

            if (number.isEmpty() || pharmacy.isEmpty()) {

                System.out.println(
                        "Enter batch number and pharmacy ID first");

                return;
            }

            String sql = "DELETE FROM medicine_batch "
                    + "WHERE batch_number = ? AND pharmacy_id = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, number);
                pst.setString(2, pharmacy);

                int count = pst.executeUpdate();

                if (count > 0) {

                    System.out.println("Batch Deleted Successfully");

                    loadInventoryTable();

                    doClearBatch(event);

                } else {

                    System.out.println("Batch Not Found");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =====================================================
    //                 CLEAR MEDICINE BATCH
    // =====================================================

    @FXML
    void doClearBatch(ActionEvent event) {

        batchMedicine.getSelectionModel().clearSelection();

        batchNumber.clear();

        manufacturingDate.setValue(null);
        expiryDate.setValue(null);

        quantityReceived.clear();
        quantityAvailable.clear();

        purchasePrice.clear();
        sellingPrice.clear();

        pharmacyId.setText("PHARM-001");

        batchStatus.setValue("AVAILABLE");

        receivedDate.setValue(LocalDate.now());

        System.out.println("Batch form cleared");
    }


    // =====================================================
    //                    INITIALIZE
    // =====================================================

    @FXML
    void initialize() {

        // Configure the table columns first
        setupInventoryTable();

        con = DBConnection.connectToMySql();

        if (con == null) {

            System.err.println("Database connection is NULL!");
            return;
        }

        // Medicine categories
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

        // Dosage forms
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

        // Batch statuses
        batchStatus.getItems().addAll(
                "AVAILABLE",
                "OUT_OF_STOCK",
                "DAMAGED",
                "QUARANTINED"
        );

        // Default values
        pharmacyId.setText("PHARM-001");

        batchStatus.setValue("AVAILABLE");

        receivedDate.setValue(LocalDate.now());

        // Load medicine dropdown
        loadBatchMedicines();

        // Load inventory table
        loadInventoryTable();


        assert clearBtn != null;
        assert delBtn != null;
        assert fetchBtn != null;
        assert updateBtn != null;
        assert getCategory != null;
        assert getDescription != null;
        assert getDosage != null;
        assert getGenName != null;
        assert getManName != null;
        assert getMedName != null;
        assert getStrength != null;
        assert saveBtn != null;
    }

}
