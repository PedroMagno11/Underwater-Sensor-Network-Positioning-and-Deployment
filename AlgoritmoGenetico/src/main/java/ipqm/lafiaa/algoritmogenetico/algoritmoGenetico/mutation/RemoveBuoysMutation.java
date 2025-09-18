package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;

import java.util.*;

public class RemoveBuoysMutation implements MutationOperator{
    private final int kMin, kMax;

    public RemoveBuoysMutation(int kMin, int kMax){
        if(kMin < 1 || kMax < kMin) throw new IllegalArgumentException("kMin/kMax invalid");
        this.kMin = kMin;
        this.kMax = kMax;
    }


    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {
        Map<String, Buoy> genes = MutationUtils.deepCopy(parent.getGenesMap());
        int n = genes.size();
        if(n <= Parametros.QUANT_MIN_BOIAS) return parent;

        int k = rand.nextInt(kMax - kMin + 1) + kMin;
        k = Math.min(k, n - Parametros.QUANT_MIN_BOIAS);

        if(k<=0) return parent;

        List<String> keys = new ArrayList<>(genes.keySet());
        Collections.shuffle(keys, rand);

        for(int i = 0; i<k; i++){
            genes.remove(keys.get(i));
        }

        return new Individual(genes);
    }
}