package hu.szte.familytree.dto;

import hu.szte.familytree.model.Szemely;

public record SzemelyDto(Long id, String gedcomId, String keresztnev, String vezeteknev, String nem,
                         String szuletesiDatum, Integer szuletesiEv, String halalozasiDatum) {

    public static SzemelyDto bol(Szemely sz) {
        return new SzemelyDto(sz.getId(), sz.getGedcomId(), sz.getKeresztnev(), sz.getVezeteknev(), sz.getNem(),
                sz.getSzuletesiDatum(), sz.getSzuletesiEv(), sz.getHalalozasiDatum());
    }
}
