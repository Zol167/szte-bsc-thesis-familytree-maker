package hu.szte.familytree.model;

import java.util.Map;

public class GedcomAdatbazis {
    private Map<String, Szemely> szemelyek;
    private Map<String, Csalad> csaladok;

    public GedcomAdatbazis(Map<String, Szemely> szemelyek, Map<String, Csalad> csaladok) {
        this.szemelyek = szemelyek;
        this.csaladok = csaladok;
    }

    public Map<String, Szemely> getSzemelyek() { return szemelyek; }
    public Map<String, Csalad> getCsaladok() { return csaladok; }
}
