package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.GedcomAdatbazis;
import hu.szte.familytree.model.Szemely;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GedcomParserServiceTest {

    private final GedcomParserService parser = new GedcomParserService();

    private GedcomAdatbazis dummy() {
        InputStream is = getClass().getResourceAsStream("/dummy.ged");
        assertNotNull(is, "dummy.ged nem található a classpath-on");
        return parser.parseGedcom(is);
    }

    private GedcomAdatbazis szovegbol(String gedcom) {
        return parser.parseGedcom(new ByteArrayInputStream(gedcom.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void dummyFajlSzemelyekEsCsaladok() {
        GedcomAdatbazis adat = dummy();
        assertEquals(3, adat.getSzemelyek().size());
        assertEquals(2, adat.getCsaladok().size());
    }

    @Test
    void nevVezeteknevPerjelekKozott() {
        Szemely robert = dummy().getSzemelyek().get("@I1@");
        assertEquals("Robert Eugene", robert.getKeresztnev());
        assertEquals("Williams", robert.getVezeteknev());
        assertEquals("M", robert.getNem());
        assertEquals("2 Oct 1822", robert.getSzuletesiDatum());
        assertEquals(1822, robert.getSzuletesiEv());
        assertEquals("14 Apr 1905", robert.getHalalozasiDatum());
    }

    @Test
    void bizonytalanDatumbolIsKinyeriAzEvet() {
        Szemely mary = dummy().getSzemelyek().get("@I2@");
        assertEquals("BEF 1828", mary.getSzuletesiDatum());
        assertEquals(1828, mary.getSzuletesiEv());
    }

    @Test
    void csaladHivatkozasokEsHazassagDatum() {
        GedcomAdatbazis adat = dummy();
        Csalad f1 = adat.getCsaladok().get("@F1@");
        assertEquals("@I1@", f1.getHusbId());
        assertEquals("@I2@", f1.getWifeId());
        assertEquals(List.of("@I3@"), f1.getChilIds());
        assertEquals("Dec 1859", f1.getHazassagDatum());

        Csalad f2 = adat.getCsaladok().get("@F2@");
        assertEquals("@I1@", f2.getHusbId());
        assertNull(f2.getWifeId());
    }

    @Test
    void masRekordokNemIrjakFelulAzElozoSzemelyt() {
        GedcomAdatbazis adat = szovegbol("""
                0 HEAD
                0 @I1@ INDI
                1 NAME Anna /Nagy/
                0 @R1@ REPO
                1 NAME Családi Levéltár
                0 @S1@ SOUR
                1 DATA
                2 DATE 1900
                0 TRLR
                """);
        Szemely anna = adat.getSzemelyek().get("@I1@");
        assertEquals("Anna", anna.getKeresztnev());
        assertEquals("Nagy", anna.getVezeteknev());
        assertNull(anna.getSzuletesiDatum());
    }

    @Test
    void magyarNevsorrendEsEkezetek() {
        GedcomAdatbazis adat = szovegbol("""
                0 @I1@ INDI
                1 NAME /Kovács/ János Ödön
                1 BIRT
                2 DATE 1901
                0 TRLR
                """);
        Szemely janos = adat.getSzemelyek().get("@I1@");
        assertEquals("János Ödön", janos.getKeresztnev());
        assertEquals("Kovács", janos.getVezeteknev());
        assertEquals(1901, janos.getSzuletesiEv());
    }

    @Test
    void utf8BomAFajlElejen() {
        GedcomAdatbazis adat = szovegbol("\uFEFF0 @I1@ INDI\n1 NAME Éva /Szabó/\n0 TRLR\n");
        assertEquals("Szabó", adat.getSzemelyek().get("@I1@").getVezeteknev());
    }

    @Test
    void duplikaltGyermekHivatkozasCsakEgyszer() {
        GedcomAdatbazis adat = szovegbol("""
                0 @I1@ INDI
                0 @F1@ FAM
                1 CHIL @I1@
                1 CHIL @I1@
                0 TRLR
                """);
        assertEquals(List.of("@I1@"), adat.getCsaladok().get("@F1@").getChilIds());
    }

    @Test
    void hibasSzintszamKivetelSorszammal() {
        GedcomParseException e = assertThrows(GedcomParseException.class, () -> szovegbol("""
                0 @I1@ INDI
                X NAME Hibás /Sor/
                """));
        assertEquals(2, e.getSorSzam());
    }

    @Test
    void evKinyerese() {
        assertEquals(1822, GedcomParserService.evKinyerese("2 Oct 1822"));
        assertEquals(1820, GedcomParserService.evKinyerese("FROM Jan 1820 TO DEC 1825"));
        assertEquals(987, GedcomParserService.evKinyerese("ABT 987"));
        assertNull(GedcomParserService.evKinyerese("unknown"));
        assertNull(GedcomParserService.evKinyerese(null));
    }
}
