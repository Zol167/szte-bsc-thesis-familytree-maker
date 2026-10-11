package hu.szte.familytree;

import hu.szte.familytree.model.Szemely;
import hu.szte.familytree.service.GedcomParserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Map;

@SpringBootApplication
public class FamilyTreeMakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyTreeMakerApplication.class, args);
    }

    @Bean
    CommandLineRunner tesztParser(GedcomParserService parserService) {
        return args -> {
            System.out.println("--- 2. Sprint: GEDCOM Parser Teszt ---");
            try {
                // Egy dummy fájl beolvasása a resources mappából
                InputStream is = new ClassPathResource("dummy.ged").getInputStream();
                Map<String, Szemely> memoriabanLevoSzemelyek = parserService.parseGedcom(is);
                
                System.out.println("Beolvasott személyek száma: " + memoriabanLevoSzemelyek.size());
                for (Szemely sz : memoriabanLevoSzemelyek.values()) {
                    System.out.println("ID: " + sz.getGedcomId() + 
                                       " | Név: " + sz.getKeresztnev() + " " + sz.getVezeteknev() + 
                                       " | Született: " + sz.getSzuletesiDatum());
                }
            } catch (Exception e) {
                System.out.println("A tesztfájl nem található vagy hiba történt.");
            }
            System.out.println("--------------------------------------");
        };
    }
}
