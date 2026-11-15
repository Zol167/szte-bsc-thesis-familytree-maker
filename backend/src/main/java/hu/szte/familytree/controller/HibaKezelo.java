package hu.szte.familytree.controller;

import hu.szte.familytree.service.GedcomParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

// Egységes {"hiba": "..."} válasz minden végpont hibáira, hogy a frontend egy helyen kezelhesse őket
@RestControllerAdvice
public class HibaKezelo {

    @ExceptionHandler(GedcomParseException.class)
    public ResponseEntity<Map<String, String>> hibasGedcom(GedcomParseException e) {
        return valasz(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statuszHiba(ResponseStatusException e) {
        return valasz(e.getStatusCode(), Objects.requireNonNullElse(e.getReason(), e.getStatusCode().toString()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tulNagyFajl(MaxUploadSizeExceededException e) {
        return valasz(HttpStatus.PAYLOAD_TOO_LARGE, "A feltöltött fájl túl nagy");
    }

    private static ResponseEntity<Map<String, String>> valasz(HttpStatusCode statusz, String uzenet) {
        return ResponseEntity.status(statusz).body(Map.of("hiba", uzenet));
    }
}
