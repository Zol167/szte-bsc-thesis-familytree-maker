package hu.szte.familytree;

import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.repository.CsaladfaRepository;
import hu.szte.familytree.service.GrafEpitoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@SpringBootApplication
public class FamilyTreeMakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyTreeMakerApplication.class, args);
    }

    // Konzolos demó (2-3. sprint): a dummy.ged betöltése "Demo" fába, ha még nincs egy fa sem.
    // Kikapcsolható: familytree.demo-import=false
    @Bean
    @ConditionalOnProperty(name = "familytree.demo-import", havingValue = "true")
    CommandLineRunner demoImport(GrafEpitoService grafEpitoService, CsaladfaRepository csaladfaRepository) {
        return args -> {
            System.out.println("--- GEDCOM Parser és Gráfépítés Demó ---");
            if (csaladfaRepository.count() > 0) {
                System.out.println("Már van családfa az adatbázisban, a demó import kimarad.");
                return;
            }
            try (InputStream is = new ClassPathResource("dummy.ged").getInputStream()) {
                Csaladfa fa = grafEpitoService.importalas(is, "Demo", "demo");
                System.out.println("Beolvasott személyek száma: " + fa.getSzemelyek().size());
                System.out.println("Beolvasott családok száma: " + fa.getCsaladok().size());
                System.out.println("Neo4j mentés kész, családfa azonosító: " + fa.getId());
            } catch (Exception e) {
                System.out.println("Hiba történt a feldolgozás vagy mentés során.");
                e.printStackTrace();
            }
            System.out.println("----------------------------------------");
        };
    }
}
