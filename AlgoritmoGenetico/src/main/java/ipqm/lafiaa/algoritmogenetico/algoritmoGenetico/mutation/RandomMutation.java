package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.GeneFactory;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.GeneUtils;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class RandomMutation implements MutationOperator{
    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {
        Map<String, Buoy> genes = MutationUtils.deepCopy(parent.getGenesMap());

        int idx = rand.nextInt(genes.size());
        Buoy b = new ArrayList<>(genes.values()).get(idx);

        Buoy newBuoy = GeneFactory.generateValidRandomGene();
        newBuoy.setNome(b.getNome());

        genes.put(newBuoy.getNome(), newBuoy);
        return new Individual(genes);
    }

    @Override
    public String getName() {
        return "Random Mutation";
    }
}
