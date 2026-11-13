package hu.szte.familytree.service;

public class GedcomParseException extends RuntimeException {

    private final int sorSzam;

    public GedcomParseException(String uzenet, int sorSzam) {
        super(sorSzam > 0 ? uzenet + " (" + sorSzam + ". sor)" : uzenet);
        this.sorSzam = sorSzam;
    }

    public GedcomParseException(String uzenet, Throwable ok) {
        super(uzenet, ok);
        this.sorSzam = -1;
    }

    public int getSorSzam() {
        return sorSzam;
    }
}
