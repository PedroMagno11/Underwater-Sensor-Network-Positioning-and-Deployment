package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.GeneFactory;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;
import ipqm.lafiaa.algoritmogenetico.utils.NameGenerator;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class AddBuoysMutation implements MutationOperator{
    private final int kMin, kMax;
    private final NameGenerator nameGenerator;

    public AddBuoysMutation(int kMin, int kMax, NameGenerator nameGenerator){
        if(kMin < 1 || kMax < kMin) throw new IllegalArgumentException("kMin / kMax invalid");
        this.kMin = kMin;
        this.kMax = kMax;
        this.nameGenerator = nameGenerator;
    }


    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {
        Map<String, Buoy> genes = MutationUtils.deepCopy(parent.getGenesMap());
        int n = genes.size();
        if(n >= Parametros.QUANT_MAX_BOIAS) return parent;

        int k = rand.nextInt(kMax - kMin + 1) + kMin;
        k = Math.min(k, Parametros.QUANT_MAX_BOIAS - n);
        if(k <= 0) return parent;

        Set<String> existing = new HashSet<>(genes.keySet());
        for(int added=0; added<k; added++){
            Buoy b = GeneFactory.generateValidRandomGene();
            String name = nameGenerator.nextName(existing);
            existing.add(name);
            b.setNome(name);
            genes.put(name, b);
        }

        return new Individual(genes);
    }
}