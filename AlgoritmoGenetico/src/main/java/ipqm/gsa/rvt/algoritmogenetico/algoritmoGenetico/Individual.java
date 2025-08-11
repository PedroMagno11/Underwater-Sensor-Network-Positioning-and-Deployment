package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Boia;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pedro Magno
 * @grupo LaFIA (Laboratório de Fusão e Inteligências Artificial Aplicada)
 */
public class Individual {
    private final List<Boia> buoys; // genes
    private double fitness;
    
    public Individual(List<Boia> genes){
        this.buoys = genes;
    }
    
    public List<Boia> getGenes(){
        return buoys;
    }
    
    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
    
    public int getNumberOfBuoys(){
        return buoys.size();
    }
    
    public Individual copy() throws Exception{
        List<Boia> c = new ArrayList<>();
        for(Boia b : buoys){ 

            Boia copy = new Boia();
            copy.setNome(b.getNome());
            copy.setPosX(b.getPosX());
            copy.setPosY(b.getPosY());
            c.add(copy);
        }
        return new Individual(c);
    }
    
}
