package hu.szte.familytree.service;

import hu.szte.familytree.model.Szemely;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.*;

class EgyezesKulcsTest {

    @Test
    void alapKulcs() {
        assertEquals("williams|robert eugene|1822", EgyezesKulcs.keszit("Williams", "Robert Eugene", 1822));
    }

    @Test
    void kisNagybetuEsSzokozokNemSzamitanak() {
        assertEquals(EgyezesKulcs.keszit("Williams", "Robert Eugene", 1822),
                EgyezesKulcs.keszit("  WILLIAMS ", "Robert   Eugene", 1822));
    }

    @Test
    void azonosEkezetKetFeleKodolasaEgyezik() {
        // "á" egy kódpontként (NFC) vagy "a" + kombinált ékezetként (NFD, pl. macOS-ről exportált fájlban)
        String nfc = Normalizer.normalize("Kovács", Normalizer.Form.NFC);
        String nfd = Normalizer.normalize("Kovács", Normalizer.Form.NFD);
        assertNotEquals(nfc, nfd);
        assertEquals(EgyezesKulcs.keszit(nfc, "János", 1901), EgyezesKulcs.keszit(nfd, "János", 1901));
    }

    @Test
    void azEkezetMegmaradPontosEgyezesMarad() {
        assertNotEquals(EgyezesKulcs.keszit("Kovács", "János", 1901), EgyezesKulcs.keszit("Kovacs", "Janos", 1901));
    }

    @Test
    void hianyosAdatnalNincsKulcs() {
        assertNull(EgyezesKulcs.keszit(null, "János", 1901));
        assertNull(EgyezesKulcs.keszit("Kovács", "   ", 1901));
        assertNull(EgyezesKulcs.keszit("Kovács", "János", null));
    }

    @Test
    void szemelyMezoibolKeszul() {
        Szemely sz = new Szemely();
        sz.setVezeteknev("Wilson");
        sz.setKeresztnev("Mary Ann");
        sz.setSzuletesiEv(1828);
        assertEquals("wilson|mary ann|1828", EgyezesKulcs.keszit(sz));
    }
}
