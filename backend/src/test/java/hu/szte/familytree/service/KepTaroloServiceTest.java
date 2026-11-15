package hu.szte.familytree.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KepTaroloServiceTest {

    @TempDir
    Path ideiglenes;

    private Path konyvtar;
    private KepTaroloService tarolo;

    @BeforeEach
    void init() throws IOException {
        konyvtar = ideiglenes.resolve("kepek");
        tarolo = new KepTaroloService(konyvtar.toString());
    }

    static byte[] kep(String formatum, int szelesseg, int magassag) throws IOException {
        BufferedImage kep = new BufferedImage(szelesseg, magassag, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(kep, formatum, out));
        return out.toByteArray();
    }

    @Test
    void pngMenteseEsMeretOlvasasa() throws IOException {
        KepTaroloService.TaroltKep t = tarolo.mentes(
                new MockMultipartFile("fajl", "csalad.png", "image/png", kep("png", 40, 30)));
        assertEquals("image/png", t.mimeTipus());
        assertEquals(40, t.szelesseg());
        assertEquals(30, t.magassag());
        assertTrue(t.fajlNev().endsWith(".png"));
        assertTrue(Files.isRegularFile(konyvtar.resolve(t.fajlNev())));
    }

    @Test
    void aValodiFormatumDontNemABejelentett() throws IOException {
        // A kliens PNG-nek mondja, de a tartalom JPEG: JPEG-ként tároljuk
        KepTaroloService.TaroltKep t = tarolo.mentes(
                new MockMultipartFile("fajl", "kep.png", "image/png", kep("jpg", 20, 10)));
        assertEquals("image/jpeg", t.mimeTipus());
        assertTrue(t.fajlNev().endsWith(".jpg"));
    }

    @Test
    void nemKepetElutasit() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> tarolo.mentes(
                new MockMultipartFile("fajl", "kep.png", "image/png", "nem kép".getBytes(StandardCharsets.UTF_8))));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getStatusCode());
    }

    @Test
    void nemTamogatottFormatumotElutasit() throws IOException {
        // BMP-t a JDK felismer, de nem engedjük
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> tarolo.mentes(
                new MockMultipartFile("fajl", "kep.bmp", "image/bmp", kep("bmp", 5, 5))));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getStatusCode());
    }

    @Test
    void webpAFejlecAlapjanMeretNelkul() {
        // A JDK nem olvas WebP-t: a bejelentett típus alapján fogadjuk el, méret nélkül
        KepTaroloService.TaroltKep t = tarolo.mentes(
                new MockMultipartFile("fajl", "kep.webp", "image/webp", new byte[]{1, 2, 3}));
        assertEquals("image/webp", t.mimeTipus());
        assertNull(t.szelesseg());
        assertTrue(t.fajlNev().endsWith(".webp"));
    }

    @Test
    void uresFajlotElutasit() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> tarolo.mentes(
                new MockMultipartFile("fajl", "ures.png", "image/png", new byte[0])));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void betoltesEsTorles() throws IOException {
        byte[] tartalom = kep("png", 3, 3);
        KepTaroloService.TaroltKep t = tarolo.mentes(new MockMultipartFile("fajl", "a.png", "image/png", tartalom));
        Resource betoltott = tarolo.betoltes(t.fajlNev());
        assertArrayEquals(tartalom, betoltott.getContentAsByteArray());

        tarolo.torles(t.fajlNev());
        assertFalse(Files.exists(konyvtar.resolve(t.fajlNev())));
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> tarolo.betoltes(t.fajlNev()));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void konyvtaronKivuliFajlNemErhetoEl() {
        assertThrows(IllegalArgumentException.class, () -> tarolo.betoltes("../titok.txt"));
        assertThrows(IllegalArgumentException.class, () -> tarolo.betoltes("alkonyvtar/kep.png"));
    }
}
