package ipqm.lafiaa.algoritmogenetico.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class IndividualDTO {
    private String individual;
    private List<BuoyDTO> buoys;

    public IndividualDTO(){
        buoys = new ArrayList<>();
    }

    public IndividualDTO(String individual, List<BuoyDTO> buoys) {
        this.individual = individual;
        this.buoys = buoys;
    }
    public String getIndividual() {
        return individual;
    }
    public void setIndividual(String individual) {
        this.individual = individual;
    }
    public List<BuoyDTO> getBuoys() {
        return buoys;
    }
    public void setBuoys(List<BuoyDTO> buoys) {
        this.buoys = buoys;
    }
}
