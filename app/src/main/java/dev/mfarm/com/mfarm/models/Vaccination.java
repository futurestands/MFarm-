package dev.mfarm.com.mfarm.models;

public class Vaccination {
    private int id;
    private int animalId;
    private String animalName; // For display purposes
    private String vaccineName;
    private String scheduledDate;
    private String status;
    private String remarks;

    public Vaccination() {
    }

    public Vaccination(int id, int animalId, String vaccineName, String scheduledDate, String status, String remarks) {
        this.id = id;
        this.animalId = animalId;
        this.vaccineName = vaccineName;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.remarks = remarks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnimalId() {
        return animalId;
    }

    public void setAnimalId(int animalId) {
        this.animalId = animalId;
    }

    public String getAnimalName() {
        return animalName;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(String scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
