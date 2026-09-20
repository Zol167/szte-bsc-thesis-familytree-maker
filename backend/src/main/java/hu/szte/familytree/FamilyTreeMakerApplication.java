package hu.szte.familytree;

import hu.szte.familytree.model.Szemely;
import hu.szte.familytree.repository.SzemelyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FamilyTreeMakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyTreeMakerApplication.class, args);
    }

    @Bean
    CommandLineRunner tesztLekerdezes(SzemelyRepository repository) {
        return args -> {
            System.out.println("--- Neo4j Adatbázis Teszt ---");
            Iterable<Szemely> szemelyek = repository.findAll();
            for (Szemely sz : szemelyek) {
                System.out.println("Találat az adatbázisból: " + sz.getKeresztnev());
            }
            System.out.println("-----------------------------");
        };
    }
}