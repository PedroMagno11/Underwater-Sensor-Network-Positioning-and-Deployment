package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Buoy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Pedro Magno
 * @grupo LaFIA (Laboratório de Fusão e Inteligências Artificial Aplicada)
 */
public class Individual {
    private final Set<Buoy> buoys; // genes
    private double fitness;
    private boolean detectaEXSUP;
    private boolean detectaGAE;

    public Individual(Set<Buoy> genes){
        this.buoys = genes;
        detectaEXSUP = false;
        detectaGAE = false;
        fitness = 0.0;
    }
    
    public Set<Buoy> getGenes(){
        return buoys;
    }
    
    public double getFitness() {
        return fitness;
    }

    public void updateFitness(double fitness) {
        this.fitness += fitness;
    }

    public boolean detectaEXSUP() {
        return detectaEXSUP;
    }

    public void detectaEXSUP(boolean detectaEXSUP) {
        this.detectaEXSUP = detectaEXSUP;
    }

    public boolean detectaGAE() {
        return detectaGAE;
    }

    public void setDetectaGAE(boolean detectaGAE) {
        this.detectaGAE = detectaGAE;
    }
}
