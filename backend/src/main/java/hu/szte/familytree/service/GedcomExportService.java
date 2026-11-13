package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.model.Szemely;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// A parser párja: ugyanazt a fókuszált tag-készletet írja ki (INDI, FAM, NAME, SEX, BIRT, DEAT,
// HUSB, WIFE, CHIL, MARR), így az import -> export -> import oda-vissza tesztelhető.
@Service
public class GedcomExportService {

    public String exportalas(Csaladfa fa) {
        StringBuilder sb = new StringBuilder();
        sor(sb, 0, "HEAD", null);
        sor(sb, 1, "GEDC", null);
        sor(sb, 2, "VERS", "5.5.1");
        sor(sb, 2, "FORM", "LINEAGE-LINKED");
        sor(sb, 1, "CHAR", "UTF-8");
        sor(sb, 1, "SOUR", "FamilyTreeMaker");

        Map<Szemely, String> szemelyIdk = azonositokKiosztasa(fa);

        for (Szemely sz : fa.getSzemelyek()) {
            sor(sb, 0, szemelyIdk.get(sz), "INDI");
            String nev = gedcomNev(sz);
            if (nev != null) sor(sb, 1, "NAME", nev);
            if (sz.getNem() != null) sor(sb, 1, "SEX", sz.getNem());
            if (sz.getSzuletesiDatum() != null) {
                sor(sb, 1, "BIRT", null);
                sor(sb, 2, "DATE", sz.getSzuletesiDatum());
            }
            if (sz.getHalalozasiDatum() != null) {
                sor(sb, 1, "DEAT", null);
                sor(sb, 2, "DATE", sz.getHalalozasiDatum());
            }
        }

        Set<String> foglaltCsaladIdk = new HashSet<>();
        int csaladSzamlalo = 1;
        for (Csalad csalad : fa.getCsaladok()) {
            String id = csalad.getGedcomId();
            while (id == null || !foglaltCsaladIdk.add(id)) {
                id = "@F" + csaladSzamlalo++ + "@";
            }
            sor(sb, 0, id, "FAM");

            // HUSB/WIFE a szülő neme alapján; ismeretlen nemnél a szabad helyre kerül
            Szemely ferj = null, feleseg = null;
            for (Szemely szulo : csalad.getSzulok()) {
                if ("M".equals(szulo.getNem()) && ferj == null) ferj = szulo;
                else if ("F".equals(szulo.getNem()) && feleseg == null) feleseg = szulo;
            }
            for (Szemely szulo : csalad.getSzulok()) {
                if (szulo == ferj || szulo == feleseg) continue;
                if (ferj == null) ferj = szulo;
                else if (feleseg == null) feleseg = szulo;
            }
            if (ferj != null) sor(sb, 1, "HUSB", szemelyIdk.get(ferj));
            if (feleseg != null) sor(sb, 1, "WIFE", szemelyIdk.get(feleseg));
            for (Szemely gyerek : csalad.getGyermekek()) {
                sor(sb, 1, "CHIL", szemelyIdk.get(gyerek));
            }
            if (csalad.getHazassagDatum() != null) {
                sor(sb, 1, "MARR", null);
                sor(sb, 2, "DATE", csalad.getHazassagDatum());
            }
        }

        sor(sb, 0, "TRLR", null);
        return sb.toString();
    }

    // Az eredeti GEDCOM azonosítót tartjuk meg; ha hiányzik vagy ütközik, újat generálunk
    private static Map<Szemely, String> azonositokKiosztasa(Csaladfa fa) {
        Map<Szemely, String> idk = new HashMap<>();
        Set<String> foglalt = new HashSet<>();
        int szamlalo = 1;
        for (Szemely sz : fa.getSzemelyek()) {
            String id = sz.getGedcomId();
            while (id == null || !foglalt.add(id)) {
                id = "@I" + szamlalo++ + "@";
            }
            idk.put(sz, id);
        }
        return idk;
    }

    private static String gedcomNev(Szemely sz) {
        String kereszt = sz.getKeresztnev();
        String vezetek = sz.getVezeteknev();
        if (kereszt == null && vezetek == null) return null;
        if (vezetek == null) return kereszt;
        return (kereszt != null ? kereszt + " " : "") + "/" + vezetek + "/";
    }

    private static void sor(StringBuilder sb, int szint, String tag, String ertek) {
        sb.append(szint).append(' ').append(tag);
        if (ertek != null && !ertek.isEmpty()) sb.append(' ').append(ertek);
        sb.append('\n');
    }
}
