package ipqm.lafiaa.algoritmogenetico.domain.dto;

public class PontoReferencialInputDTO {
    private Double latitude;
    private Double longitude;

    public PontoReferencialInputDTO() {

    }

    public PontoReferencialInputDTO(Double latitude, Double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

}
