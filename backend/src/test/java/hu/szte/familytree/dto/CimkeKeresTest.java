package hu.szte.familytree.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class CimkeKeresTest {

    private static void hibas(CimkeKeres keres) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, keres::ellenoriz);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void ervenyesKeret() {
        assertDoesNotThrow(() -> new CimkeKeres(0.1, 0.2, 0.3, 0.4).ellenoriz());
        assertDoesNotThrow(() -> new CimkeKeres(0.0, 0.0, 1.0, 1.0).ellenoriz()); // a teljes kép
    }

    @Test
    void lebegopontosKerekitesAKepSzelen() {
        assertDoesNotThrow(() -> new CimkeKeres(0.7, 0.1, 0.3, 0.9).ellenoriz());
    }

    @Test
    void hianyzoVagyNemVegesErtek() {
        hibas(new CimkeKeres(null, 0.1, 0.1, 0.1));
        hibas(new CimkeKeres(Double.NaN, 0.1, 0.1, 0.1));
        hibas(new CimkeKeres(0.1, 0.1, Double.POSITIVE_INFINITY, 0.1));
    }

    @Test
    void negativSarok() {
        hibas(new CimkeKeres(-0.1, 0.1, 0.2, 0.2));
    }

    @Test
    void nullaMeret() {
        hibas(new CimkeKeres(0.1, 0.1, 0.0, 0.2));
    }

    @Test
    void kilogAKepbol() {
        hibas(new CimkeKeres(0.8, 0.1, 0.5, 0.2));
        hibas(new CimkeKeres(0.1, 0.9, 0.2, 0.2));
    }
}
