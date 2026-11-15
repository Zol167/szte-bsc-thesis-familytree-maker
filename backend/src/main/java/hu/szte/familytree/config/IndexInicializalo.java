package hu.szte.familytree.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

// Az adatbázis-indexek létrehozása induláskor (az SDN magától nem hoz létre indexet).
// HIGHEST_PRECEDENCE: a demó import előtt fusson, hogy az ütközés-detektálás már indexet használjon.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IndexInicializalo implements ApplicationRunner {

    private final Neo4jClient neo4jClient;

    public IndexInicializalo(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Az egzakt egyezés kulcsa: ezzel lesz az ütközés-keresés személyenkénti indexkeresés
        neo4jClient.query("CREATE INDEX szemely_egyezes_kulcs IF NOT EXISTS FOR (sz:Szemely) ON (sz.egyezesKulcs)").run();
        // Az értesítések lekérdezése tulajdonos szerint indul
        neo4jClient.query("CREATE INDEX csaladfa_tulajdonos IF NOT EXISTS FOR (f:Csaladfa) ON (f.tulajdonos)").run();
    }
}
