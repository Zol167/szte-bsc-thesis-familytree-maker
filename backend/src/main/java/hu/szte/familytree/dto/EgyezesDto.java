package hu.szte.familytree.dto;

import java.time.LocalDateTime;

// Egy értesítés egy felhasználó szemszögéből: a saját fájában lévő személy és a másik felhasználó
// fájában talált, pontosan egyező (név + születési év) személy
public record EgyezesDto(Long id, LocalDateTime letrehozva, boolean olvasott,
                         SzemelyDto sajatSzemely, Long sajatFaId, String sajatFaNev,
                         SzemelyDto masikSzemely, Long masikFaId, String masikFaNev, String masikTulajdonos) {
}
