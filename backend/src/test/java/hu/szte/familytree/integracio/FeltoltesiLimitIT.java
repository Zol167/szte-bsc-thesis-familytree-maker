package hu.szte.familytree.integracio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

// A 3. sprint rejtett hibájának regressziós tesztje: a Spring alapból 1 MB-nál nagyobb feltöltést elutasít.
// Valódi Tomcat (RANDOM_PORT) kell hozzá, mert a MockMvc nem alkalmazza a feltöltési korlátot.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "familytree.demo-import=false")
class FeltoltesiLimitIT extends Neo4jIntegracioTeszt {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void tobbMegabajtosGedcomFeltoltheto() throws Exception {
        byte[] gedcom = nagyGedcom(2 * 1024 * 1024);
        assertTrue(gedcom.length > 2 * 1024 * 1024);

        ResponseEntity<String> valasz = restTemplate.postForEntity("/api/csaladfak",
                gedcomFeltoltes(gedcom, "anna"), String.class);
        assertEquals(HttpStatus.CREATED, valasz.getStatusCode(), valasz.getBody());
        assertEquals(3, szam(valasz.getBody(), "$.szemelyDb"));
    }
}
