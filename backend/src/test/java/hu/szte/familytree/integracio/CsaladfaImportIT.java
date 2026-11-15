package hu.szte.familytree.integracio;

import com.jayway.jsonpath.JsonPath;
import hu.szte.familytree.model.GedcomAdatbazis;
import hu.szte.familytree.service.GedcomParserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A 3. sprint (import, gráfépítés, REST) ellenőrzése valódi Neo4j-jel
class CsaladfaImportIT extends Neo4jIntegracioTeszt {

    @Test
    void importEsGrafSzerkezet() throws Exception {
        String valasz = importValasz("dummy.ged", "Teszt", "anna");
        long faId = szam(valasz, "$.id");
        assertEquals(3, szam(valasz, "$.szemelyDb"));
        assertEquals(2, szam(valasz, "$.csaladDb"));
        assertEquals(0, szam(valasz, "$.egyezesDb"));

        long i1 = szemelyId(faId, "@I1@");
        long i2 = szemelyId(faId, "@I2@");
        long i3 = szemelyId(faId, "@I3@");
        String graf = lekeres("/api/csaladfak/{id}/graf", faId);
        assertEquals(3, szam(graf, "$.szemelyek.length()"));
        assertEquals(Set.of(i1, i2), szamok(graf, "$.csaladok[?(@.gedcomId == '@F1@')].szuloIdk[*]"));
        assertEquals(Set.of(i3), szamok(graf, "$.csaladok[?(@.gedcomId == '@F1@')].gyermekIdk[*]"));
        assertEquals(Set.of(i1), szamok(graf, "$.csaladok[?(@.gedcomId == '@F2@')].szuloIdk[*]"));
        assertEquals(Set.of(i3), szamok(graf, "$.csaladok[?(@.gedcomId == '@F2@')].gyermekIdk[*]"));
        assertEquals(0, szam(graf, "$.profilkepek.length()"));
    }

    @Test
    void nincsDuplikaltEl() throws Exception {
        importalas("dummy.ged", "Teszt", "anna");
        assertEquals(0, darab("MATCH (a)-[r]->(b) WITH a, b, type(r) AS t, count(*) AS c WHERE c > 1 RETURN count(*)"));
    }

    @Test
    void azEgyezesKulcsMentesreKerul() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long i1 = szemelyId(faId, "@I1@");
        String kulcs = neo4jClient.query("MATCH (sz:Szemely) WHERE id(sz) = $id RETURN sz.egyezesKulcs")
                .bind(i1).to("id").fetchAs(String.class).one().orElseThrow();
        assertEquals("williams|robert eugene|1822", kulcs);
    }

    @Test
    void exportAzAdatbazisbolVisszaolvashatoEsUjraImportalhato() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        String ged = mockMvc.perform(get("/api/csaladfak/{id}/export", faId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("csaladfa-" + faId + ".ged")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        GedcomAdatbazis adat = new GedcomParserService()
                .parseGedcom(new ByteArrayInputStream(ged.getBytes(StandardCharsets.UTF_8)));
        assertEquals(3, adat.getSzemelyek().size());
        assertEquals(2, adat.getCsaladok().size());

        String ujra = mockMvc.perform(multipart("/api/csaladfak")
                        .file(new MockMultipartFile("fajl", "export.ged", "text/plain", ged.getBytes(StandardCharsets.UTF_8)))
                        .param("nev", "Visszaimportált")
                        .param("tulajdonos", "anna"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(3, szam(ujra, "$.szemelyDb"));
        assertEquals(2, szam(ujra, "$.csaladDb"));
    }

    @Test
    void osokLeszarmazottakEsNevkereses() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        long i1 = szemelyId(faId, "@I1@");
        long i2 = szemelyId(faId, "@I2@");
        long i3 = szemelyId(faId, "@I3@");
        assertEquals(Set.of(i1, i2), szamok(lekeres("/api/szemelyek/{id}/osok", i3), "$[*].id"));
        assertEquals(Set.of(i3), szamok(lekeres("/api/szemelyek/{id}/leszarmazottak", i1), "$[*].id"));
        assertEquals(Set.of(i1, i3), szamok(lekeres("/api/szemelyek/search?nev=williams"), "$[*].id"));
    }

    @Test
    void listazasEsTorles() throws Exception {
        long faId = importalas("dummy.ged", "Teszt", "anna");
        String lista = lekeres("/api/csaladfak");
        assertEquals(1, szam(lista, "$.length()"));
        assertEquals(3, szam(lista, "$[0].szemelyDb"));
        assertEquals(2, szam(lista, "$[0].csaladDb"));

        mockMvc.perform(delete("/api/csaladfak/{id}", faId)).andExpect(status().isNoContent());
        assertEquals(0, darab("MATCH (n) RETURN count(n)"));

        mockMvc.perform(delete("/api/csaladfak/{id}", faId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.hiba").exists());
        mockMvc.perform(get("/api/csaladfak/{id}/graf", faId)).andExpect(status().isNotFound());
    }

    @Test
    void hibasGedcomEsetenSemmiNemMentodik() throws Exception {
        String valasz = mockMvc.perform(multipart("/api/csaladfak")
                        .file(new MockMultipartFile("fajl", "hibas.ged", "text/plain",
                                "0 @I1@ INDI\nX NAME Hibás /Sor/\n".getBytes(StandardCharsets.UTF_8)))
                        .param("nev", "Hibás")
                        .param("tulajdonos", "anna"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String hiba = JsonPath.read(valasz, "$.hiba");
        assertTrue(hiba.contains("2. sor"), hiba);
        assertEquals(0, darab("MATCH (n) RETURN count(n)"));
    }

    @Test
    void ismeretlenTulajdonosnakNincsErtesitese() throws Exception {
        importalas("dummy.ged", "Teszt", "anna");
        String ertesitesek = lekeres("/api/egyezesek?tulajdonos=senki");
        assertEquals(0, szam(ertesitesek, "$.olvasatlanDb"));
        List<?> lista = JsonPath.read(ertesitesek, "$.egyezesek");
        assertTrue(lista.isEmpty());
    }
}
