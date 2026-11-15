package hu.szte.familytree.integracio;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fotók és fotó-taggelés (4. sprint)
class KepCimkezesIT extends Neo4jIntegracioTeszt {

    private long kepFeltoltes(long faId) throws Exception {
        String valasz = mockMvc.perform(multipart("/api/csaladfak/{faId}/kepek", faId)
                        .file(new MockMultipartFile("fajl", "csalad.png", "image/png", pngKep(40, 30))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return szam(valasz, "$.id");
    }

    private ResultActions cimkezes(long kepId, long szemelyId, double x, double y, double szelesseg, double magassag)
            throws Exception {
        return mockMvc.perform(put("/api/kepek/{kepId}/cimkek/{szemelyId}", kepId, szemelyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"x": %s, "y": %s, "szelesseg": %s, "magassag": %s}""".formatted(x, y, szelesseg, magassag)));
    }

    private static String torzs(ResultActions eredmeny) throws Exception {
        return eredmeny.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void feltoltesLetoltesEsTorles() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        byte[] png = pngKep(40, 30);
        String kep = mockMvc.perform(multipart("/api/csaladfak/{faId}/kepek", faId)
                        .file(new MockMultipartFile("fajl", "csalad.png", "image/png", png))
                        .param("leiras", "Családi fotó"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long kepId = szam(kep, "$.id");
        assertEquals(faId, szam(kep, "$.csaladfaId"));
        assertEquals(40, szam(kep, "$.szelesseg"));
        assertEquals(30, szam(kep, "$.magassag"));
        assertEquals("image/png", JsonPath.read(kep, "$.mimeTipus"));
        assertEquals("Családi fotó", JsonPath.read(kep, "$.leiras"));
        assertEquals("csalad.png", JsonPath.read(kep, "$.eredetiNev"));
        assertEquals("/api/kepek/" + kepId + "/fajl", JsonPath.read(kep, "$.url"));
        assertEquals(0, szam(kep, "$.cimkek.length()"));
        assertEquals(1, kepfajlokSzama());

        mockMvc.perform(get("/api/kepek/{id}/fajl", kepId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(png));
        assertEquals(Set.of(kepId), szamok(lekeres("/api/csaladfak/{id}/kepek", faId), "$[*].id"));

        mockMvc.perform(delete("/api/kepek/{id}", kepId)).andExpect(status().isNoContent());
        assertEquals(0, kepfajlokSzama());
        mockMvc.perform(get("/api/kepek/{id}", kepId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.hiba").exists());
        mockMvc.perform(delete("/api/kepek/{id}", kepId)).andExpect(status().isNotFound());
    }

    @Test
    void nemKepEsNemLetezoFaElutasitva() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        mockMvc.perform(multipart("/api/csaladfak/{faId}/kepek", faId)
                        .file(new MockMultipartFile("fajl", "level.txt", "text/plain", "szöveg".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.hiba").exists());
        mockMvc.perform(multipart("/api/csaladfak/{faId}/kepek", 999_999_999L)
                        .file(new MockMultipartFile("fajl", "csalad.png", "image/png", pngKep(4, 4))))
                .andExpect(status().isNotFound());
        assertEquals(0, kepfajlokSzama());
        assertEquals(0, darab("MATCH (k:Kep) RETURN count(k)"));
    }

    @Test
    void cimkezesAthelyezesEsCimkeTorles() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long i1 = szemelyId(faId, "@I1@");
        long i2 = szemelyId(faId, "@I2@");
        long kepId = kepFeltoltes(faId);

        String kep = torzs(cimkezes(kepId, i1, 0.1, 0.1, 0.3, 0.4).andExpect(status().isOk()));
        assertEquals(1, szam(kep, "$.cimkek.length()"));
        assertEquals(i1, szam(kep, "$.cimkek[0].szemelyId"));
        assertEquals("Robert Eugene", JsonPath.read(kep, "$.cimkek[0].keresztnev"));
        assertEquals(0.1, (Double) JsonPath.read(kep, "$.cimkek[0].x"), 1e-9);
        assertEquals(0.4, (Double) JsonPath.read(kep, "$.cimkek[0].magassag"), 1e-9);
        assertEquals(false, JsonPath.read(kep, "$.cimkek[0].profilkep"));

        // Ugyanaz a személy újra: a keret áthelyeződik, nem jön létre második címke
        kep = torzs(cimkezes(kepId, i1, 0.5, 0.5, 0.2, 0.2).andExpect(status().isOk()));
        assertEquals(1, szam(kep, "$.cimkek.length()"));
        assertEquals(0.5, (Double) JsonPath.read(kep, "$.cimkek[0].x"), 1e-9);
        assertEquals(1, darab("MATCH (:Szemely)-[r:SZEREPEL_RAJTA]->(:Kep) RETURN count(r)"));

        kep = torzs(cimkezes(kepId, i2, 0.0, 0.0, 0.2, 0.2).andExpect(status().isOk()));
        assertEquals(Set.of(i1, i2), szamok(kep, "$.cimkek[*].szemelyId"));

        assertEquals(Set.of(kepId), szamok(lekeres("/api/szemelyek/{id}/kepek", i1), "$[*].id"));

        mockMvc.perform(delete("/api/kepek/{kepId}/cimkek/{szemelyId}", kepId, i2)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/kepek/{kepId}/cimkek/{szemelyId}", kepId, i2)).andExpect(status().isNotFound());
        assertEquals(Set.of(i1), szamok(lekeres("/api/kepek/{id}", kepId), "$.cimkek[*].szemelyId"));
        assertEquals(0, szam(lekeres("/api/szemelyek/{id}/kepek", i2), "$.length()"));
    }

    @Test
    void ervenytelenCimkezesek() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long i1 = szemelyId(faId, "@I1@");
        long kepId = kepFeltoltes(faId);

        cimkezes(kepId, i1, 0.8, 0.1, 0.5, 0.2).andExpect(status().isBadRequest()).andExpect(jsonPath("$.hiba").exists());
        cimkezes(kepId, 999_999_999L, 0.1, 0.1, 0.2, 0.2).andExpect(status().isNotFound());
        cimkezes(999_999_999L, i1, 0.1, 0.1, 0.2, 0.2).andExpect(status().isNotFound());

        // Másik családfa személye nem jelölhető meg ezen a képen
        long masikFa = importalas("dummy2.ged", "Másik", "bela");
        long idegen = szemelyId(masikFa, "@I1@");
        cimkezes(kepId, idegen, 0.1, 0.1, 0.2, 0.2).andExpect(status().isBadRequest());

        assertEquals(0, darab("MATCH (:Szemely)-[r:SZEREPEL_RAJTA]->(:Kep) RETURN count(r)"));
    }

    @Test
    void profilkepBeallitasaCsereEsTorles() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long i1 = szemelyId(faId, "@I1@");
        long i3 = szemelyId(faId, "@I3@");
        long kep1 = kepFeltoltes(faId);
        long kep2 = kepFeltoltes(faId);
        cimkezes(kep1, i1, 0.1, 0.2, 0.3, 0.4).andExpect(status().isOk());
        cimkezes(kep2, i1, 0.5, 0.5, 0.25, 0.25).andExpect(status().isOk());

        mockMvc.perform(put("/api/szemelyek/{id}/profilkep", i1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"kepId\": " + kep1 + "}"))
                .andExpect(status().isNoContent());
        String graf = lekeres("/api/csaladfak/{id}/graf", faId);
        assertEquals(1, szam(graf, "$.profilkepek.length()"));
        assertEquals(i1, szam(graf, "$.profilkepek[0].szemelyId"));
        assertEquals(kep1, szam(graf, "$.profilkepek[0].kepId"));
        assertEquals("/api/kepek/" + kep1 + "/fajl", JsonPath.read(graf, "$.profilkepek[0].url"));
        assertEquals(0.2, (Double) JsonPath.read(graf, "$.profilkepek[0].y"), 1e-9);

        // Másik fotóra cserélve csak egy profilkép marad
        mockMvc.perform(put("/api/szemelyek/{id}/profilkep", i1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"kepId\": " + kep2 + "}"))
                .andExpect(status().isNoContent());
        graf = lekeres("/api/csaladfak/{id}/graf", faId);
        assertEquals(1, szam(graf, "$.profilkepek.length()"));
        assertEquals(kep2, szam(graf, "$.profilkepek[0].kepId"));
        assertEquals(1, darab("MATCH (:Szemely)-[r:SZEREPEL_RAJTA {profilkep: true}]->(:Kep) RETURN count(r)"));

        // Aki nincs megjelölve a képen, annak az nem lehet profilképe
        mockMvc.perform(put("/api/szemelyek/{id}/profilkep", i3)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"kepId\": " + kep1 + "}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/szemelyek/{id}/profilkep", i1)).andExpect(status().isNoContent());
        assertEquals(0, szam(lekeres("/api/csaladfak/{id}/graf", faId), "$.profilkepek.length()"));
    }

    @Test
    void faTorleseAKepeketEsFajljaikatIsTorli() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long kepId = kepFeltoltes(faId);
        kepFeltoltes(faId);
        cimkezes(kepId, szemelyId(faId, "@I1@"), 0.1, 0.1, 0.2, 0.2).andExpect(status().isOk());
        assertEquals(2, kepfajlokSzama());

        mockMvc.perform(delete("/api/csaladfak/{id}", faId)).andExpect(status().isNoContent());
        assertEquals(0, kepfajlokSzama());
        assertEquals(0, darab("MATCH (n) RETURN count(n)"));
    }
}
