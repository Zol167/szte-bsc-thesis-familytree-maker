package hu.szte.familytree.dto;

import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.model.SzerepelRajta;

import java.util.List;

public record CsaladfaGrafDto(Long id, String nev, String tulajdonos,
                              List<SzemelyDto> szemelyek, List<CsaladDto> csaladok,
                              List<ProfilkepDto> profilkepek) {

    public static CsaladfaGrafDto bol(Csaladfa fa) {
        return new CsaladfaGrafDto(fa.getId(), fa.getNev(), fa.getTulajdonos(),
                fa.getSzemelyek().stream().map(SzemelyDto::bol).toList(),
                fa.getCsaladok().stream().map(CsaladDto::bol).toList(),
                // személyenként legfeljebb egy profilkép: a profilkep jelzésű címke
                fa.getSzemelyek().stream()
                        .flatMap(sz -> sz.getSzereplesekKepeken().stream()
                                .filter(SzerepelRajta::isProfilkep)
                                .limit(1)
                                .map(cimke -> ProfilkepDto.bol(sz, cimke)))
                        .toList());
    }
}
