package hu.szte.familytree.service;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.model.GedcomAdatbazis;
import hu.szte.familytree.model.Szemely;
import hu.szte.familytree.repository.CsaladfaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class GrafEpitoService {

    private final GedcomParserService parserService;
    private final CsaladfaRepository csaladfaRepository;

    // Spring Dependency Injection konstruktoron keresztül
    public GrafEpitoService(GedcomParserService parserService, CsaladfaRepository csaladfaRepository) {
        this.parserService = parserService;
        this.csaladfaRepository = csaladfaRepository;
    }

    // Tisztán memóriabeli lépés (adatbázis nélkül tesztelhető): a parser szöveges GEDCOM
    // hivatkozásait (HUSB, WIFE, CHIL) valódi objektum-kapcsolatokká alakítja.
    public Csaladfa csaladfaEpitese(GedcomAdatbazis adat, String nev, String tulajdonos) {
        Map<String, Szemely> szemelyek = adat.getSzemelyek();
        Csaladfa fa = new Csaladfa(nev, tulajdonos);
        fa.getSzemelyek().addAll(szemelyek.values());
        // Az ütközés-detektálás kulcsa importkor készül el (normalizált név + születési év)
        for (Szemely szemely : szemelyek.values()) {
            szemely.setEgyezesKulcs(EgyezesKulcs.keszit(szemely));
        }

        for (Csalad csalad : adat.getCsaladok().values()) {
            csalad.getSzulok().clear();
            csalad.getGyermekek().clear();

            hozzaadNemDuplikalt(csalad.getSzulok(), szemelyek.get(csalad.getHusbId()));
            hozzaadNemDuplikalt(csalad.getSzulok(), szemelyek.get(csalad.getWifeId()));
            for (String gyerekId : csalad.getChilIds()) {
                hozzaadNemDuplikalt(csalad.getGyermekek(), szemelyek.get(gyerekId));
            }
            fa.getCsaladok().add(csalad);
        }
        return fa;
    }

    // Biztosítja, hogy vagy minden elmentődik, vagy hiba esetén semmi (Rollback)
    @Transactional("transactionManager")
    public Csaladfa importalas(InputStream gedcom, String nev, String tulajdonos) {
        GedcomAdatbazis adat = parserService.parseGedcom(gedcom);
        Csaladfa fa = csaladfaEpitese(adat, nev, tulajdonos);
        // Egyetlen kaszkádolt mentés: a Spring Data Neo4j a fán keresztül eléri az összes
        // személyt és családot, az azonos objektumot pedig csak egyszer menti.
        return csaladfaRepository.save(fa);
    }

    private static void hozzaadNemDuplikalt(List<Szemely> lista, Szemely szemely) {
        // null: a GEDCOM nem létező személyre hivatkozik (hiányos fájl) -> kihagyjuk
        if (szemely != null && !lista.contains(szemely)) {
            lista.add(szemely);
        }
    }
}
