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

///**
// *
// * @author Pedro Magno
// * @grupo GSA (Grupo de Sistema de Armas)
// */
public class GeneticAlgorithm {

    public static void main(String[] args) {

        Raia raia = Raia.getRaia();

        final int populationSize = 50;

        final int maxGenerations = 5;

        long startTime = System.currentTimeMillis();

        Population pop = new Population(populationSize);

        int i = 0;
        Individual best = pop.getPopulation()[0];

        while ((i++ <= maxGenerations) && (best.getFitness() != 0)) {
            System.out.println("Generation " + i + ": " + best.getGenes());
            pop.evolve();
            best = pop.getPopulation()[0];
        }

        // Get the end time for the simulation.
        long endTime = System.currentTimeMillis();

        // Print out some information to the console.
        System.out.println("Generation " + i + ": " + best.getGenes());
        System.out.println("Total execution time: " + (endTime - startTime) +
                "ms");
    }
}