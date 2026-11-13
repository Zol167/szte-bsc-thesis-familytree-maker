package hu.szte.familytree.controller;

import hu.szte.familytree.dto.SzemelyDto;
import hu.szte.familytree.repository.SzemelyRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/szemelyek")
@CrossOrigin(origins = "*")
public class SzemelyController {

    private final SzemelyRepository szemelyRepository;

    public SzemelyController(SzemelyRepository szemelyRepository) {
        this.szemelyRepository = szemelyRepository;
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
}
