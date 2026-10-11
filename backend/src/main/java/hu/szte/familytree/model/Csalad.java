package hu.szte.familytree.model;

import java.util.ArrayList;
import java.util.List;

// A 2. sprintben ez még csak egy memóriabeli DTO/modell a parser számára
public class Csalad {
    private String gedcomId;
    private String husbId;
    private String wifeId;
    private List<String> chilIds = new ArrayList<>();

    public Csalad(String gedcomId) {
        this.gedcomId = gedcomId;
    }

    public String getGedcomId() {
        return gedcomId;
    }

    public void setGedcomId(String gedcomId) {
        this.gedcomId = gedcomId;
    }

    public String getHusbId() {
        return husbId;
    }

    public void setHusbId(String husbId) {
        this.husbId = husbId;
    }

    public String getWifeId() {
        return wifeId;
    }

    public void setWifeId(String wifeId) {
        this.wifeId = wifeId;
    }

    public List<String> getChilIds() {
        return chilIds;
    }

    public void setChilIds(List<String> chilIds) {
        this.chilIds = chilIds;
    }

    public void addChilId(String chilId) {
        this.chilIds.add(chilId);
    }
}