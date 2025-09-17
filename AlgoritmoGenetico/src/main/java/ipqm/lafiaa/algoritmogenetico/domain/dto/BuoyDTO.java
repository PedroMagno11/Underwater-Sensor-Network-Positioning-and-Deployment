package ipqm.lafiaa.algoritmogenetico.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

public class BuoyDTO {
    @JsonAlias("lat")
    private Double latitude;
    @JsonAlias({"lng", "long"})
    private Double longitude;
    private String name;
    private Long timeSplash;

    public BuoyDTO(Buoy b){
        latitude = b.getLatGeo();
        longitude = b.getLonGeo();
        name = b.getNome();
        timeSplash = b.getTempoDeteccao();
    }

    public BuoyDTO(Double latitude, Double longitude, String name, Long timeSplash) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.timeSplash = timeSplash;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public Long getTimeSplash() {
        return timeSplash;
    }
}
