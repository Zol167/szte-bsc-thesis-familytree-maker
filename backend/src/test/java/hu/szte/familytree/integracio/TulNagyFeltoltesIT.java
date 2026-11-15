package hu.szte.familytree.integracio;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

// A korlát feletti feltöltés tiszta 413-as JSON választ kap (nem kapcsolatbontást).
// A korlátot itt 1 MB-ra állítjuk, hogy ne kelljen 50 MB-ot küldeni.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "familytree.demo-import=false",
        "spring.servlet.multipart.max-file-size=1MB",
        "spring.servlet.multipart.max-request-size=1MB"})
class TulNagyFeltoltesIT extends Neo4jIntegracioTeszt {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void korlatFelettiFeltoltes413JsonValasszal() throws Exception {
        ResponseEntity<String> valasz = restTemplate.postForEntity("/api/csaladfak",
                gedcomFeltoltes(nagyGedcom(2 * 1024 * 1024), "anna"), String.class);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, valasz.getStatusCode(), valasz.getBody());
        String hiba = JsonPath.read(valasz.getBody(), "$.hiba");
        assertFalse(hiba.isBlank());
        assertEquals(0, darab("MATCH (f:Csaladfa) RETURN count(f)"));
    }
}
