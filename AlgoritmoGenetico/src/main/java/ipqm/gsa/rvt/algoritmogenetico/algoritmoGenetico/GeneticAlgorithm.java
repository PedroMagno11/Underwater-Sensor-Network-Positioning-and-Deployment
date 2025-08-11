package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.Avaliador;
import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Boia;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pedro Magno
 * @grupo GSA (Grupo de Sistema de Armas)
 */
public class GeneticAlgorithm {
    private final int populationSize;
    private final int numberOfBuoys;
    private final int areaSize;
    private final double sigma; // mutation rate
    private final int generations;
    private static Random rand;
    private Raia raia = Raia.getRaia();


    
    public GeneticAlgorithm(int populationSize, int numberOfBuoys, int areaSize, double sigma, int generations) {
        this.populationSize = populationSize;
        this.numberOfBuoys = numberOfBuoys;
        this.areaSize = areaSize;
        this.sigma = sigma; 
        this.generations = generations;
        rand = new Random();
    }
        
    public List<Individual> initializePopulation() throws Exception{
        List<Individual> population = new ArrayList<>();
        for(int i = 0; i < populationSize; i++){
            List<Boia> genes = generateBuoysInRandomPositions();
            population.add(new Individual(genes));
        }
        return population;
    }
    
    private static List<Boia> generateBuoysInRandomPositions() throws Exception{
        List<Boia> buoys = new ArrayList<>();
        int quantBoias = Raia.QUANTMINBOIAS + rand.nextInt(Raia.MAXBOIAS - Raia.QUANTMINBOIAS + 1);

        for(int i = 0; i < quantBoias; i++){
            Boia buoy = new Boia();
            buoy.setNome("buoy" + (i + 1));
            buoy.setPosX(rand.nextInt(Raia.DIMMAX));
            buoy.setPosY(rand.nextInt(Raia.DIMMAX));
            buoys.add(buoy);
        }
        return buoys;
    }

    private static Individual crossover(Individual p1, Individual p2) throws Exception{
        int n = Math.min(p1.getGenes().size(), p2.getGenes().size());
        int pontoDeCorte = (n <= 1) ? 1 : rand.nextInt(n - 1) + 1;
        List<Boia> genesDoFilho = new ArrayList<>();
        genesDoFilho.addAll(p1.getGenes().subList(0, Math.min(pontoDeCorte, p1.getGenes().size())));
        if(pontoDeCorte < p2.getGenes().size()){
            genesDoFilho.addAll(p2.getGenes().subList(pontoDeCorte, p2.getGenes().size()));
        }
        while(genesDoFilho.size() < Raia.QUANTMINBOIAS){
            Boia b = new Boia();
            b.setNome("B" + genesDoFilho.size() + 1);
            b.setPosX(rand.nextInt(Raia.DIMMAX));
            b.setPosY(rand.nextInt(Raia.DIMMAX));
            genesDoFilho.add(b);
        }
        while(genesDoFilho.size() > Raia.MAXBOIAS){
            genesDoFilho.remove(genesDoFilho.size() - 1);
        }
        return new Individual(genesDoFilho);
    }
       
    public static void mutar(Individual ind) throws Exception{
    
        for(Boia b : ind.getGenes()){
            if(rand.nextDouble() < 0.2){
                b.setPosX(Math.max(0, Math.min(Raia.DIMMAX -1 , b.getPosX() + rand.nextInt(Raia.DIMMAX) - (Raia.DIMMAX/2))));
                b.setPosY(Math.max(0, Math.min(Raia.DIMMAX -1 , b.getPosY() + rand.nextInt(Raia.DIMMAX) - (Raia.DIMMAX/2))));
                System.out.println(b.getNome() + " - : " + b.getPosX() + " - Y: " + b.getPosY());
            }
            // mutação estrutural: add ou remove boia
            if(rand.nextDouble() < 0.1 && ind.getGenes().size() < Raia.MAXBOIAS){
                int x = rand.nextInt(Raia.DIMMAX -1);
                int y = rand.nextInt(Raia.DIMMAX -1);
                Boia boia = new Boia();
                boia.setPosX(x);
                boia.setPosY(y);
                boia.setNome("B"+(ind.getGenes().size() + 1));
                ind.getGenes().add(boia);
            } else if (rand.nextDouble() < 0.1 && ind.getGenes().size() > Raia.QUANTMINBOIAS) {
                ind.getGenes().remove(rand.nextInt(ind.getGenes().size()));
            }
        }

    }
     
    
//    public static void executar(List<Alvo> alvos) throws Exception {
//        // 1) ref local fixo = centro da carta
//        PontoRef ref = refDoConjunto(alvos);
//
//        // 2) população inicial
//        int POP = 40, GER = 60;
//        List<Individual> pop = new ArrayList<>();
//        for (int i=0;i<POP;i++) pop.add(new Individual(generateBuoysInRandomPositions()));
//
//        // 3) loop evolutivo
//        for (int g=1; g<=GER; g++) {
//            pop.sort(Comparator.comparingDouble(ind -> {
//                try {
//                    Avaliador.erroMedioArranjo(ind, alvos, Raia.getRaia());
//                } catch (Exception ex) {
//                    Logger.getLogger(GeneticAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                return 0;
//            }));
//            List<Individual> nova = new ArrayList<>(pop.subList(0, 5)); // elitismo
//            while (nova.size() < POP) {
//                Individual p1 = pop.get(rand.nextInt(15));
//                Individual p2 = pop.get(rand.nextInt(15));
//                Individual f = crossover(p1, p2);
//                mutar(f);
//                nova.add(f);
//            }
//            pop = nova;
//            double melhor = Avaliador.erroMedioArranjo(pop.get(0), alvos, Raia.getRaia());
//            System.out.printf("Geração %d | Melhor erro médio: %.2f m | Boias: %d%n",
//                    g, melhor, pop.get(0).getGenes().size());
//        }
//
//        // 4) resultado
//        Individual best = pop.get(0);
//        System.out.println("\nMelhor arranjo:");
//        for (int i=0;i<best.getGenes().size();i++) {
//            Boia b = best.getGenes().get(i);
//            double[] ll = GEO.xyToLatLon(b.getPosX(), b.getPosY(), ref);
//            System.out.printf("Boia %d (%s): x=%.1f, y=%.1f | lat=%.6f, lon=%.6f%n",
//                    i+1, b.getNome(), b.getPosX(), b.getPosY(), ll[0], ll[1]);
//        }
//    }


}

    
//    public Individual run(){
//        List<Individual> population = initializePopulation();
//        List<double[]> explosionPoints = ExplosionSampler.generateExplosionPoints(30, areaSize, rand);
//        
//        for(int gen = 0; gen < generations; gen++){
//            for(Individual individual : population){
//                double fitness = 0.0;
//                for(double[] explosion : explosionPoints){
//                    fitness += FIMCalculator.computeFIMDeterminant(individual.getGenes(), explosion[0], explosion[1], sigma);
//                }
//                
//                fitness /= explosionPoints.size(); // média dos determinantes;
//                individual.setFitness(fitness);
//            }
//            
//            population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
//            List<Individual> nextGen = new ArrayList<>();
//            
//            // elitismo
//            nextGen.add(population.get(0));
//            
//            // crossover + mutação
//            while(nextGen.size() < populationSize){
//                Individual p1 = tournamentSelection(population);
//                Individual p2 = tournamentSelection(population);
//                Individual child = crossover(p1, p2);
//                mutate(child);
//                nextGen.add(child);
//            }
//            
//            population = nextGen;
//        }
//        
//        return population.get(0); // Melhor indivíduo
//    }    
    
    
//    private Individual tournamentSelection(List<Individual> population){
//        Individual best = population.get(rand.nextInt(population.size()));
//        for(int i = 0; i < 2; i++){
//            Individual candidate = population.get(rand.nextInt(population.size()));
//            if(candidate.getFitness() > best.getFitness()){
//                best = candidate;
//            }
//        }
//        return best;
//    }
    
//    private Individual crossover(Individual p1, Individual p2){
//        double[] childGenes = new double[p1.getGenes().length];
//        for(int i = 0; i < childGenes.length; i++){
//            childGenes[i] = (rand.nextBoolean()) ? p1.getGenes()[i] : p2.getGenes()[i];       
//        }
//        return new Individual(childGenes);
//    }
//    
//    private void mutate(Individual individual){
//        double[] genes = individual.getGenes();
//        for(int i = 0; i < genes.length; i++){
//            if (rand.nextDouble() < 0.1){
//                genes[i] = rand.nextDouble() * areaSize;
//            }
//        }
//    }
