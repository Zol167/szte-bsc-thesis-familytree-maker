package hu.szte.familytree.controller;

import hu.szte.familytree.dto.KepDto;
import hu.szte.familytree.dto.ProfilkepKeres;
import hu.szte.familytree.dto.SzemelyDto;
import hu.szte.familytree.repository.SzemelyRepository;
import hu.szte.familytree.service.KepService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/szemelyek")
@CrossOrigin(origins = "*")
public class SzemelyController {

    private final SzemelyRepository szemelyRepository;
    private final KepService kepService;

    public SzemelyController(SzemelyRepository szemelyRepository, KepService kepService) {
        this.szemelyRepository = szemelyRepository;
        this.kepService = kepService;
    }

    @GetMapping("/search")
    public List<SzemelyDto> keresesNevAlapjan(@RequestParam String nev) {
        return szemelyRepository.keresesNevAlapjan(nev).stream().map(SzemelyDto::bol).toList();
    }

    @GetMapping("/{id}/osok")
    public List<SzemelyDto> osok(@PathVariable Long id) {
        return szemelyRepository.osok(id).stream().map(SzemelyDto::bol).toList();
    }

    @GetMapping("/{id}/leszarmazottak")
    public List<SzemelyDto> leszarmazottak(@PathVariable Long id) {
        return szemelyRepository.leszarmazottak(id).stream().map(SzemelyDto::bol).toList();
    }

    // Azok a fotók, amelyeken a személy meg van jelölve
    @GetMapping("/{id}/kepek")
    public List<KepDto> kepek(@PathVariable Long id) {
        return kepService.szemelyKepei(id);
    }

    // Profilkép: a személy egyik címkéje az adott fotón (a frontend a keretet vágja ki)
    @PutMapping("/{id}/profilkep")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void profilkep(@PathVariable Long id, @RequestBody ProfilkepKeres keres) {
        kepService.profilkepBeallitas(id, keres.kepId());
    }

    @DeleteMapping("/{id}/profilkep")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void profilkepTorles(@PathVariable Long id) {
        kepService.profilkepTorles(id);
    }
}
