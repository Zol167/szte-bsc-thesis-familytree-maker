package hu.szte.familytree.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// A fotók fájlrendszerbeli tárolása. Az adatbázisba csak a generált fájlnév kerül, így a fájlok
// helye (familytree.kep-konyvtar) szabadon áthelyezhető, és a felhasználó által adott név nem lesz útvonal.
@Service
public class KepTaroloService {

    private static final Logger LOG = LoggerFactory.getLogger(KepTaroloService.class);

    private static final Map<String, String> KITERJESZTESEK = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    public record TaroltKep(String fajlNev, String mimeTipus, Integer szelesseg, Integer magassag) {
    }

    // A fájl tartalmából felismert formátum (a kliens által küldött Content-Type nem megbízható)
    private record Formatum(String mimeTipus, int szelesseg, int magassag) {
    }

    private final Path gyoker;

    public KepTaroloService(@Value("${familytree.kep-konyvtar:feltoltesek}") String konyvtar) throws IOException {
        this.gyoker = Path.of(konyvtar).toAbsolutePath().normalize();
        Files.createDirectories(this.gyoker);
    }

    public TaroltKep mentes(MultipartFile fajl) {
        if (fajl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Üres fájl");
        }
        Formatum formatum = formatumFelismerese(fajl);
        String mimeTipus;
        if (formatum != null) {
            mimeTipus = formatum.mimeTipus();
        } else if ("image/webp".equals(bejelentettTipus(fajl))) {
            mimeTipus = "image/webp"; // a JDK nem tud WebP-t olvasni, ezért ezt a fejléc alapján fogadjuk el
        } else {
            throw nemTamogatott();
        }
        String kiterjesztes = KITERJESZTESEK.get(mimeTipus);
        if (kiterjesztes == null) {
            throw nemTamogatott();
        }

        String fajlNev = UUID.randomUUID() + kiterjesztes;
        try (InputStream is = fajl.getInputStream()) {
            Files.copy(is, utvonal(fajlNev));
        } catch (IOException e) {
            throw new UncheckedIOException("A kép nem menthető", e);
        }
        return new TaroltKep(fajlNev, mimeTipus,
                formatum != null ? formatum.szelesseg() : null,
                formatum != null ? formatum.magassag() : null);
    }

    public Resource betoltes(String fajlNev) {
        Path fajl = utvonal(fajlNev);
        if (!Files.isRegularFile(fajl)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A képfájl nem található");
        }
        return new FileSystemResource(fajl);
    }

    public void torles(String fajlNev) {
        try {
            Files.deleteIfExists(utvonal(fajlNev));
        } catch (IOException e) {
            // Az adatbázisból már törölve van; egy árva fájl nem ok arra, hogy a kérés hibával térjen vissza
            LOG.warn("A képfájl nem törölhető: {}", fajlNev, e);
        }
    }

    // Csak a tárolókönyvtár közvetlen fájljai érhetők el ("../" vagy alkönyvtár nem)
    private Path utvonal(String fajlNev) {
        Path fajl = gyoker.resolve(fajlNev).normalize();
        if (!gyoker.equals(fajl.getParent())) {
            throw new IllegalArgumentException("Érvénytelen fájlnév: " + fajlNev);
        }
        return fajl;
    }

    // Csak a kép fejlécét olvassa be (formátum, méret), a teljes képet nem dekódolja
    private static Formatum formatumFelismerese(MultipartFile fajl) {
        try (InputStream is = fajl.getInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            if (iis == null) return null;
            Iterator<ImageReader> olvasok = ImageIO.getImageReaders(iis);
            if (!olvasok.hasNext()) return null;
            ImageReader olvaso = olvasok.next();
            try {
                olvaso.setInput(iis, true, true);
                String[] mimeTipusok = olvaso.getOriginatingProvider().getMIMETypes();
                if (mimeTipusok == null || mimeTipusok.length == 0) return null;
                return new Formatum(mimeTipusok[0], olvaso.getWidth(0), olvaso.getHeight(0));
            } finally {
                olvaso.dispose();
            }
        } catch (IOException e) {
            return null; // felismerhető fejlécű, de sérült fájl
        }
    }

    // A kliens által küldött típus paraméterek nélkül, kisbetűsen (pl. "image/webp")
    private static String bejelentettTipus(MultipartFile fajl) {
        if (fajl.getContentType() == null) return null;
        try {
            MediaType tipus = MediaType.parseMediaType(fajl.getContentType());
            return tipus.getType() + "/" + tipus.getSubtype();
        } catch (InvalidMediaTypeException e) {
            return null;
        }
    }

    private static ResponseStatusException nemTamogatott() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Csak JPEG, PNG, GIF vagy WebP kép tölthető fel");
    }
}
