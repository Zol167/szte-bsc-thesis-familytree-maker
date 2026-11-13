package hu.szte.familytree.controller;

import hu.szte.familytree.dto.CsaladfaGrafDto;
import hu.szte.familytree.dto.CsaladfaOsszegzesDto;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.service.CsaladfaService;
import hu.szte.familytree.service.GedcomParseException;
import hu.szte.familytree.service.GrafEpitoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/csaladfak")
@CrossOrigin(origins = "*") // Engedélyezi, hogy a Vue.js (másik porton) hozzáférjen az adatokhoz
public class CsaladfaController {

    private final GrafEpitoService grafEpitoService;
    private final CsaladfaService csaladfaService;

    public CsaladfaController(GrafEpitoService grafEpitoService, CsaladfaService csaladfaService) {
        this.grafEpitoService = grafEpitoService;
        this.csaladfaService = csaladfaService;
    }

    // GEDCOM feltöltés: multipart/form-data, mezők: fajl, nev, tulajdonos
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CsaladfaOsszegzesDto importalas(@RequestParam("fajl") MultipartFile fajl,
                                           @RequestParam String nev,
                                           @RequestParam String tulajdonos) throws IOException {
        try (InputStream is = fajl.getInputStream()) {
            Csaladfa fa = grafEpitoService.importalas(is, nev, tulajdonos);
            return CsaladfaOsszegzesDto.bol(fa);
        }
    }

    @GetMapping
    public List<CsaladfaOsszegzesDto> listazas() {
        return csaladfaService.osszegzesek();
    }

    // Lapos csomópont + él lista a frontend elrendező algoritmusa számára
    @GetMapping("/{id}/graf")
    public CsaladfaGrafDto graf(@PathVariable Long id) {
        return csaladfaService.graf(id);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] tartalom = csaladfaService.export(id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"csaladfa-" + id + ".ged\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(tartalom);
    }

    @ExceptionHandler(GedcomParseException.class)
    public ResponseEntity<Map<String, String>> hibasGedcom(GedcomParseException e) {
        return ResponseEntity.badRequest().body(Map.of("hiba", e.getMessage()));
    }
}
