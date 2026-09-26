package dev.mfarm.com.mfarm.models;

/**
 * Created by SIMGICH on 11/08/2016.
 */
public class Breeds {

    int id;

    public Breeds(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Breeds() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    String name;
}
