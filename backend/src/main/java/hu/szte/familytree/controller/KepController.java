package hu.szte.familytree.controller;

import hu.szte.familytree.dto.CimkeKeres;
import hu.szte.familytree.dto.KepDto;
import hu.szte.familytree.service.KepService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class KepController {

    private final KepService kepService;

    public KepController(KepService kepService) {
        this.kepService = kepService;
    }

    // Fotó feltöltése egy családfához: multipart/form-data, mezők: fajl, leiras (nem kötelező)
    @PostMapping(path = "/csaladfak/{faId}/kepek", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KepDto feltoltes(@PathVariable Long faId,
                            @RequestParam("fajl") MultipartFile fajl,
                            @RequestParam(required = false) String leiras) {
        return kepService.feltoltes(faId, fajl, leiras);
    }

    @GetMapping("/csaladfak/{faId}/kepek")
    public List<KepDto> fanakKepei(@PathVariable Long faId) {
        return kepService.fanakKepei(faId);
    }

    @GetMapping("/kepek/{id}")
    public KepDto kep(@PathVariable Long id) {
        return kepService.kep(id);
    }

    // Maga a képfájl (img src / SVG <image href> számára)
    @GetMapping("/kepek/{id}/fajl")
    public ResponseEntity<Resource> fajl(@PathVariable Long id) {
        KepService.KepFajl fajl = kepService.fajl(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fajl.mimeTipus()))
                .header("X-Content-Type-Options", "nosniff")
                .body(fajl.tartalom());
    }

    @DeleteMapping("/kepek/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void torles(@PathVariable Long id) {
        kepService.torles(id);
    }

    // Címke (keret) létrehozása vagy áthelyezése; a válasz a kép az összes címkéjével
    @PutMapping("/kepek/{kepId}/cimkek/{szemelyId}")
    public KepDto cimkezes(@PathVariable Long kepId, @PathVariable Long szemelyId, @RequestBody CimkeKeres keres) {
        return kepService.cimkezes(kepId, szemelyId, keres);
    }

    @DeleteMapping("/kepek/{kepId}/cimkek/{szemelyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cimkeTorles(@PathVariable Long kepId, @PathVariable Long szemelyId) {
        kepService.cimkeTorles(kepId, szemelyId);
    }
}
