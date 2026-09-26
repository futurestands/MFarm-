package dev.mfarm.com.mfarm.models;

/**
 * Created by SIMGICH on 4/20/2016.
 */
public class milkproduction {
    String id;

    public String getA_name() {
        return a_name;
    }

    public void setA_name(String a_name) {
        this.a_name = a_name;
    }

    String a_name;

    public milkproduction() {
    }

    String animal_id;

    public milkproduction(String id, String animal_id, String litres, String description, String datetime, String a_name) {
        this.id = id;
        this.animal_id = animal_id;
        this.litres = litres;
        this.description = description;
        this.datetime = datetime;
        this.a_name = a_name;
    }

    String litres;

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

    public String getLitres() {
        return litres;
    }

    public void setLitres(String litres) {
        this.litres = litres;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    String description;
    String datetime;
}
