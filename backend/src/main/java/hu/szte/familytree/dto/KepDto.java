package hu.szte.familytree.dto;

import java.time.LocalDateTime;
import java.util.List;

// Egy fotó a rajta megjelölt személyekkel; az url-ről tölthető le maga a kép
public record KepDto(Long id, Long csaladfaId, String eredetiNev, String mimeTipus,
                     Integer szelesseg, Integer magassag, String leiras, LocalDateTime feltoltIdo,
                     String url, List<CimkeDto> cimkek) {

    public static String url(Long kepId) {
        return "/api/kepek/" + kepId + "/fajl";
    }
}
