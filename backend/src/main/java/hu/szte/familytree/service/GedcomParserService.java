package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.Szemely;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class GedcomParserService {

    public Map<String, Szemely> parseGedcom(InputStream inputStream) {
        Map<String, Szemely> szemelyek = new HashMap<>();
        Map<String, Csalad> csaladok = new HashMap<>();

        Szemely aktualisSzemely = null;
        Csalad aktualisCsalad = null;
        String aktualisTagLevel1 = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split(" ", 3);
                if (parts.length < 2) continue;

                String level = parts[0];
                String tagOrId = parts[1];
                String value = parts.length > 2 ? parts[2] : "";

                if (level.equals("0")) {
                    if (value.equals("INDI")) {
                        aktualisSzemely = new Szemely();
                        aktualisSzemely.setGedcomId(tagOrId);
                        szemelyek.put(tagOrId, aktualisSzemely);
                        aktualisCsalad = null;
                    } else if (value.equals("FAM")) {
                        aktualisCsalad = new Csalad(tagOrId);
                        csaladok.put(tagOrId, aktualisCsalad);
                        aktualisSzemely = null;
                    }
                } else if (level.equals("1")) {
                    if (aktualisSzemely != null) {
                        if (tagOrId.equals("NAME")) {
                            String nev = value.replace("/", ""); // Perjelek eltávolítása a vezetéknév körül
                            String[] nevBontas = nev.split(" ", 2);
                            if (nevBontas.length > 0) aktualisSzemely.setKeresztnev(nevBontas[0]);
                            if (nevBontas.length > 1) aktualisSzemely.setVezeteknev(nevBontas[1]);
                            // TODO ugyelni a dupla kereszt vagy csaladnevekre
                        }
                        aktualisTagLevel1 = tagOrId; // BIRT vagy DEAT megjegyzése a kettes szinthez
                    } else if (aktualisCsalad != null) {
                        if (tagOrId.equals("HUSB")) aktualisCsalad.setHusbId(value);
                        else if (tagOrId.equals("WIFE")) aktualisCsalad.setWifeId(value);
                        else if (tagOrId.equals("CHIL")) aktualisCsalad.addChilId(value);
                    }
                } else if (level.equals("2")) {
                    if (aktualisSzemely != null && tagOrId.equals("DATE")) {
                        if (aktualisTagLevel1.equals("BIRT")) {
                            aktualisSzemely.setSzuletesiDatum(value);
                        } else if (aktualisTagLevel1.equals("DEAT")) {
                            aktualisSzemely.setHalalozasiDatum(value);
                        }
                    }
                }
            }
            
            // TODO (3. Sprint): Itt kell majd a 'csaladok' Map alapján összekötni a 'szemelyek'
            // objektumait (pl. szülő-gyermek kapcsolatok) és elmenteni a Neo4j-be.
            
        } catch (Exception e) {
            //TODO rendes sajat GedcomParseException
            System.err.println("Hiba a GEDCOM feldolgozása közben: " + e.getMessage());
        }

        return szemelyek;
    }
}
