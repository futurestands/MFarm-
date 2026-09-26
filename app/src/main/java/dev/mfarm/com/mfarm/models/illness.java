package dev.mfarm.com.mfarm.models;

/**
 * Created by SIMGICH on 16/01/2017.
 */
public class illness {
    public illness() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnimal_id() {
        return animal_id;
    }

    public void setAnimal_id(String animal_id) {
        this.animal_id = animal_id;
    }

    public String getAnimal_name() {
        return animal_name;
    }

    public void setAnimal_name(String animal_name) {
        this.animal_name = animal_name;
    }

    public String getIllness_occured() {
        return illness_occured;
    }

    public void setIllness_occured(String illness_occured) {
        this.illness_occured = illness_occured;
    }

    public String getSings_noted() {
        return sings_noted;
    }

    public void setSings_noted(String sings_noted) {
        this.sings_noted = sings_noted;
    }

    public String getDate_occured() {
        return date_occured;
    }

    public void setDate_occured(String date_occured) {
        this.date_occured = date_occured;
    }

    public String getSync_datetime() {
        return sync_datetime;
    }

    public void setSync_datetime(String sync_datetime) {
        this.sync_datetime = sync_datetime;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getMedicine() {
        return medicine;
    }

    public void setMedicine(String medicine) {
        this.medicine = medicine;
    }

    public String getTreatment_date() {
        return treatment_date;
    }

    public void setTreatment_date(String treatment_date) {
        this.treatment_date = treatment_date;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    public String getMedicine_quantity() {
        return medicine_quantity;
    }

    public void setMedicine_quantity(String medicine_quantity) {
        this.medicine_quantity = medicine_quantity;
    }

    public String getPregnancy_status() {
        return pregnancy_status;
    }

    public void setPregnancy_status(String pregnancy_status) {
        this.pregnancy_status = pregnancy_status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    String id;

    public illness(String id, String animal_id, String animal_name, String illness_occured, String sings_noted, String date_occured, String sync_datetime, String treatment, String diagnosis, String medicine, String treatment_date, String others, String medicine_quantity, String pregnancy_status, String comments) {
        this.id = id;
        this.animal_id = animal_id;
        this.animal_name = animal_name;
        this.illness_occured = illness_occured;
        this.sings_noted = sings_noted;
        this.date_occured = date_occured;
        this.sync_datetime = sync_datetime;
        this.treatment = treatment;
        this.diagnosis = diagnosis;
        this.medicine = medicine;
        this.treatment_date = treatment_date;
        this.others = others;
        this.medicine_quantity = medicine_quantity;
        this.pregnancy_status = pregnancy_status;
        this.comments = comments;
    }

    String animal_id;
    String animal_name;
    String illness_occured;
    String sings_noted;
    String date_occured;
    String sync_datetime;
    String treatment;
    String diagnosis;
    String medicine;
    String treatment_date;
    String others;
    String medicine_quantity;
    String pregnancy_status;
    String comments;
}
