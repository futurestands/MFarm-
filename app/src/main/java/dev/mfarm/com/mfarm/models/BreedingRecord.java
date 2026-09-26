package dev.mfarm.com.mfarm.models;

public class BreedingRecord {
    private int id;
    private int animalId;
    private String animalName;
    private String matingDate;
    private String bullId;
    private String expectedBirthDate;
    private String status;

    public BreedingRecord() {}

    public BreedingRecord(int id, int animalId, String matingDate, String bullId, String expectedBirthDate, String status) {
        this.id = id;
        this.animalId = animalId;
        this.matingDate = matingDate;
        this.bullId = bullId;
        this.expectedBirthDate = expectedBirthDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAnimalId() { return animalId; }
    public void setAnimalId(int animalId) { this.animalId = animalId; }

    public String getAnimalName() { return animalName; }
    public void setAnimalName(String animalName) { this.animalName = animalName; }

    public String getMatingDate() { return matingDate; }
    public void setMatingDate(String matingDate) { this.matingDate = matingDate; }

    public String getBullId() { return bullId; }
    public void setBullId(String bullId) { this.bullId = bullId; }

    public String getExpectedBirthDate() { return expectedBirthDate; }
    public void setExpectedBirthDate(String expectedBirthDate) { this.expectedBirthDate = expectedBirthDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
