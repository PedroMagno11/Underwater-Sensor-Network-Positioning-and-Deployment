package ipqm.lafiaa.algoritmogenetico;
import com.fasterxml.jackson.databind.ObjectMapper;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Population;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover.Crossover;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover.CrossoverOperator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.FitnessEvaluator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation.*;
import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.dto.IndividualOutputDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.PontoReferencialInputDTO;
import ipqm.lafiaa.algoritmogenetico.utils.HTTPRequest;
import ipqm.lafiaa.algoritmogenetico.utils.NameGenerator;
import ipqm.lafiaa.algoritmogenetico.utils.Response;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.Posicao;
import ipqm.lafiaa.algoritmogenetico.utils.file.GeneratorFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 * @author Pedro Magno
 * @grupo GSA (Grupo de Sistema de Armas)
 */
public class GeneticAlgorithm {

    public static void main(String[] args) throws Exception {

        requestTarget();

        // compõe os mutadores
        MutationOperator randomMutation = new RandomMutation();
        MutationOperator addBuoysMutation = new AddBuoysMutation(1,2, new NameGenerator("buoy"));
        MutationOperator removeBuoyMutation = new RemoveBuoysMutation(1, 2);

        Map<MutationOperator, Double> weigths = new LinkedHashMap<>();

        weigths.put(removeBuoyMutation, 2.0);
        weigths.put(randomMutation, 3.0);
        weigths.put(addBuoysMutation, 5.0);

        MutationOperator mutator = new CompositeMutator(weigths);

        // Parâmetros do GA
        int populationSize = 100;
        double elitismRate = 0.1;   // 10% melhores preservados
        double mutationRate = 0.2;  // 20% chance de mutar
        double crossoverRate = 0.2; // 20% chance de cruzar
        int tournamentSize = 3; // 3
        int generations = 200;


        // Crossover
        CrossoverOperator crossover = new Crossover();

        // Avaliação do fitness (com threads)
        int threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 8);
        try (FitnessEvaluator evaluator = new FitnessEvaluator(threads, true)) {

            // População inicial
            Population pop = new Population(
                populationSize,
                elitismRate,
                mutationRate,
                crossoverRate,
                tournamentSize,
                mutator,
                crossover,
                evaluator
            );

            GeneratorFile g = GeneratorFile.getInstance(generations);

            List<Double> bestFitnessPerGeneration = new ArrayList<>();
            List<Double> avgFitnessPerGeneration = new ArrayList<>();

            // Loop de gerações
            for (int gen = 1; gen <= generations; gen++) {
                pop.evolve();
                Individual best = pop.getPopulation().get(0); // menor fitness
                double bestFitness = pop.getPopulation().get(0).getFitness();
                double avg = pop.getPopulation().stream()
                        .mapToDouble(Individual::getFitness)
                        .average()
                        .orElse(Double.NaN);

                bestFitnessPerGeneration.add(bestFitness);
                avgFitnessPerGeneration.add(avg);

                IndividualOutputDTO bestOutput = new IndividualOutputDTO(best);
                g.registrar(bestOutput);
                g.salvar("temporario", "individuals");
                System.out.printf("Geração %d | Genes: %s | Melhor fitness: %.6f%n", gen, bestOutput.getGenes(), best.getFitness());
                System.out.printf("Geração %d | Melhor: %.6f | Média: %.6f%n", gen, bestFitness, avg);
            }

            try(PrintWriter out = new PrintWriter("fitness_data.csv")){
                out.println("geracao,melhor,media");
                for(int i = 0; i < bestFitnessPerGeneration.size(); i++){
                    out.printf(Locale.US,"%d,%f,%f%n", i, bestFitnessPerGeneration.get(i), avgFitnessPerGeneration.get(i));
                }
                System.out.println("Arquivo fitness_data.csv salvo com sucesso!");
            } catch (Exception e){
                e.printStackTrace();
            }
            // 🏆 Resultado final
            Individual bestOverall = pop.getPopulation().get(0);
            System.out.println("Melhor indivíduo final:");
            System.out.println(bestOverall);
        }

    }

    private static void requestTarget() throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();

        Response r = HTTPRequest.get("http://localhost:34085/api/ponto-referencial");

        PontoReferencialInputDTO pr = mapper.readValue(r.getBody().toString(), PontoReferencialInputDTO.class);

        Posicao posAlvo = new Posicao(new CoordenadaGeografica(pr.getLatitude(), pr.getLongitude()), null, null);

        Alvo alvo = Alvo.getInstance();
        alvo.setPosicao(posAlvo);
    }

}