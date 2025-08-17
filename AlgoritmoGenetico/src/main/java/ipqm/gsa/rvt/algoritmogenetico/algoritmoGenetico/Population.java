package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Population {
    private static final Random rand = new Random();
    private static final int TOURNAMENT_SIZE = 3;
    private final float elitism = 0.1f;
    private final float mutation = 0.03f;
    private final float crossover = 0.8f;

    private Individual[] population;

    public Population(int populationSize) {
        this.population = new Individual[populationSize];
        for (int i = 0; i < populationSize; i++) {
            population[i] = Individual.generateRandomIndividual();
        }
        Arrays.sort(population);
    }

    public void evolve() {
        // garante que a população atual está ordenada (caso tenha sido alterada fora)
        Arrays.sort(population);

        Individual[] buffer = new Individual[population.length];

        int idx = Math.round(population.length * elitism);
        System.arraycopy(population, 0, buffer, 0, idx); // copia elites

        while (idx < buffer.length) {
            if (rand.nextFloat() <= crossover) {
                Individual[] parents = tournamentToSelectParents();
                Individual[] children = parents[0].mate(parents[1]);

                for (int k = 0; k < children.length && idx < buffer.length; k++) {
                    Individual child = children[k];
                    if (rand.nextFloat() <= mutation) {
                        child = child.mutate();
                    }
                    buffer[idx++] = child;
                }
            } else {
                // fallback quando não rola crossover: replica/muta um pai
                Individual parent = population[rand.nextInt(population.length)];
                Individual child = (rand.nextFloat() <= mutation) ? parent.mutate() : parent;
                buffer[idx++] = child;
            }
        }

        // ordena a NOVA geração e comuta
        Arrays.sort(buffer);
        population = buffer;
    }

    private Individual[] tournamentToSelectParents(){
        Individual[] parents = new Individual[2];

        for (int i = 0; i < 2; i++) {
            parents[i] = population[rand.nextInt(population.length)];
            for(int j = 0; j < TOURNAMENT_SIZE; j++){
                int index = rand.nextInt(population.length);
                if(population[index].compareTo(parents[i]) < 0){
                    parents[i] = population[index];
                }
            }
        }
        return parents;
    }

    public Individual[] getPopulation() {
        return population;
    }
}
