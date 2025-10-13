package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover.CrossoverOperator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.FitnessEvaluator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.IndividualFactory;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation.MutationOperator;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Population {

    private final CrossoverOperator crossover;

    private final int tournamentSize;
    private final double elitismRate;
    private final double mutationRate;
    private final double crossoverRate;
    private final MutationOperator mutator;
    private final FitnessEvaluator fitnessEvaluator;
    private static final ThreadLocalRandom rand = ThreadLocalRandom.current();

    private List<Individual> population;

    public Population(int populationSize, double elitismRate, double mutatuionRate, double crossoverRate, int tournamentSize, MutationOperator mutationOperator, CrossoverOperator crossoverOperator, FitnessEvaluator fitnessEvaluator) throws IOException, URISyntaxException, InterruptedException {

        this.mutator = Objects.requireNonNull(mutationOperator);
        this.crossover = Objects.requireNonNull(crossoverOperator);
        this.fitnessEvaluator = Objects.requireNonNull(fitnessEvaluator);

        this.elitismRate = elitismRate;
        this.mutationRate = mutatuionRate;
        this.crossoverRate = crossoverRate;
        this.tournamentSize = tournamentSize;
        this.population = new CopyOnWriteArrayList<>();

        while (this.population.size() < populationSize) {
            // Gera indivíduo sem fitness
            Individual individual;
            // Gera parte da população circularmente (por exemplo, 30%)
//            if (this.population.size() < populationSize * 0.3) {
                individual = IndividualFactory.generateCircularIndividual();
//            } else {
//                individual = IndividualFactory.generateRandomIndividual();
//            }

            this.population.add(individual);
        }

        // avalia em paralelo
        fitnessEvaluator.evaluateAllBlocking(population);
        population.sort(Individual::compareTo);
    }

    public List<Individual> getPopulation() {
        population.sort(Individual::compareTo);
        return Collections.unmodifiableList(population);
    }

    public void evolve() throws Exception {

        List<Individual> currentPopulation = new ArrayList<>(population);
        currentPopulation.sort(Individual::compareTo);

        int eliteCount = Math.max(1, (int) Math.round(elitismRate * population.size()));
        eliteCount = Math.min(eliteCount, population.size() - 1);

        // Copia os individuos da elite direto
        List<Individual> nextGeneration = new ArrayList<>(currentPopulation.subList(0, eliteCount));
        // remove duplicatas por genes para manter diversidade
        nextGeneration = dedupByGenes(nextGeneration);

        while (nextGeneration.size() < population.size()){
            Individual[] parents = tournamentSelect(currentPopulation, tournamentSize);
            Individual p1 = parents[0];
            Individual p2 = parents[1];

            Individual child1, child2;

            if(rand.nextDouble() < crossoverRate){
                Individual[] children = crossover.crossover(p1, p2, rand);
                child1 = validateChild(children[0], p1, p2, rand);
                child2 = validateChild(children[1], p1, p2, rand);

            } else {
                child1 = p1.copy();
                child2 = p2.copy();
            }

            // Mutação (probabilística)
            if(rand.nextDouble() < mutationRate){
                child1 = mutator.mutate(child1, rand);
            }

            if(rand.nextDouble() < mutationRate){
                child2 = mutator.mutate(child2, rand);
            }

            if(nextGeneration.size() < population.size()){
                nextGeneration.add(child1);
            }

            if(nextGeneration.size() < population.size()){
                nextGeneration.add(child2);
            }
        }

        nextGeneration = dedupByGenes(nextGeneration);

        while(nextGeneration.size() < population.size()){
//            nextGeneration.add(IndividualFactory.generateRandomIndividual());
            nextGeneration.add(IndividualFactory.generateCircularIndividual());
        }
        // avalia a nova geração
        fitnessEvaluator.evaluateAllBlocking(nextGeneration);
        // organiza pelo menor fitness
        nextGeneration.sort(Individual::compareTo);
        this.population = nextGeneration;
    }

    private Individual[] tournamentSelect(List<Individual> currentPopulation, int tournamentSize) {
        Individual[] parents = new Individual[2];
        for(int i = 0; i < 2; i++){
            parents[i] = currentPopulation.get(rand.nextInt(currentPopulation.size()));
            for(int j = 0; j < tournamentSize; j++){
                int index = rand.nextInt(currentPopulation.size());
                if(currentPopulation.get(index).compareTo(parents[i]) < 0){
                    parents[i] = currentPopulation.get(index);
                }
            }
        }
        return parents;
    }

    private Individual validateChild(Individual child, Individual p1, Individual p2, ThreadLocalRandom rand) {
        if(child == null || child.getGenes() == null || child.getGenes().isEmpty()){
            try {
                return mutator.mutate(p1, rand);
            } catch (Exception e){
                return (p1 != null) ? p1 : p2;
            }
        }
        return child;
    }

    private List<Individual> dedupByGenes(List<Individual> currentPopulation) {
        Map<String, Individual> seen = new LinkedHashMap<>();
        for(Individual individual : currentPopulation){
            String signature = signature(individual);
            seen.putIfAbsent(signature, individual);
        }
        return new ArrayList<>(seen.values());
    }

    private String signature(Individual individual) {
        // Cria uma assinatura determinística do indivíduo
        // Ordena boias pelo nome e concatena nome:x:y
        return individual.getGenes().stream()
                .sorted(Comparator.comparing(Buoy::getNome))
                .map(b->b.getNome() + ":" + b.getPosX() + ":" + b.getPosY())
                .collect(Collectors.joining("|"));
    }
}
