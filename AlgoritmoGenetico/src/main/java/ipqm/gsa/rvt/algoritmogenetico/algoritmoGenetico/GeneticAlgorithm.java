package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.TipoGranada;
import ipqm.gsa.rvt.algoritmogenetico.domain.config.Parametros;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Buoy;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.PontoQueda;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.gsa.rvt.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Geodesics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Pedro Magno
 * @grupo GSA (Grupo de Sistema de Armas)
 */
public class GeneticAlgorithm {
    private final int populationSize;
    private final int generations;
    private static Random rand;
    private final Alvo alvo;
    private final static Logger LOGGER = LoggerFactory.getLogger(GeneticAlgorithm.class);
    private final Raia raia;

    public GeneticAlgorithm(int populationSize, int generations, Alvo alvo) {
        this.populationSize = populationSize;
        this.generations = generations;
        rand = new Random();
        this.alvo = alvo;
        raia = Raia.getRaia();
    }
        
    public List<Individual> initializePopulation() {
        List<Individual> population = new ArrayList<>();
        for(int i = 0; i < populationSize; i++){
            Set<Buoy> genes = generateBuoysInRandomPositions();
            population.add(new Individual(genes));
        }
        return population;
    }
    
    private Set<Buoy> generateBuoysInRandomPositions() {
        Set<Buoy> buoys = new HashSet<>();
        int numberOfBuoys = Raia.QUANTMINBOIAS + rand.nextInt(Raia.MAXBOIAS - Raia.QUANTMINBOIAS + 1);

        for(int i = 0; i < numberOfBuoys; i++){
            int posX = rand.nextInt(Raia.DIMMAX);
            int posY = rand.nextInt(Raia.DIMMAX);
            Buoy buoy = new Buoy("buoy" + (i + 1), posX, posY);
            CoordenadaGeografica coordGeoBoia = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(new CoordenadaCartesianaRVT(buoy.getPosX(), buoy.getPosY()), this.alvo.getPosicao());
            buoy.setLatGeo(coordGeoBoia.getLatitude());
            buoy.setLonGeo(coordGeoBoia.getLongitude());

            LOGGER.info("Buoy: " + buoy.getNome() + " | LAT: " + buoy.getLatGeo() + " | LNG: " + buoy.getLonGeo() + " | POS X: " + buoy.getPosX() + " | POS Y: " + buoy.getPosY());

            buoys.add(buoy);
        }
        return buoys;
    }

    private static Individual crossover(Individual p1, Individual p2) throws Exception{
        int n = Math.min(p1.getGenes().size(), p2.getGenes().size());
        int pontoDeCorte = (n <= 1) ? 1 : rand.nextInt(n - 1) + 1;
        Set<Buoy> genesDoFilho = new HashSet<>();
        genesDoFilho.addAll(p1.getGenes().stream().collect(Collectors.toList()).subList(0, Math.min(pontoDeCorte, p1.getGenes().size())));
        if(pontoDeCorte < p2.getGenes().size()){
            genesDoFilho.addAll(p2.getGenes().stream().collect(Collectors.toList()).subList(pontoDeCorte, p2.getGenes().size()));
        }
        while(genesDoFilho.size() < Raia.QUANTMINBOIAS){
            Buoy b = new Buoy();
            b.setNome("buoy" + genesDoFilho.size() + 1);
            b.setPosX(rand.nextInt(Raia.DIMMAX));
            b.setPosY(rand.nextInt(Raia.DIMMAX));
            genesDoFilho.add(b);
        }
        while(genesDoFilho.size() > Raia.MAXBOIAS){
            genesDoFilho.remove(genesDoFilho.size() - 1);
        }
        return new Individual(genesDoFilho);
    }
       
    public static void mutar(Individual individual) {
    
        for(Buoy b : individual.getGenes()){
            if(rand.nextDouble() < 0.2){
                try{
                    b.setPosX(Math.max(0, Math.min(Raia.DIMMAX -1 , b.getPosX() + rand.nextInt(Raia.DIMMAX) - (Raia.DIMMAX/2))));
                    b.setPosY(Math.max(0, Math.min(Raia.DIMMAX -1 , b.getPosY() + rand.nextInt(Raia.DIMMAX) - (Raia.DIMMAX/2))));
                    System.out.println(b.getNome() + " - X: " + b.getPosX() + " - Y: " + b.getPosY());
                } catch (Exception e){
                    System.err.println(e.getMessage());
                }
            }
            if(rand.nextDouble() < 0.1 && individual.getGenes().size() < Raia.MAXBOIAS){
                try{
                    int x = rand.nextInt(Raia.DIMMAX -1);
                    int y = rand.nextInt(Raia.DIMMAX -1);
                    Buoy buoy = new Buoy("buoy" + individual.getGenes().size() + 1, x, y);
                    individual.getGenes().add(buoy);
                } catch (Exception ex){
                    System.err.println(ex.getMessage());
                }

            } else if (rand.nextDouble() < 0.1 && individual.getGenes().size() > Raia.MAXBOIAS) {
                individual.getGenes().remove(rand.nextInt(individual.getGenes().size()));
            }
        }

    }

    // Retorna o indivíduo com o melhor fitness
    private double fitness(Individual individual){
        // Verifico a aptidão da população para cada tipo de granada
        for(TipoGranada t : TipoGranada.values()){
            // Verifico a aptidão do gene do indivíduo
            for(Buoy b : individual.getGenes()){
                try {
                    if(!detecta(b, alvo, t)) {
                        // Penaliza, pois a posição em que a boia se encontra , é impossível de detectar o splash.
                        individual.updateFitness(-1e2);
                        continue;
                    }

                    if(t == TipoGranada.GAE){
                        individual.detectaGAE();
                        individual.updateFitness(1e2);
                    }

                    if(t == TipoGranada.EXSUP){
                        individual.detectaEXSUP();
                        individual.updateFitness(1e2);
                    }

                    tempoDeteccao(b, alvo);
                    raia.atualizarBoia(b.getNome(), b.getPosX(), b.getPosY(), b.getLatGeo(), b.getLonGeo(), b.getTempoDeteccao());

                } catch (Exception ex){
                    LOGGER.error("Erro ao atualizar biblioteca de boias: " + ex.getMessage());
                }
            }
            try{
                PontoQueda pontoQueda = raia.calcularPontoQueda();
                CoordenadaGeografica coordGeoPontoQueda = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(pontoQueda.getPontoDeQueda(), alvo.getPosicao());
                individual.updateFitness(1e5);

                LOGGER.info("PONTO QUEDA: " + pontoQueda.getPontoDeQueda().getX() + " - " + pontoQueda.getPontoDeQueda().getY() + "CUSTO: " + pontoQueda.getMenorCusto());

                double distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo = Geodesics.distance(coordGeoPontoQueda.getLongitude(), coordGeoPontoQueda.getLatitude(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
                double distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo);

                if(distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo < 9.0){
                    individual.updateFitness(1e5);
                    System.out.println("EXATO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
                }
                else if(distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo >= 9.0 && distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo <= 45.0){
                    individual.updateFitness(1e2);
                    System.out.println("PRECISO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
                }
                else{
                    individual.updateFitness(-1e2);
                    System.out.println("EXATO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
                }

            } catch (Exception ex){
                System.out.println("Erro ao calcular ponto queda: " + ex.getMessage());
                LOGGER.error("Erro ao calcular ponto queda: " + ex.getMessage());
            }
        }

        return individual.getFitness();
    }

    private boolean detecta(Buoy buoy, Alvo alvo, TipoGranada tipo){
        double distanciaEmMilhasNauticas = Geodesics.distance(buoy.getLonGeo(), buoy.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        return distanciaEmMetros <= Parametros.getRaioDeDetecaoDoSplash(tipo);
    }

    // em ms
    private void tempoDeteccao(Buoy b, Alvo alvo) {
        double distanciaEmMilhasNauticas = Geodesics.distance(b.getLonGeo(), b.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        double tempo = distanciaEmMetros / Raia.VELOCSOM * 1000;
        if(rand.nextBoolean()){
            tempo += rand.nextGaussian() * Parametros.RUIDO_TEMPO_DETECCAO;
        }

        long t = Math.round(tempo) / 1000;
        b.setTempoDeteccao(t);
    }

    // Retorna o melhor indivíduo
    public void executar(Alvo alvo) throws Exception {

        // população inicial
        int POP = 40, GER = 60; // retirar essa merda já já
        List<Individual> population = initializePopulation();

        int interador = 0;
        // Avaliar se a população está apta
        for(Individual individual : population){
            System.out.println("FITNESS: " + fitness(individual));
            interador += 1;
        }

        // selecionar
        // reproduzir
        // mutar

        // avaliar a população


//        // loop evolutivo
//        for (int g=1; g<=GER; g++) {
//            pop.sort(Comparator.comparingDouble(ind -> {
//                try {
//                    Avaliador.erroMedioArranjo(ind, alvo);
//                } catch (Exception ex) {
//                    LOGGER.info("Erro: " + ex.getMessage());
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
//            double melhor = Avaliador.erroMedioArranjo(pop.get(0), alvo);
//            System.out.printf("Geração %d | Melhor erro médio: %.2f m | Boias: %d%n",
//                    g, melhor, pop.get(0).getGenes().size());
//        }

//        // 4) resultado
//        Individual best = pop.get(0);
//        System.out.println("\nMelhor arranjo:");
//        for (int i=0;i<best.getGenes().size();i++) {
//            Buoy b = best.getGenes().iterator().next();
//            System.out.printf("Boia %d (%s): x=%.1f, y=%.1f | lat=%.6f, lon=%.6f%n",
//                    i+1, b.getNome(), b.getPosX(), b.getPosY(), b.getLatGeo(), b.getLonGeo());
//        }
//    }
    }

}


    
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
