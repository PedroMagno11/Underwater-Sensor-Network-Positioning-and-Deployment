package ipqm.lafiaa.algoritmogenetico.domain.dto;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;

import java.util.ArrayList;
import java.util.List;

public class IndividualOutputDTO {
    private List<BuoyOutputDTO> genes;
    private double fitness;

    public IndividualOutputDTO(Individual best) {
        genes = best.getGenes().stream().map(BuoyOutputDTO::new).toList();
        fitness = best.getFitness();
    }

    public List<BuoyOutputDTO> getGenes() {
        return genes;
    }
    public void setGenes(List<BuoyOutputDTO> genes) {
        this.genes = genes;
    }
    public double getFitness() {
        return fitness;
    }
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
