package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;

import java.util.Random;

public interface MutationOperator {
    Individual mutate(Individual parent, Random rand) throws Exception;
    String getName();
}
