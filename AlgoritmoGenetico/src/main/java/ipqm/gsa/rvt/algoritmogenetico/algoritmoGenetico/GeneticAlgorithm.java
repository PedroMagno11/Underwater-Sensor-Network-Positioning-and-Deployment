package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;//package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;
//
//import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
//import ipqm.gsa.rvt.algoritmogenetico.domain.TipoGranada;
//import ipqm.gsa.rvt.algoritmogenetico.domain.config.Parametros;
//import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Buoy;
//import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
//import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.PontoQueda;
//import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
//import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
//import ipqm.gsa.rvt.algoritmogenetico.utils.conversor.ConversorUnidades;
//import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Geodesics;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//import java.util.stream.Collectors;
//

import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.Population;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;

import java.util.ArrayList;
import java.util.List;

///**
// *
// * @author Pedro Magno
// * @grupo GSA (Grupo de Sistema de Armas)
// */
public class GeneticAlgorithm {

    public static void main(String[] args) {

        Raia raia = Raia.getRaia();

        final int populationSize = 100;
        final double crossoverRate = 0.7;
        final double elitismRate = 0.25;
        final double mutationRate = 0.05;
        final int maxGenerations = 10;

        long startTime = System.currentTimeMillis();

        Population population = new Population(populationSize, elitismRate, mutationRate, crossoverRate, maxGenerations);
        List<Individual> bestIndividuals = new ArrayList<>();
        Individual best = population.getPopulation().getFirst();
        int i = 0;
        while ( i < maxGenerations) {
            population.evolve();
            best = population.getPopulation().getFirst();
            bestIndividuals.add(best);
            i+=1;
        }

        long endTime = System.currentTimeMillis();

        for(Individual individual : bestIndividuals) {
            System.out.println(individual);
        }

        System.out.println("Generation " + i + ": " + bestIndividuals.getFirst());
        System.out.println("Total execution time: " + (endTime - startTime) +
                "ms");
    }
}