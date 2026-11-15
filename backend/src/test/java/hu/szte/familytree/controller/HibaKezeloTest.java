package hu.szte.familytree.controller;

import hu.szte.familytree.service.GedcomParseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HibaKezeloTest {

    private final HibaKezelo kezelo = new HibaKezelo();

    @Test
    void tulNagyFeltoltes413() {
        ResponseEntity<Map<String, String>> v = kezelo.tulNagyFajl(new MaxUploadSizeExceededException(1024));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, v.getStatusCode());
        assertNotNull(v.getBody().get("hiba"));
    }

    @Test
    void hibasGedcom400ASorszammal() {
        ResponseEntity<Map<String, String>> v = kezelo.hibasGedcom(new GedcomParseException("Érvénytelen szintszám", 7));
        assertEquals(HttpStatus.BAD_REQUEST, v.getStatusCode());
        assertTrue(v.getBody().get("hiba").contains("7. sor"));
    }

    @Test
    void statuszHibaAzEredetiStatusszalEsUzenettel() {
        ResponseEntity<Map<String, String>> v = kezelo.statuszHiba(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Nincs ilyen kép: 5"));
        assertEquals(HttpStatus.NOT_FOUND, v.getStatusCode());
        assertEquals("Nincs ilyen kép: 5", v.getBody().get("hiba"));
    }

    @Test
    void statuszHibaUzenetNelkul() {
        ResponseEntity<Map<String, String>> v = kezelo.statuszHiba(new ResponseStatusException(HttpStatus.CONFLICT));
        assertEquals(HttpStatus.CONFLICT, v.getStatusCode());
        assertNotNull(v.getBody().get("hiba"));
    }
}
