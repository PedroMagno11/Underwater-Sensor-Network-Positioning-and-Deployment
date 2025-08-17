package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.TipoGranada;
import ipqm.gsa.rvt.algoritmogenetico.domain.config.Parametros;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Buoy;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.PontoQueda;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.Posicao;
import ipqm.gsa.rvt.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Geodesics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Pedro Magno
 * @grupo LaFIAA (Laboratório de Fusão e Inteligências Artificial Aplicada)
 */
public class Individual implements Comparable<Individual> {
    private final static Logger LOGGER = LoggerFactory.getLogger(Individual.class);
    private static final Random rand = new Random();
    private static final Alvo TARGET = new Alvo(new Posicao(new CoordenadaGeografica(-22.34234, -43.23423), null, null));
    private final Raia raia = Raia.getRaia();
    private final Map<String, Buoy> genes;
    private double fitness;
    private double custo = 10.0;
    private boolean detectaEXSUP;
    private boolean detectaGAE;


    public Individual(Map<String, Buoy> genes){
        this.genes = genes;
        detectaEXSUP = false;
        detectaGAE = false;
        fitness = calculateFitness(genes);
    }

    public List<Buoy> getGenes(){
        return new ArrayList<>(genes.values());
    }

    public double getFitness() {
        return fitness;
    }

    public double getCusto() {
        return custo;
    }

    @Override
    public String toString() {
        return "Individual [genes=" + genes + "] [custo=" + custo + "] [fitness=" + fitness + "] [detectaEXSUP=" + detectaEXSUP + "] [detectaGAE=" + detectaGAE + "]";
    }

    @Override
    public int compareTo(Individual o) {
        if(this.fitness >= o.fitness && custo < o.custo){
            return 1;
        } else if (fitness < o.fitness && custo > o.custo){
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Individual){
            Individual ind = (Individual)obj;
            return (genes.equals(ind.genes) && fitness == ind.fitness);
        }
        return false;
    }

    public static Individual generateRandomIndividual() {
        Map<String, Buoy> buoys = new HashMap<>();
        int numberOfBuoys = Raia.QUANTMINBOIAS + rand.nextInt(Raia.MAXBOIAS - Raia.QUANTMINBOIAS + 1);
        for(int i = 0; i < numberOfBuoys; i++) {
            Buoy buoy = generateRandomGene();
            buoy.setNome("buoy" + (i + 1));
            buoys.put(buoy.getNome(), buoy);
        }
        return new Individual(buoys);
    }

    private static Buoy generateRandomGene(){
        int posX = rand.nextInt(Raia.DIMMAX);
        int posY = rand.nextInt(Raia.DIMMAX);
        Buoy buoy = new Buoy("", posX, posY);
        CoordenadaGeografica coordGeoBoia = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(new CoordenadaCartesianaRVT(buoy.getPosX(), buoy.getPosY()), TARGET.getPosicao());
        buoy.setLatGeo(coordGeoBoia.getLatitude());
        buoy.setLonGeo(coordGeoBoia.getLongitude());
        LOGGER.info("Buoy: " + buoy.getNome() + " | LAT: " + buoy.getLatGeo() + " | LNG: " + buoy.getLonGeo() + " | POS X: " + buoy.getPosX() + " | POS Y: " + buoy.getPosY());
        return buoy;
    }

    private double calculateFitness(Map<String, Buoy> genes) {
        // Verifica a aptidão do indivíduo para cada tipo de granada
        for(TipoGranada t : TipoGranada.values()){

            int quantDeteccoes = 0;
            // Verifico a aptidão do gene do indivíduo
            for(Buoy b : genes.values()){
                try {
                    if(detecta(b, TARGET, t)){
                        if(t == TipoGranada.GAE){
                            this.detectaGAE = true;
                            quantDeteccoes += 1;
                        }
                        if(t == TipoGranada.EXSUP){
                            this.detectaEXSUP = true;
                            quantDeteccoes += 1;
                        }
                        tempoDeteccao(b, TARGET);
                    } else {
                        continue;
                    }
                    raia.atualizarBoia(b.getNome(), b.getPosX(), b.getPosY(), b.getLatGeo(), b.getLonGeo(), b.getTempoDeteccao());

                } catch (Exception ex){
                    LOGGER.error("Erro ao atualizar biblioteca de boias: " + ex.getMessage());
                }
            }

            if(quantDeteccoes >= 3 ) {
                calcularPontoDeQueda();
            }

            else {
                System.out.println("Não é possível calcular o ponto de queda com somente " + quantDeteccoes + " boias detectando.");
            }
        }
        return custo;
    }

    private void calcularPontoDeQueda() {
        try {
            PontoQueda pontoQueda = raia.calcularPontoQueda();
            custo = pontoQueda.getMenorCusto();
            CoordenadaGeografica coordGeoPontoQueda = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(pontoQueda.getPontoDeQueda(), TARGET.getPosicao());

            System.out.println("PONTO QUEDA: " + pontoQueda.getPontoDeQueda().getX() + " - " + pontoQueda.getPontoDeQueda().getY() + "CUSTO: " + pontoQueda.getMenorCusto());

            double distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo = Geodesics.distance(coordGeoPontoQueda.getLongitude(), coordGeoPontoQueda.getLatitude(), TARGET.getCoordenadaGeografica().getLongitude(), TARGET.getCoordenadaGeografica().getLatitude());
            double distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo);

            if (distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo < 9.0) {
                System.out.println("EXATO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
            } else if (distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo >= 9.0 && distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo <= 45.0) {
                System.out.println("PRECISO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
            } else {
                System.out.println("ERRADO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
            }

        } catch (Exception ex) {
            System.out.println("Erro ao calcular ponto queda: " + ex.getMessage());
            LOGGER.error("Erro ao calcular ponto queda: " + ex.getMessage());
        }
    }

    private boolean detecta(Buoy buoy, Alvo alvo, TipoGranada tipo){
        double distanciaEmMilhasNauticas = Geodesics.distance(buoy.getLonGeo(), buoy.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        return distanciaEmMetros <= Parametros.getRaioDeDetecaoDoSplash(tipo);
    }

    private void tempoDeteccao(Buoy b, Alvo alvo) {
        double distanciaEmMilhasNauticas = Geodesics.distance(b.getLonGeo(), b.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        double tempo = distanciaEmMetros / Raia.VELOCSOM;

        if(rand.nextBoolean()){
            tempo += rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO / 1000.0;
        }

        long t = Math.max(0, Math.round(tempo));
        b.setTempoDeteccao(t);
    }

    public Individual mutate(){
        Map<String, Buoy> newGenes = new HashMap<>(genes);
        int index = rand.nextInt(genes.size());
        Buoy buoy = new ArrayList<>(genes.values()).get(index);

//        System.out.println("SELECIONADA: " + buoy);

        Buoy newBuoy = generateRandomGene();
        newBuoy.setNome(buoy.getNome());

//        System.out.println("GERADA: " + newBuoy);

        newGenes.put(newBuoy.getNome(), newBuoy);

//        for(Buoy b : genes.values()){
//            System.out.println("GENE ANTIGO: " + b);
//        }
//
//        for(Buoy b : newGenes.values()){
//            System.out.println("GENE NOVO: " + b);
//
//        }

        return new Individual(newGenes);
    }

    public Individual[] mate(Individual p2) {
        // Listas ordenadas por nome para ordem estável
        List<Buoy> l1 = this.genes.values()
                .stream().sorted(Comparator.comparing(Buoy::getNome))
                .collect(Collectors.toList());
        List<Buoy> l2 = p2.genes.values()
                .stream().sorted(Comparator.comparing(Buoy::getNome))
                .collect(Collectors.toList());

        int size1 = l1.size();
        int size2 = l2.size();
        int minSize = Math.min(size1, size2);

        // pivot em [0, minSize]
        int pivot = (minSize == 0) ? 0 : rand.nextInt(minSize + 1);

        // Filho 1: prefixo de l1 + sufixo de l2
        List<Buoy> child1List = new ArrayList<>(pivot + (size2 - pivot));
        child1List.addAll(l1.subList(0, pivot));
        child1List.addAll(l2.subList(pivot, size2));

        // Filho 2: prefixo de l2 + sufixo de l1
        List<Buoy> child2List = new ArrayList<>(pivot + (size1 - pivot));
        child2List.addAll(l2.subList(0, pivot));
        child2List.addAll(l1.subList(pivot, size1));

        // Converte listas para mapas (chave = nome da boia)
        Map<String, Buoy> genesChild1 = child1List.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a, b) -> b, HashMap::new));
        Map<String, Buoy> genesChild2 = child2List.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a, b) -> b, HashMap::new));

        return new Individual[] {
                new Individual(genesChild1),
                new Individual(genesChild2)
        };
    }
}
