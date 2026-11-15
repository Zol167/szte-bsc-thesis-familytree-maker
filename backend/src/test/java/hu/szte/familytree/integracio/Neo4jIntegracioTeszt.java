package hu.szte.familytree.integracio;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.Neo4jContainer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Közös alap az integrációs tesztekhez: valódi Neo4j (ugyanaz a verzió, mint a docker-compose.yml-ben)
// egy eldobható Docker konténerben. A konténer JVM-enként egyszer indul, a tesztek előtt üres az adatbázis.
@SpringBootTest(properties = "familytree.demo-import=false")
@AutoConfigureMockMvc
abstract class Neo4jIntegracioTeszt {

    @ServiceConnection
    @SuppressWarnings("resource") // szándékosan nincs lezárva: a Testcontainers (Ryuk) takarítja el a JVM végén
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5.26-community");

    static final Path KEP_KONYVTAR;

    static {
        NEO4J.start();
        try {
            KEP_KONYVTAR = Files.createTempDirectory("familytree-kepek");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void tulajdonsagok(DynamicPropertyRegistry registry) {
        registry.add("familytree.kep-konyvtar", KEP_KONYVTAR::toString);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected Neo4jClient neo4jClient;

    @BeforeEach
    void uresKiindulas() throws IOException {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        try (Stream<Path> fajlok = Files.list(KEP_KONYVTAR)) {
            for (Path fajl : fajlok.toList()) {
                Files.delete(fajl);
            }
        }
    }

    // --- Segédfüggvények ---

    protected String importValasz(String eroforras, String nev, String tulajdonos) throws Exception {
        byte[] tartalom = new ClassPathResource(eroforras).getContentAsByteArray();
        return mockMvc.perform(multipart("/api/csaladfak")
                        .file(new MockMultipartFile("fajl", eroforras, "text/plain", tartalom))
                        .param("nev", nev)
                        .param("tulajdonos", tulajdonos))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected long importalas(String eroforras, String nev, String tulajdonos) throws Exception {
        return szam(importValasz(eroforras, nev, tulajdonos), "$.id");
    }

    protected String lekeres(String url, Object... valtozok) throws Exception {
        return mockMvc.perform(get(url, valtozok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected long szemelyId(long faId, String gedcomId) {
        return neo4jClient.query("""
                        MATCH (f:Csaladfa)-[:TARTALMAZ]->(sz:Szemely {gedcomId: $gedcomId})
                        WHERE id(f) = $faId
                        RETURN id(sz)""")
                .bindAll(Map.of("faId", faId, "gedcomId", gedcomId))
                .fetchAs(Long.class)
                .one()
                .orElseThrow();
    }

    protected long darab(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }

    protected static long kepfajlokSzama() throws IOException {
        try (Stream<Path> fajlok = Files.list(KEP_KONYVTAR)) {
            return fajlok.count();
        }
    }

    protected static long szam(String json, String utvonal) {
        return ((Number) JsonPath.read(json, utvonal)).longValue();
    }

    protected static Set<Long> szamok(String json, String utvonal) {
        List<Number> lista = JsonPath.read(json, utvonal);
        return lista.stream().map(Number::longValue).collect(Collectors.toSet());
    }

    protected static byte[] pngKep(int szelesseg, int magassag) throws IOException {
        BufferedImage kep = new BufferedImage(szelesseg, magassag, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(kep, "png", out);
        return out.toByteArray();
    }

    // A dummy.ged kiegészítve egy NOTE rekorddal (a parser átlépi), hogy legalább minMeret bájt legyen
    protected static byte[] nagyGedcom(int minMeret) throws IOException {
        String eredeti = new String(new ClassPathResource("dummy.ged").getContentAsByteArray(), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(eredeti.substring(0, eredeti.lastIndexOf("0 TRLR")));
        sb.append("0 @N1@ NOTE Kitöltés a feltöltési korlát teszteléséhez\n");
        String sor = "1 CONT " + "x".repeat(200) + "\n";
        while (sb.length() < minMeret) {
            sb.append(sor);
        }
        sb.append("0 TRLR\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Valódi HTTP multipart kérés törzse (TestRestTemplate-hez)
    protected static HttpEntity<MultiValueMap<String, Object>> gedcomFeltoltes(byte[] tartalom, String tulajdonos) {
        MultiValueMap<String, Object> torzs = new LinkedMultiValueMap<>();
        torzs.add("fajl", new ByteArrayResource(tartalom) {
            @Override
            public String getFilename() {
                return "nagy.ged";
            }
        });
        torzs.add("nev", "Nagy fájl");
        torzs.add("tulajdonos", tulajdonos);
        HttpHeaders fejlec = new HttpHeaders();
        fejlec.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(torzs, fejlec);
    }
}
