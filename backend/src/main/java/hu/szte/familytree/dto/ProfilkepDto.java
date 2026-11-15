package hu.szte.familytree.dto;

import hu.szte.familytree.model.Szemely;
import hu.szte.familytree.model.SzerepelRajta;

// A profilkép a fotó egy kivágása: a frontend az url-ről betöltött képből a keretet jeleníti meg
public record ProfilkepDto(Long szemelyId, Long kepId, String url,
                           double x, double y, double szelesseg, double magassag) {

    public static ProfilkepDto bol(Szemely szemely, SzerepelRajta cimke) {
        Long kepId = cimke.getKep().getId();
        return new ProfilkepDto(szemely.getId(), kepId, KepDto.url(kepId),
                cimke.getxPozicio(), cimke.getyPozicio(), cimke.getSzelesseg(), cimke.getMagassag());
    }
}
