package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.model.Szemely;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Import -> export -> import oda-vissza teszt, adatbázis nélkül
class GedcomRoundTripTest {

    private final GedcomParserService parser = new GedcomParserService();
    private final GrafEpitoService grafEpito = new GrafEpitoService(parser, null);
    private final GedcomExportService exporter = new GedcomExportService();

    private Csaladfa beolvas(InputStream is) {
        return grafEpito.csaladfaEpitese(parser.parseGedcom(is), "teszt", "teszt");
    }

    @Test
    void dummyFajlOdaVissza() {
        Csaladfa eredeti = beolvas(getClass().getResourceAsStream("/dummy.ged"));
        String exportalt = exporter.exportalas(eredeti);
        Csaladfa visszaolvasott = beolvas(new ByteArrayInputStream(exportalt.getBytes(StandardCharsets.UTF_8)));

        assertEquals(eredeti.getSzemelyek().size(), visszaolvasott.getSzemelyek().size());
        assertEquals(eredeti.getCsaladok().size(), visszaolvasott.getCsaladok().size());

        Map<String, Szemely> uj = szerintGedcomId(visszaolvasott.getSzemelyek(), Szemely::getGedcomId);
        for (Szemely e : eredeti.getSzemelyek()) {
            Szemely v = uj.get(e.getGedcomId());
            assertNotNull(v, "Hiányzik exportálás után: " + e.getGedcomId());
            assertEquals(e.getKeresztnev(), v.getKeresztnev());
            assertEquals(e.getVezeteknev(), v.getVezeteknev());
            assertEquals(e.getNem(), v.getNem());
            assertEquals(e.getSzuletesiDatum(), v.getSzuletesiDatum());
            assertEquals(e.getSzuletesiEv(), v.getSzuletesiEv());
            assertEquals(e.getHalalozasiDatum(), v.getHalalozasiDatum());
        }

        Map<String, Csalad> ujCsaladok = szerintGedcomId(visszaolvasott.getCsaladok(), Csalad::getGedcomId);
        for (Csalad e : eredeti.getCsaladok()) {
            Csalad v = ujCsaladok.get(e.getGedcomId());
            assertNotNull(v, "Hiányzik exportálás után: " + e.getGedcomId());
            assertEquals(idk(e.getSzulok()), idk(v.getSzulok()));
            assertEquals(idk(e.getGyermekek()), idk(v.getGyermekek()));
            assertEquals(e.getHazassagDatum(), v.getHazassagDatum());
        }
    }

    @Test
    void grafEpitesNemDuplikalKapcsolatot() {
        Csaladfa fa = beolvas(getClass().getResourceAsStream("/dummy.ged"));
        Map<String, Csalad> csaladok = szerintGedcomId(fa.getCsaladok(), Csalad::getGedcomId);

        // I3 két családban is gyermek (vér szerinti és örökbefogadó), de családonként csak egyszer
        assertEquals(List.of("@I1@", "@I2@"), idk(csaladok.get("@F1@").getSzulok()));
        assertEquals(List.of("@I3@"), idk(csaladok.get("@F1@").getGyermekek()));
        assertEquals(List.of("@I1@"), idk(csaladok.get("@F2@").getSzulok()));
        assertEquals(List.of("@I3@"), idk(csaladok.get("@F2@").getGyermekek()));
    }

    @Test
    void grafEpitesBeallitjaAzEgyezesKulcsot() {
        Csaladfa fa = beolvas(getClass().getResourceAsStream("/dummy.ged"));
        Map<String, Szemely> szemelyek = szerintGedcomId(fa.getSzemelyek(), Szemely::getGedcomId);
        assertEquals("williams|robert eugene|1822", szemelyek.get("@I1@").getEgyezesKulcs());
        assertEquals("wilson|mary ann|1828", szemelyek.get("@I2@").getEgyezesKulcs());
        assertEquals("williams|joe|1861", szemelyek.get("@I3@").getEgyezesKulcs());
    }

    private static List<String> idk(List<Szemely> szemelyek) {
        return szemelyek.stream().map(Szemely::getGedcomId).sorted().toList();
    }

    private static <T> Map<String, T> szerintGedcomId(List<T> lista, Function<T, String> kulcs) {
        return lista.stream().collect(Collectors.toMap(kulcs, Function.identity()));
    }
}
