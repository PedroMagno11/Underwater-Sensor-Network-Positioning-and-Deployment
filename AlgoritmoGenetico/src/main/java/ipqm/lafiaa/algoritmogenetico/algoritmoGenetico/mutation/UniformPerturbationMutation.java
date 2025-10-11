package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class UniformPerturbationMutation implements MutationOperator{

    private final int stepX, stepY;

    public UniformPerturbationMutation(int stepX, int stepY){
        this.stepX = Math.max(0, stepX);
        this.stepY = Math.max(0, stepY);
    }

    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {
        Map<String, Buoy> genes = MutationUtils.deepCopy(parent.getGenesMap());
        if(genes.isEmpty()) return parent;

        String key = new ArrayList<>(genes.keySet()).get(rand.nextInt(genes.size()));
        Buoy b = genes.get(key);

        int dx = (int)Math.round((rand.nextDouble()*2 - 1) * stepX);
        int dy = (int)Math.round((rand.nextDouble()*2 - 1) * stepY);

        b.setPosX(MutationUtils.clamp(b.getPosX() + dx, 0, Parametros.DIMENSAO_RAIA));
        b.setPosY(MutationUtils.clamp(b.getPosY() + dy, 0, Parametros.DIMENSAO_RAIA));

        genes.put(key, b);
        return new Individual(genes);
    }

    @Override
    public String getName() {
        return "Uniform Perturbation Mutation";
    }
}
