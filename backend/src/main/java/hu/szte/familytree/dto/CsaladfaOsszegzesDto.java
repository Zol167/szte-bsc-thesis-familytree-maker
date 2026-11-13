package hu.szte.familytree.dto;

import hu.szte.familytree.model.Csaladfa;

import java.time.LocalDateTime;

public record CsaladfaOsszegzesDto(Long id, String nev, String tulajdonos, LocalDateTime importIdo,
                                   long szemelyDb, long csaladDb) {

    public static CsaladfaOsszegzesDto bol(Csaladfa fa) {
        return new CsaladfaOsszegzesDto(fa.getId(), fa.getNev(), fa.getTulajdonos(), fa.getImportIdo(),
                fa.getSzemelyek().size(), fa.getCsaladok().size());
    }
}
