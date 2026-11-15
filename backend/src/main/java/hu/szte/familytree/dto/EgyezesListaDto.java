package hu.szte.familytree.dto;

import java.util.List;

// Az értesítések lenyíló ablakának adatai: az olvasatlanok száma (jelvény) és a lista
public record EgyezesListaDto(long olvasatlanDb, List<EgyezesDto> egyezesek) {
}
