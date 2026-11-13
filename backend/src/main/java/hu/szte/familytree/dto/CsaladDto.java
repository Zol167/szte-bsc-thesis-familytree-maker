package hu.szte.familytree.dto;

import hu.szte.familytree.model.Csalad;
import hu.szte.familytree.model.Szemely;

import java.util.List;

// Az élek azonosító-listákként: a frontend elrendező algoritmusa ebből építi a saját gráfját
public record CsaladDto(Long id, String gedcomId, String hazassagDatum, List<Long> szuloIdk, List<Long> gyermekIdk) {

    public static CsaladDto bol(Csalad cs) {
        return new CsaladDto(cs.getId(), cs.getGedcomId(), cs.getHazassagDatum(),
                cs.getSzulok().stream().map(Szemely::getId).toList(),
                cs.getGyermekek().stream().map(Szemely::getId).toList());
    }
}
