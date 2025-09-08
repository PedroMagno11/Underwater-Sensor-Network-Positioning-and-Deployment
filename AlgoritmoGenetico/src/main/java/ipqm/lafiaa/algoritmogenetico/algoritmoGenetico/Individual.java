package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico;

import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.TipoGranada;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.PontoQueda;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Raia;
import ipqm.lafiaa.algoritmogenetico.utils.MutationUtils;
import ipqm.lafiaa.algoritmogenetico.utils.NameGenerator;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.Posicao;
import ipqm.lafiaa.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.lafiaa.algoritmogenetico.utils.coord.Geodesics;
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
    private static NameGenerator nameGenerator = new NameGenerator("buoy");
    private final Raia raia = Raia.getRaia();
    private final Map<String, Buoy> genes;
    private double fitness;
    private double custo = 10.0;


    public Individual(Map<String, Buoy> genes){
        this.genes = genes;
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
        return "Individual [genes=" + genes + "] [custo=" + custo + "] [fitness=" + fitness + "]";
    }

    @Override
    public int compareTo(Individual o) {
        if(this.fitness > o.fitness){
            return 1;
        } else if (fitness < o.fitness){
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
            Buoy buoy = generateValidRandomGene();
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

        return buoy;
    }

    private static Buoy generateValidRandomGene(){

        while (true){
            Buoy buoy = generateRandomGene();

            if(detecta(buoy, TARGET, TipoGranada.GAE) && detecta(buoy, TARGET, TipoGranada.EXSUP)){
                return buoy;
            }
        }
    }

    private double calculateFitness(Map<String, Buoy> genes) {
        // Verifica a aptidão do indivíduo para cada tipo de granada
//        for(TipoGranada t : TipoGranada.values()){
            // Verifico a aptidão do gene do indivíduo
            for(Buoy b : genes.values()){
                try {
                    tempoDeteccao(b, TARGET);
                    raia.atualizarBoia(b.getNome(), b.getPosX(), b.getPosY(), b.getLatGeo(), b.getLonGeo(), b.getTempoDeteccao());

                } catch (Exception ex){
                    LOGGER.error("Erro ao atualizar biblioteca de boias: " + ex.getMessage());
                }
            }
             calcularPontoDeQueda();
//        }
        return custo;
    }

    private void calcularPontoDeQueda() {
        try {
            PontoQueda pontoQueda = raia.calcularPontoQueda();
            custo = pontoQueda.getMenorCusto();
            CoordenadaGeografica coordGeoPontoQueda = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(pontoQueda.getPontoDeQueda(), TARGET.getPosicao());

//            System.out.println("PONTO QUEDA: " + pontoQueda.getPontoDeQueda().getX() + " - " + pontoQueda.getPontoDeQueda().getY() + "CUSTO: " + pontoQueda.getMenorCusto());

            double distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo = Geodesics.distance(coordGeoPontoQueda.getLongitude(), coordGeoPontoQueda.getLatitude(), TARGET.getCoordenadaGeografica().getLongitude(), TARGET.getCoordenadaGeografica().getLatitude());
            double distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticasEntrePosicoesPontoQuedaEAlvo);

//            if (distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo < 9.0) {
//                System.out.println("EXATO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
//            } else if (distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo >= 9.0 && distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo <= 45.0) {
//                System.out.println("PRECISO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
//            } else {
//                System.out.println("ERRADO: " + distanciaEmMetrosEntrePosicaoPontoQuedaEAlvo);
//            }

        } catch (Exception ex) {
            System.out.println("Erro ao calcular ponto queda: " + ex.getMessage());
            LOGGER.error("Erro ao calcular ponto queda: " + ex.getMessage());
        }
    }

    private static boolean detecta(Buoy buoy, Alvo alvo, TipoGranada tipo){
        double distanciaEmMetros = calcularDistanciaEntreBoiaSplash(buoy, alvo);
        return distanciaEmMetros <= Parametros.getRaioDeDetecaoDoSplash(tipo);
    }

    private static double calcularDistanciaEntreBoiaSplash(Buoy buoy, Alvo alvo) {
        double distanciaEmMilhasNauticas = Geodesics.distance(buoy.getLonGeo(), buoy.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        return distanciaEmMetros;
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

    public Individual randomMutate(){
        Map<String, Buoy> newGenes = MutationUtils.deepCopy(genes);
        int index = rand.nextInt(genes.size());
        Buoy buoy = new ArrayList<>(genes.values()).get(index);
        Buoy newBuoy = generateRandomGene();
        newBuoy.setNome(buoy.getNome());
        newGenes.put(newBuoy.getNome(), newBuoy);

        return new Individual(newGenes);
    }

    public Individual gaussianMutation() throws Exception {
        Map<String, Buoy> copy = MutationUtils.deepCopy(genes);
        if(copy.isEmpty()){
            return this;
        }

        int index = rand.nextInt(genes.size());
        String key = new ArrayList<>(genes.keySet()).get(index);
        Buoy b = genes.get(key);

        // o sigma indica uma mudança. Valor pequeno para ajustar a posição e valor grande para grandes mudanças de posição
        double sigmaX = 2.5; // valor inicial
        double sigmaY = 2.5; // valor inicial

        if(this.getFitness() >= 8.0){
            sigmaX = 2.5 + rand.nextDouble(10.0, 52.5);
            sigmaY = 50.0 + rand.nextDouble(10.0, 52.5);
        }

        int nx = (int) Math.round(b.getPosX() + rand.nextGaussian() + sigmaX);
        int ny = (int) Math.round(b.getPosY() + rand.nextGaussian() + sigmaY);

        b.setPosX(MutationUtils.clamp(nx, 0, Raia.DIMMAX));
        b.setPosY(MutationUtils.clamp(ny, 0, Raia.DIMMAX));

        genes.put(key, b);

        return new Individual(genes);
    }

    public Individual uniformMutation() throws Exception {
        Map<String, Buoy> newGenes = MutationUtils.deepCopy(genes);
        if(newGenes.isEmpty()){
            return this;
        }

        String key = new ArrayList<>(genes.keySet()).get(rand.nextInt(genes.size()));
        Buoy b = genes.get(key);

        // Tamanho do passo a ser dados para os lados ou para cima ou para baixo
        double stepX = 1.0;
        double stepY = 1.0;

        // deslocamento horizontal da boia
        int dx = Math.toIntExact(Raia.DIMMAX - Math.round(rand.nextDouble() * Raia.DIMMAX));
        int dy = Math.toIntExact(Raia.DIMMAX - Math.round(rand.nextDouble() * Raia.DIMMAX));

        if(dx <= Raia.DIMMAX/2 && dy <= Raia.DIMMAX/2 ){
            stepX = Math.abs(rand.nextDouble() * 2 - 1);
            stepY = Math.abs(rand.nextDouble() * 2 - 1);
        }

        dx = Math.toIntExact(Math.round(dx * stepX));
        dy = Math.toIntExact(Math.round(dy * stepY));

        b.setPosX(MutationUtils.clamp(b.getPosX() + dx, 0, Raia.DIMMAX));
        b.setPosY(MutationUtils.clamp(b.getPosY() + dy, 0, Raia.DIMMAX));

        genes.put(key, b);
        return new Individual(genes);
    }

    public Individual removeBuoysMutation(){
        int quantMinBoiasRemovidas = 1;
        int quantMaxBoiasRemovidas = 2;

        Map<String, Buoy> newGenes = MutationUtils.deepCopy(genes);
        int quantBoiasIndividuo = genes.size();

        if(quantBoiasIndividuo <= Raia.QUANTMINBOIAS){
            return this;
        }

        int quantBoiasASeremRemovidas = rand.nextInt(quantMaxBoiasRemovidas - quantMinBoiasRemovidas + 1) + quantMinBoiasRemovidas;
        quantBoiasASeremRemovidas = Math.min(quantBoiasASeremRemovidas, quantBoiasIndividuo - Raia.QUANTMINBOIAS);

        List<String> keys = new ArrayList<>(genes.keySet());
        Collections.shuffle(keys, rand);
        for(int i = 0; i < quantBoiasASeremRemovidas; i++){
            genes.remove(keys.get(i));
        }

        return new Individual(genes);
    }

    public Individual addBuoysMutation() throws Exception {
        Map<String, Buoy> newGenes = MutationUtils.deepCopy(genes);
        int quantBoiasIndividuo = genes.size();
        if(quantBoiasIndividuo >= Raia.MAXBOIAS){
            return this;
        }
        int quantMinBoiasAdicionadas = 1;
        int quantMaxBoiasAdicionadas = 2;

        int quantBoiasASeremAdicionadas = rand.nextInt(quantMaxBoiasAdicionadas - quantMinBoiasAdicionadas + 1) + quantMinBoiasAdicionadas;
        quantBoiasASeremAdicionadas = Math.min(quantBoiasASeremAdicionadas, Raia.MAXBOIAS - quantBoiasIndividuo);

        Set<String> names = new HashSet<>(genes.keySet());
        final int MAX_TRIES = 1000;

        int limiteInferiorX = 0;
        int limiteInferiorY = 0;
        int limiteSuperiorX = Raia.DIMMAX;
        int limiteSuperiorY = Raia.DIMMAX;

        for(int added = 0; added < quantBoiasASeremAdicionadas; ){
            Buoy candidate = new Buoy();

            int tries = 1;
            boolean placed = false;
            while (tries++ < MAX_TRIES){

                int x = (int) (limiteInferiorX + rand.nextDouble() * (limiteSuperiorX - limiteInferiorX));
                int y = (int) (limiteInferiorY + rand.nextDouble() * (limiteSuperiorY - limiteInferiorY));

                candidate.setPosX(x);
                candidate.setPosY(y);

                if(detecta(candidate, TARGET, TipoGranada.GAE) && detecta(candidate, TARGET, TipoGranada.EXSUP)) {
                    placed = true;
                    break;
                }
                if(!placed) break;
            }

            String name = nameGenerator.nextName(names);
            names.add(name);
            candidate.setNome(name);
            genes.put(name, candidate);
            added+=1;
        }

        return new Individual(genes);
    }

    public Individual[] crossover(Individual p2) {
        Comparator<Buoy> byName = Comparator
                .comparing((Buoy b) -> b.getNome().replaceAll("\\d+$", ""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> {
                    String n = b.getNome();
                    String m = n.replaceAll("^.*?(\\d+)$", "$1");
                    return m.equals(n) ? 0 : Integer.parseInt(m);
                })
                .thenComparing(Buoy::getNome);

        List<Buoy> l1 = new ArrayList<>(this.genes.values());
        l1.sort(byName);

        List<Buoy> l2 = new ArrayList<>(p2.genes.values());
        l2.sort(byName);

        // Decide quem é o maior (A) e o menor (B)
        List<Buoy> A = l1.size() >= l2.size() ? l1 : l2;
        List<Buoy> B = l1.size() >= l2.size() ? l2 : l1;
        int targetSize = A.size();
        Random r = rand;

        // child1 começa com Todos de A, depois injeta de B por substituição se não existirem
        List<Buoy> child1 = new ArrayList<>(A); // já no tamanho máximo
        Set<String> used1 = child1.stream().map(Buoy::getNome).collect(Collectors.toSet());
        for (Buoy g : B) {
            if (!used1.contains(g.getNome())) {
                // substitui posição aleatória para manter tamanho e misturar genes
                int idx = r.nextInt(child1.size());
                used1.remove(child1.get(idx).getNome());
                child1.set(idx, g);
                used1.add(g.getNome());
            }
        }

        // child2  começa vazio, adiciona B preservando ordem, completa com genes de A únicos
        List<Buoy> child2 = new ArrayList<>(targetSize);
        Set<String> used2 = new HashSet<>();
        // adiciona B
        for (Buoy g : B) {
            if (child2.size() == targetSize) break;
            if (used2.add(g.getNome())) child2.add(g);
        }
        // completa com A até targetSize
        for (Buoy g : A) {
            if (child2.size() == targetSize) break;
            if (used2.add(g.getNome())) child2.add(g);
        }
        // se por algum motivo ainda faltou (colisões de nomes), preenche com A por substituição
        while (child2.size() < targetSize) {
            Buoy g = A.get(r.nextInt(A.size()));
            if (used2.add(g.getNome())) child2.add(g);
            else {
                int idx = r.nextInt(child2.size());
                used2.remove(child2.get(idx).getNome());
                child2.set(idx, g);
                used2.add(g.getNome());
            }
        }

        // Converte para Map (mantendo último em caso de chave duplicada)
        Map<String, Buoy> genesChild1 = child1.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a,b)->b, HashMap::new));
        Map<String, Buoy> genesChild2 = child2.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a,b)->b, HashMap::new));

        return new Individual[] { new Individual(genesChild1), new Individual(genesChild2) };
    }

}
