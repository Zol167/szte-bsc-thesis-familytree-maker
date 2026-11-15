package hu.szte.familytree.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Címke (keret) létrehozása vagy áthelyezése; relatív koordináták: (0,0) a kép bal felső sarka, (1,1) a jobb alsó
public record CimkeKeres(Double x, Double y, Double szelesseg, Double magassag) {

    // Lebegőpontos kerekítés (pl. 0.7 + 0.3) miatt a kép széle egy hajszálnyit túlléphető
    private static final double TURES = 1e-9;

    public void ellenoriz() {
        for (Double ertek : new Double[]{x, y, szelesseg, magassag}) {
            if (ertek == null || !Double.isFinite(ertek)) {
                hiba("A keret minden értéke (x, y, szelesseg, magassag) kötelező, véges szám");
            }
        }
        if (x < 0 || y < 0) {
            hiba("A keret bal felső sarka nem lehet a képen kívül");
        }
        if (szelesseg <= 0 || magassag <= 0) {
            hiba("A keret szélessége és magassága pozitív kell legyen");
        }
        if (x + szelesseg > 1 + TURES || y + magassag > 1 + TURES) {
            hiba("A keret nem lóghat ki a képből (a koordináták a kép méretéhez viszonyítva 0 és 1 közöttiek)");
        }
    }

    private static void hiba(String uzenet) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, uzenet);
    }
}
