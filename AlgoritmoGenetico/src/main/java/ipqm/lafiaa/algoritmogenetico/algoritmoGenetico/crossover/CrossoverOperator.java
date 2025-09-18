package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;

import java.util.concurrent.ThreadLocalRandom;

public interface CrossoverOperator {
    Individual[] crossover(Individual p1, Individual p2, ThreadLocalRandom rand) throws Exception;
}
