package hu.szte.familytree.dto;

// Egy személy kerete egy fotón, a kép méretéhez viszonyított (0–1) koordinátákkal
public record CimkeDto(Long szemelyId, String keresztnev, String vezeteknev,
                       double x, double y, double szelesseg, double magassag, boolean profilkep) {
}
