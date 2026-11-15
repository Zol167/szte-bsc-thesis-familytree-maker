package hu.szte.familytree.integracio;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pontos egyezésen alapuló ütközés-detektálás (4. sprint).
// dummy.ged és dummy2.ged: Robert Eugene Williams (1822) és Joe Williams (1861) egyezik,
// Mary Ann Wilson és Mary Wilson (1828) szándékosan nem (a pontos egyezés korlátja).
class UtkozesDetektalasIT extends Neo4jIntegracioTeszt {

    private static final String EGYEZES_ELEK = "MATCH ()-[e:LEHETSEGES_EGYEZES]->() RETURN count(e)";

    @Test
    void kulonbozoTulajdonosokFaiKozottJelez() throws Exception {
        assertEquals(0, szam(importValasz("dummy.ged", "Anna fája", "anna"), "$.egyezesDb"));
        assertEquals(2, szam(importValasz("dummy2.ged", "Béla fája", "bela"), "$.egyezesDb"));
        assertEquals(2, darab(EGYEZES_ELEK));

        String anna = lekeres("/api/egyezesek?tulajdonos=anna");
        assertEquals(2, szam(anna, "$.olvasatlanDb"));
        List<String> annaSajat = JsonPath.read(anna, "$.egyezesek[*].sajatSzemely.keresztnev");
        assertEquals(Set.of("Robert Eugene", "Joe"), Set.copyOf(annaSajat));
        List<String> masikTulajdonosok = JsonPath.read(anna, "$.egyezesek[*].masikTulajdonos");
        assertEquals(Set.of("bela"), Set.copyOf(masikTulajdonosok));
        List<String> masikFak = JsonPath.read(anna, "$.egyezesek[*].masikFaNev");
        assertEquals(Set.of("Béla fája"), Set.copyOf(masikFak));

        // Béla fájában nagybetűs a vezetéknév (WILLIAMS): a normalizálás miatt mégis egyezik
        String bela = lekeres("/api/egyezesek?tulajdonos=bela");
        assertEquals(2, szam(bela, "$.olvasatlanDb"));
        List<String> belaSajatVezetek = JsonPath.read(bela, "$.egyezesek[*].sajatSzemely.vezeteknev");
        assertTrue(belaSajatVezetek.contains("WILLIAMS"));
        List<Number> szuletesiEvek = JsonPath.read(bela, "$.egyezesek[*].masikSzemely.szuletesiEv");
        assertEquals(Set.of(1822, 1861), Set.copyOf(szuletesiEvek.stream().map(Number::intValue).toList()));
    }

    @Test
    void olvasottnakJelolesTulajdonosonkentKulon() throws Exception {
        importalas("dummy.ged", "Anna fája", "anna");
        importalas("dummy2.ged", "Béla fája", "bela");
        long egyezesId = szam(lekeres("/api/egyezesek?tulajdonos=anna"), "$.egyezesek[0].id");

        mockMvc.perform(patch("/api/egyezesek/{id}/olvasott", egyezesId).param("tulajdonos", "anna"))
                .andExpect(status().isNoContent());
        String anna = lekeres("/api/egyezesek?tulajdonos=anna");
        assertEquals(1, szam(anna, "$.olvasatlanDb"));
        List<Boolean> olvasott = JsonPath.read(anna, "$.egyezesek[?(@.id == " + egyezesId + ")].olvasott");
        assertEquals(List.of(true), olvasott);
        // Béla számára ugyanaz az egyezés olvasatlan marad
        assertEquals(2, szam(lekeres("/api/egyezesek?tulajdonos=bela"), "$.olvasatlanDb"));

        // Ismételt jelölés nem duplikálja a lattak listát
        mockMvc.perform(patch("/api/egyezesek/{id}/olvasott", egyezesId).param("tulajdonos", "anna"))
                .andExpect(status().isNoContent());
        assertEquals(1, darab("MATCH ()-[e:LEHETSEGES_EGYEZES]->() WHERE size(e.lattak) > 0 RETURN max(size(e.lattak))"));

        // Más felhasználó nem jelölheti olvasottnak
        mockMvc.perform(patch("/api/egyezesek/{id}/olvasott", egyezesId).param("tulajdonos", "cecil"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.hiba").exists());

        mockMvc.perform(patch("/api/egyezesek/olvasott").param("tulajdonos", "bela"))
                .andExpect(status().isNoContent());
        assertEquals(0, szam(lekeres("/api/egyezesek?tulajdonos=bela"), "$.olvasatlanDb"));
        assertEquals(1, szam(lekeres("/api/egyezesek?tulajdonos=anna"), "$.olvasatlanDb"));
    }

    @Test
    void ugyanazonTulajdonosFaiNemUtkoznek() throws Exception {
        importalas("dummy.ged", "Első fa", "anna");
        assertEquals(0, szam(importValasz("dummy.ged", "Második fa", "anna"), "$.egyezesDb"));
        assertEquals(0, darab(EGYEZES_ELEK));
    }

    @Test
    void ujrakeresesIdempotens() throws Exception {
        importalas("dummy.ged", "Anna fája", "anna");
        importalas("dummy2.ged", "Béla fája", "bela");
        assertEquals(2, darab(EGYEZES_ELEK));

        mockMvc.perform(post("/api/egyezesek/ujrakereses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.egyezesDb").value(2));
        assertEquals(2, darab(EGYEZES_ELEK));

        // Az élek törlése után az újrakeresés ugyanazokat hozza létre újra
        neo4jClient.query("MATCH ()-[e:LEHETSEGES_EGYEZES]->() DELETE e").run();
        mockMvc.perform(post("/api/egyezesek/ujrakereses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.egyezesDb").value(2));
        assertEquals(2, darab(EGYEZES_ELEK));
    }

    @Test
    void listazasMutatjaAzEgyezesekSzamat() throws Exception {
        importalas("dummy.ged", "Anna fája", "anna");
        importalas("dummy2.ged", "Béla fája", "bela");
        String lista = lekeres("/api/csaladfak");
        List<Number> egyezesek = JsonPath.read(lista, "$[*].egyezesDb");
        assertEquals(List.of(2, 2), egyezesek.stream().map(Number::intValue).toList());
    }

    @Test
    void faTorleseAzEgyezeseitIsTorli() throws Exception {
        importalas("dummy.ged", "Anna fája", "anna");
        long belaFa = importalas("dummy2.ged", "Béla fája", "bela");

        mockMvc.perform(delete("/api/csaladfak/{id}", belaFa)).andExpect(status().isNoContent());
        assertEquals(0, darab(EGYEZES_ELEK));
        assertEquals(0, szam(lekeres("/api/egyezesek?tulajdonos=anna"), "$.egyezesek.length()"));
    }

    @Test
    void azEgyezesKulcsIndexelve() {
        assertEquals(1, darab("SHOW INDEXES YIELD name, properties WHERE name = 'szemely_egyezes_kulcs' RETURN count(*)"));
    }
}
