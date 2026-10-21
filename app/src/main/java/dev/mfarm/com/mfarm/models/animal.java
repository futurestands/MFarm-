package dev.mfarm.com.mfarm.models;

/**
 * Enhanced Animal Model supporting Cattle, Goats, and Sheep
 */
public class animal {
    String id;
    String name;
    String breed_id;
    String gender;
    String body_conf;
    String dob;
    String dam_id;
    String sire_id;
    String species = "Cattle";
    String ear_tag = "";
    String origin_source = "";
    String birth_type = "Single";
    String location_name = "Main Farm";

    public animal() {
    }

    public animal(String id, String name, String breed_id, String gender, String body_conf, String dob, String dam_id, String sire_id) {
        this.id = id;
        this.name = name;
        this.breed_id = breed_id;
        this.gender = gender;
        this.body_conf = body_conf;
        this.dob = dob;
        this.dam_id = dam_id;
        this.sire_id = sire_id;
    }

    public animal(String id, String name, String breed_id, String gender, String body_conf, String dob, String dam_id, String sire_id, String species, String ear_tag, String origin_source, String birth_type, String location_name) {
        this.id = id;
        this.name = name;
        this.breed_id = breed_id;
        this.gender = gender;
        this.body_conf = body_conf;
        this.dob = dob;
        this.dam_id = dam_id;
        this.sire_id = sire_id;
        this.species = species != null ? species : "Cattle";
        this.ear_tag = ear_tag != null ? ear_tag : "";
        this.origin_source = origin_source != null ? origin_source : "";
        this.birth_type = birth_type != null ? birth_type : "Single";
        this.location_name = location_name != null ? location_name : "Main Farm";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed_id() {
        return breed_id;
    }

    public void setBreed_id(String breed_id) {
        this.breed_id = breed_id;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBody_conf() {
        return body_conf;
    }

    public void setBody_conf(String body_conf) {
        this.body_conf = body_conf;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getDam_id() {
        return dam_id;
    }

    public void setDam_id(String dam_id) {
        this.dam_id = dam_id;
    }

    public String getSire_id() {
        return sire_id;
    }

    public void setSire_id(String sire_id) {
        this.sire_id = sire_id;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getEar_tag() {
        return ear_tag;
    }

    public void setEar_tag(String ear_tag) {
        this.ear_tag = ear_tag;
    }

    public String getOrigin_source() {
        return origin_source;
    }

    public void setOrigin_source(String origin_source) {
        this.origin_source = origin_source;
    }

    public String getBirth_type() {
        return birth_type;
    }

    public void setBirth_type(String birth_type) {
        this.birth_type = birth_type;
    }

    public String getLocation_name() {
        return location_name;
    }

    public void setLocation_name(String location_name) {
        this.location_name = location_name;
    }
}
