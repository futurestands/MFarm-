package dev.mfarm.com.mfarm.models;

/**
 * Created by SIMGICH on 4/20/2016.
 */
public class animal {
    String id;

    public animal() {
    }

    String name;
    String breed_id;
    String gender;
    String body_conf;
    String dob;
    String dam_id;
    String sire_id;

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


}
