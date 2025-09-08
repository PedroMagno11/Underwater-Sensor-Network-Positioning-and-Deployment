package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

import java.util.*;
import java.util.stream.Collectors;

public class Population {
    private static final Random rand = new Random();
    private final int tournamentSize;
    private final double elitismRate;
    private final double mutationRate;
    private final double crossoverRate;

    private List<Individual> population;

    public Population(int populationSize, double elitismRate, double mutatuionRate, double crossoverRate, int tournamentSize) {
        this.elitismRate = elitismRate;
        this.mutationRate = mutatuionRate;
        this.crossoverRate = crossoverRate;
        this.tournamentSize = tournamentSize;
        this.population = new ArrayList<>();

        while (this.population.size() < populationSize) {
            this.population.add(Individual.generateRandomIndividual());
        }

        population.sort(Individual::compareTo);
    }

    public List<Individual> getPopulation() {
        population.sort(Individual::compareTo);
        return Collections.unmodifiableList(population);
    }

    public void evolve() {
        List<Individual> currentPopulation = new ArrayList<>(population);
        currentPopulation.sort(Individual::compareTo);

        int eliteCount = Math.max(1, (int) Math.round(elitismRate * population.size()));
        eliteCount = Math.min(eliteCount, population.size() - 1);

        // Copia os individuos da elite direto
        List<Individual> nextGeneration = new ArrayList<>(population.subList(0, eliteCount));

        // remove duplicatas por genes para manter diversidade
        nextGeneration = dedupByGenes(nextGeneration);

        while (nextGeneration.size() < population.size()){
            Individual[] parents = tournamentSelect(currentPopulation, tournamentSize);
            Individual p1 = parents[0];
            Individual p2 = parents[1];

            Individual child1, child2;

            if(rand.nextDouble() < crossoverRate){
                Individual[] children = p1.crossover(p2);
                child1 = validateChild(children[0], p1, p2);
                child2 = validateChild(children[1], p1, p2);

            } else {
                child1 = p1;
                child2 = p2;
            }

            // Mutação (probabilística)
            if(rand.nextDouble() < mutationRate){
                child1 = child1.randomMutate();
            }

            if(rand.nextDouble() < mutationRate){
                child2 = child2.randomMutate();
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
            nextGeneration.add(Individual.generateRandomIndividual());
        }

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

    private Individual validateChild(Individual child, Individual p1, Individual p2) {
        if(child == null || child.getGenes().isEmpty()){
            return p1.randomMutate();
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
