package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.GedcomAdatbazis;
import hu.szte.familytree.model.Szemely;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GedcomParserService {

    private static final Pattern EV_MINTA = Pattern.compile("\\b(\\d{3,4})\\b");

    public GedcomAdatbazis parseGedcom(InputStream inputStream) {
        // LinkedHashMap: megtartja a fájlbeli sorrendet (exportnál és tesztelésnél hasznos)
        Map<String, Szemely> szemelyek = new LinkedHashMap<>();
        Map<String, Csalad> csaladok = new LinkedHashMap<>();

        Szemely aktualisSzemely = null;
        Csalad aktualisCsalad = null;
        String aktualisTagLevel1 = "";
        int sorSzam = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sorSzam++;
                if (sorSzam == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // UTF-8 BOM eltávolítása
                }
                if (line.isBlank()) continue;

                String[] parts = line.trim().split(" ", 3);
                if (parts.length < 2) {
                    throw new GedcomParseException("Hiányos GEDCOM sor: '" + line + "'", sorSzam);
                }

                int level;
                try {
                    level = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    throw new GedcomParseException("Érvénytelen szintszám: '" + parts[0] + "'", sorSzam);
                }
                String tagOrId = parts[1];
                String value = parts.length > 2 ? parts[2] : "";

                if (level == 0) {
                    // Minden új 0-s rekord lezárja az előzőt; a nem kezelt rekordok (SOUR, REPO, SUBM, NOTE...)
                    // alsóbb szintű sorai így nem írhatják felül az előző személy adatait.
                    aktualisSzemely = null;
                    aktualisCsalad = null;
                    aktualisTagLevel1 = "";

                    if (value.equals("INDI")) {
                        aktualisSzemely = new Szemely();
                        aktualisSzemely.setGedcomId(tagOrId);
                        szemelyek.put(tagOrId, aktualisSzemely);
                    } else if (value.equals("FAM")) {
                        aktualisCsalad = new Csalad(tagOrId);
                        csaladok.put(tagOrId, aktualisCsalad);
                    }
                } else if (level == 1) {
                    aktualisTagLevel1 = tagOrId; // BIRT, DEAT vagy MARR megjegyzése a kettes szinthez
                    if (aktualisSzemely != null) {
                        if (tagOrId.equals("NAME") && aktualisSzemely.getKeresztnev() == null
                                && aktualisSzemely.getVezeteknev() == null) {
                            String[] nev = nevFelbontasa(value);
                            aktualisSzemely.setKeresztnev(nev[0]);
                            aktualisSzemely.setVezeteknev(nev[1]);
                        } else if (tagOrId.equals("SEX")) {
                            aktualisSzemely.setNem(value.trim());
                        }
                    } else if (aktualisCsalad != null) {
                        if (tagOrId.equals("HUSB")) aktualisCsalad.setHusbId(value);
                        else if (tagOrId.equals("WIFE")) aktualisCsalad.setWifeId(value);
                        else if (tagOrId.equals("CHIL")) aktualisCsalad.addChilId(value);
                    }
                } else if (level == 2 && tagOrId.equals("DATE")) {
                    if (aktualisSzemely != null) {
                        if (aktualisTagLevel1.equals("BIRT")) {
                            aktualisSzemely.setSzuletesiDatum(value);
                            aktualisSzemely.setSzuletesiEv(evKinyerese(value));
                        } else if (aktualisTagLevel1.equals("DEAT")) {
                            aktualisSzemely.setHalalozasiDatum(value);
                        }
                    } else if (aktualisCsalad != null && aktualisTagLevel1.equals("MARR")) {
                        aktualisCsalad.setHazassagDatum(value);
                    }
                }
            }
        } catch (IOException e) {
            throw new GedcomParseException("A GEDCOM fájl nem olvasható", e);
        }

        return new GedcomAdatbazis(szemelyek, csaladok);
    }

    // GEDCOM névformátum: a vezetéknév perjelek között áll, bárhol a névben.
    // "Robert Eugene /Williams/" -> [Robert Eugene, Williams]; "/Kovács/ János" -> [János, Kovács]
    static String[] nevFelbontasa(String nev) {
        int elso = nev.indexOf('/');
        int masodik = elso >= 0 ? nev.indexOf('/', elso + 1) : -1;
        if (elso < 0) {
            return new String[]{uresbolNull(nev), null};
        }
        if (masodik < 0) {
            masodik = nev.length(); // lezáratlan perjel: a vezetéknév a sor végéig tart
        }
        String vezeteknev = nev.substring(elso + 1, masodik);
        String keresztnev = nev.substring(0, elso) + " " + (masodik < nev.length() ? nev.substring(masodik + 1) : "");
        return new String[]{uresbolNull(keresztnev.replaceAll("\\s+", " ")), uresbolNull(vezeteknev)};
    }

    // Az első 3-4 jegyű szám a dátumban: "BEF 1828" -> 1828, "2 Oct 1822" -> 1822
    static Integer evKinyerese(String datum) {
        if (datum == null) return null;
        Matcher m = EV_MINTA.matcher(datum);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String uresbolNull(String s) {
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
