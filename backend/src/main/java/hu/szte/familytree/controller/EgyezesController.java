package hu.szte.familytree.controller;

import hu.szte.familytree.dto.EgyezesListaDto;
import hu.szte.familytree.service.UtkozesDetektaloService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Az ütközés-detektálás értesítései (a frontend értesítési lenyíló ablaka ezt használja)
@RestController
@RequestMapping("/api/egyezesek")
@CrossOrigin(origins = "*")
public class EgyezesController {

    private final UtkozesDetektaloService utkozesDetektalo;

    public EgyezesController(UtkozesDetektaloService utkozesDetektalo) {
        this.utkozesDetektalo = utkozesDetektalo;
    }

    @GetMapping
    public EgyezesListaDto egyezesek(@RequestParam String tulajdonos) {
        return utkozesDetektalo.egyezesek(tulajdonos);
    }

    @PatchMapping("/{id}/olvasott")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void olvasott(@PathVariable Long id, @RequestParam String tulajdonos) {
        utkozesDetektalo.olvasottnakJelol(id, tulajdonos);
    }

    @PatchMapping("/olvasott")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mindOlvasott(@RequestParam String tulajdonos) {
        utkozesDetektalo.mindOlvasott(tulajdonos);
    }

    // Teljes újrakeresés az összes fa között; a válasz az egyezések száma
    @PostMapping("/ujrakereses")
    public Map<String, Long> ujrakereses() {
        return Map.of("egyezesDb", utkozesDetektalo.teljesKereses());
    }
}
