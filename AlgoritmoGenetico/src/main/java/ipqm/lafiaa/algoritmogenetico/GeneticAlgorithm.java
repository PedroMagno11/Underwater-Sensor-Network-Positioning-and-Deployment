package ipqm.lafiaa.algoritmogenetico;
import com.fasterxml.jackson.databind.ObjectMapper;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Population;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover.Crossover;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover.CrossoverOperator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual.FitnessEvaluator;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation.*;
import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.dto.PontoReferencialInputDTO;
import ipqm.lafiaa.algoritmogenetico.utils.HTTPRequest;
import ipqm.lafiaa.algoritmogenetico.utils.NameGenerator;
import ipqm.lafiaa.algoritmogenetico.utils.Response;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.Posicao;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
        int populationSize = 1000;
        double elitismRate = 0.05;   // 5% melhores preservados
        double mutationRate = 0.4;  // 40% chance de mutar
        double crossoverRate = 0.7; // 70% chance de cruzar
        int tournamentSize = 3;
        int generations = 100;


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

            // 🔁 Loop de gerações
            for (int gen = 1; gen <= generations; gen++) {
                pop.evolve();
                Individual best = pop.getPopulation().get(0); // menor fitness
                System.out.printf("Geração %d | Melhor fitness: %.4f%n", gen, best.getFitness());
            }

            // 🏆 Resultado final
            Individual bestOverall = pop.getPopulation().get(0);
            System.out.println("Melhor indivíduo final:");
            System.out.println(bestOverall);
        }
    }

    private static void requestTarget() throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();

        Response r = HTTPRequest.get("http://localhost:34080/api/ponto-referencial");

        PontoReferencialInputDTO pr = mapper.readValue(r.getBody().toString(), PontoReferencialInputDTO.class);

        Posicao posAlvo = new Posicao(new CoordenadaGeografica(pr.getLatitude(), pr.getLongitude()), null, null);

        Alvo alvo = Alvo.getInstance();
        alvo.setPosicao(posAlvo);
    }

}