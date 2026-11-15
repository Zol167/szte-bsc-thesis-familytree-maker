package hu.szte.familytree.dto;

import hu.szte.familytree.model.Csaladfa;

import java.time.LocalDateTime;

// egyezesDb: a fa személyeit érintő lehetséges egyezések (ütközések) száma más felhasználók fáival
public record CsaladfaOsszegzesDto(Long id, String nev, String tulajdonos, LocalDateTime importIdo,
                                   long szemelyDb, long csaladDb, long egyezesDb) {

    public static CsaladfaOsszegzesDto bol(Csaladfa fa, long egyezesDb) {
        return new CsaladfaOsszegzesDto(fa.getId(), fa.getNev(), fa.getTulajdonos(), fa.getImportIdo(),
                fa.getSzemelyek().size(), fa.getCsaladok().size(), egyezesDb);
    }
}
