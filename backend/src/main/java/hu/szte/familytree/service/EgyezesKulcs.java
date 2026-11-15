package hu.szte.familytree.service;

import hu.szte.familytree.model.Szemely;

import java.text.Normalizer;
import java.util.Locale;

// Az ütközés-detektálás egzakt egyezési kulcsa: "vezetéknév|keresztnév|születési év".
// Normalizálás: Unicode NFC (az "á" lehet egy vagy két kódpont), szélső szóközök levágása, belső
// szóközök összevonása, kisbetűsítés. Az ékezetek megmaradnak, így "Kovács" és "Kovacs" nem egyezik:
// ez szándékosan pontos egyezés, a hasonlóság-alapú keresés későbbi továbbfejlesztés.
public final class EgyezesKulcs {

    private EgyezesKulcs() {
    }

    public static String keszit(Szemely szemely) {
        return keszit(szemely.getVezeteknev(), szemely.getKeresztnev(), szemely.getSzuletesiEv());
    }

    // null, ha bármelyik rész hiányzik: hiányos adatú személyre nem jelzünk egyezést
    public static String keszit(String vezeteknev, String keresztnev, Integer szuletesiEv) {
        String vezetek = normalizalas(vezeteknev);
        String kereszt = normalizalas(keresztnev);
        if (vezetek == null || kereszt == null || szuletesiEv == null) {
            return null;
        }
        return vezetek + "|" + kereszt + "|" + szuletesiEv;
    }

    static String normalizalas(String nev) {
        if (nev == null) return null;
        String n = Normalizer.normalize(nev, Normalizer.Form.NFC)
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return n.isEmpty() ? null : n;
    }
}
