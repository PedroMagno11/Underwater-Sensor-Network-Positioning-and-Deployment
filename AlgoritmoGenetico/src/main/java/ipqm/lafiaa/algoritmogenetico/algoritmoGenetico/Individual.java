package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 * @author Pedro Magno
 * @grupo LaFIAA (Laboratório de Fusão e Inteligências Artificial Aplicada)
 */
public class Individual implements Comparable<Individual> {
    private final static Logger LOGGER = LoggerFactory.getLogger(Individual.class);
    private final Map<String, Buoy> genes;
    private volatile double fitness = Double.NaN;

    public Individual(Map<String, Buoy> genes) throws IOException, URISyntaxException, InterruptedException {
        this.genes = new LinkedHashMap<>(genes);
    }

    public Map<String, Buoy> getGenesMap(){
        return new LinkedHashMap<>(genes);
    }

    public List<Buoy> getGenes(){
        return new ArrayList<>(genes.values());
    }

    public boolean hasFitness(){
        return !Double.isNaN(fitness);
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public int compareTo(Individual o) {
        double a = Double.isNaN(this.fitness) ? Double.POSITIVE_INFINITY : this.fitness;
        double b = Double.isNaN(o.fitness) ? Double.POSITIVE_INFINITY : o.fitness;
        return Double.compare(a, b);
    }

    @Override
    public String toString() {
        return "Individual [genes=" + genes + "] [fitness=" + fitness + "]";
    }

}