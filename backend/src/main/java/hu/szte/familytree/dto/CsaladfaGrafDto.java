package hu.szte.familytree.dto;

import hu.szte.familytree.model.Csaladfa;

import java.util.List;

public record CsaladfaGrafDto(Long id, String nev, String tulajdonos,
                              List<SzemelyDto> szemelyek, List<CsaladDto> csaladok) {

    public static CsaladfaGrafDto bol(Csaladfa fa) {
        return new CsaladfaGrafDto(fa.getId(), fa.getNev(), fa.getTulajdonos(),
                fa.getSzemelyek().stream().map(SzemelyDto::bol).toList(),
                fa.getCsaladok().stream().map(CsaladDto::bol).toList());
    }
}
