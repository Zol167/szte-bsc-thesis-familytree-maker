package hu.szte.familytree;

import hu.szte.familytree.dto.CsaladfaOsszegzesDto;
import hu.szte.familytree.repository.CsaladfaRepository;
import hu.szte.familytree.service.CsaladfaService;
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
    CommandLineRunner demoImport(CsaladfaService csaladfaService, CsaladfaRepository csaladfaRepository) {
        return args -> {
            System.out.println("--- GEDCOM Parser és Gráfépítés Demó ---");
            if (csaladfaRepository.count() > 0) {
                System.out.println("Már van családfa az adatbázisban, a demó import kimarad.");
                return;
            }
            try (InputStream is = new ClassPathResource("dummy.ged").getInputStream()) {
                CsaladfaOsszegzesDto fa = csaladfaService.importalas(is, "Demo", "demo");
                System.out.println("Beolvasott személyek száma: " + fa.szemelyDb());
                System.out.println("Beolvasott családok száma: " + fa.csaladDb());
                System.out.println("Neo4j mentés kész, családfa azonosító: " + fa.id());
                System.out.println("Talált lehetséges egyezések más felhasználók fáival: " + fa.egyezesDb());
            } catch (Exception e) {
                System.out.println("Hiba történt a feldolgozás vagy mentés során.");
                e.printStackTrace();
            }
            System.out.println("----------------------------------------");
        };
    }
}
