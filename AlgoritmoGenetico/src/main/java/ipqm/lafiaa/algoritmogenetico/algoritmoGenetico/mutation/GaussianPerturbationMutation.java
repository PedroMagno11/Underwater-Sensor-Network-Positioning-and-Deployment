package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class GaussianPerturbationMutation implements MutationOperator{

    private final double sigmaX, sigmaY;

    public GaussianPerturbationMutation(double sigmaX, double sigmaY){
        this.sigmaX = sigmaX;
        this.sigmaY = sigmaY;
    }

    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {
        Map<String, Buoy> genes = MutationUtils.deepCopy(parent.getGenesMap());
        if(genes.isEmpty()) return parent;

        String key = new ArrayList<>(genes.keySet()).get(rand.nextInt(genes.size()));
        Buoy b = genes.get(key);

        int nx = (int)Math.round(b.getPosX() + rand.nextGaussian() * sigmaX);
        int ny = (int)Math.round(b.getPosY() + rand.nextGaussian() * sigmaY);

        b.setPosX(MutationUtils.clamp(nx, 0, Parametros.DIMENSAO_RAIA));
        b.setPosY(MutationUtils.clamp(ny, 0, Parametros.DIMENSAO_RAIA));

        genes.put(key, b);
        return new Individual(genes);
    }

//    public Individual gaussianMutation() throws Exception {
//        Map<String, Buoy> copy = MutationUtils.deepCopy(genes);
//        if(copy.isEmpty()){
//            return this;
//        }
//
//        int index = rand.nextInt(genes.size());
//        String key = new ArrayList<>(genes.keySet()).get(index);
//        Buoy b = genes.get(key);
//
//        // o sigma indica uma mudança. Valor pequeno para ajustar a posição e valor grande para grandes mudanças de posição
//        double sigmaX = 2.5; // valor inicial
//        double sigmaY = 2.5; // valor inicial
//
//        if(this.getFitness() >= 8.0){
//            sigmaX = 2.5 + rand.nextDouble(10.0, 52.5);
//            sigmaY = 50.0 + rand.nextDouble(10.0, 52.5);
//        }
//
//        int nx = (int) Math.round(b.getPosX() + rand.nextGaussian() + sigmaX);
//        int ny = (int) Math.round(b.getPosY() + rand.nextGaussian() + sigmaY);
//
//        b.setPosX(MutationUtils.clamp(nx, 0, Raia.DIMMAX));
//        b.setPosY(MutationUtils.clamp(ny, 0, Raia.DIMMAX));
//
//        genes.put(key, b);
//
//        return new Individual(genes);
//    }
}
