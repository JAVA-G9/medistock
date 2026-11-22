package org.example.medistock.model;

/**
 * Medicine entity representing a drug catalog item.
 * Owned by Member 2, referenced by Member 3.
 */
public class Medicine {

    private int medicineId;
    private String medicineName;
    private String genericName;
    private String manufacturerName;
    private String category;
    private String strength;
    private String dosageForm;
    private String description;

    public Medicine() {
    }

    public Medicine(int medicineId, String medicineName, String genericName,
                    String manufacturerName, String category, String strength,
                    String dosageForm, String description) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.genericName = genericName;
        this.manufacturerName = manufacturerName;
        this.category = category;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.description = description;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return medicineName + " (" + strength + ")";
    }
}
