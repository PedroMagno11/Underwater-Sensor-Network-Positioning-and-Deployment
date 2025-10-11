package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.lafiaa.algoritmogenetico.utils.coord.Geodesics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class IndividualFactory {
    private static final Random rand = new Random();

    public static Individual generateRandomIndividual() throws IOException, URISyntaxException, InterruptedException {
        Map<String, Buoy> buoys = new HashMap<>();
        int numberOfBuoys = Parametros.QUANT_MIN_BOIAS + rand.nextInt(Parametros.QUANT_MAX_BOIAS - Parametros.QUANT_MIN_BOIAS + 1);
//        int numberOfBuoys = 3;
        for(int i = 0; i < numberOfBuoys; i++) {
            Buoy buoy = GeneFactory.generateValidRandomGene();
            buoy.setNome("buoy" + (i + 1));
            buoys.put(buoy.getNome(), buoy);
        }
        return new Individual(buoys);
    }

    public static Individual generateCircularIndividual() throws IOException, URISyntaxException, InterruptedException {
        Map<String, Buoy> buoys = new HashMap<>();
//        int numberOfBuoys = Parametros.QUANT_MIN_BOIAS + rand.nextInt(Parametros.QUANT_MAX_BOIAS - Parametros.QUANT_MIN_BOIAS + 1);
        int numberOfBuoys = 5;
        double centerX = Parametros.DIMENSAO_RAIA / 2.0;
        double centerY = Parametros.DIMENSAO_RAIA / 2.0;

        double jitter = ThreadLocalRandom.current().nextDouble(-2,2); // pequena variação no raio

        double radius = rand.nextDouble(276) + jitter; // Ex: centro da raia [508,508] + 100 = [608, 608]. OBS: O máximo é 278

        double angleOffset = ThreadLocalRandom.current().nextDouble(0,2 * Math.PI);

        for(int i = 0; i < numberOfBuoys; i++) {

            double angle = angleOffset + 2 * Math.PI * i / numberOfBuoys;

            // posição circular com pequeno ruído
//            double posX = centerX + radius * Math.cos(angle) + (rand.nextDouble() - 0.5) * 0.05 * radius;
//            double posY = centerY + radius * Math.sin(angle) + (rand.nextDouble() - 0.5) * 0.05 * radius;

            double posX = centerX + radius * Math.cos(angle);
            double posY = centerY + radius * Math.sin(angle);

            // garante limites dentro da raia
            posX = Math.max(0, Math.min(Math.round(posX), Parametros.DIMENSAO_RAIA - 1));
            posY = Math.max(0, Math.min(Math.round(posY), Parametros.DIMENSAO_RAIA - 1));

            // cria boia
            Buoy buoy = new Buoy("buoy" + (i + 1), (int) posX, (int) posY);

            // calcula coordenadas geográficas e tempo de detecção (igual ao generateValidRandomGene)
            var coordGeoBoia = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(
                    new CoordenadaCartesianaRVT(buoy.getPosX(), buoy.getPosY()),
                    Alvo.getInstance().getPosicao()
            );
            buoy.setLatGeo(coordGeoBoia.getLatitude());
            buoy.setLonGeo(coordGeoBoia.getLongitude());

            // reutiliza cálculo de tempo de detecção do GeneFactory
            var alvo = Alvo.getInstance();
            double distanciaNm = Geodesics.distance(
                    buoy.getLonGeo(), buoy.getLatGeo(),
                    alvo.getCoordenadaGeografica().getLongitude(),
                    alvo.getCoordenadaGeografica().getLatitude()
            );
            double distanciaM = ConversorUnidades.milhasNauticasParaMetros(distanciaNm);
            double tempo = distanciaM / Parametros.VELOC_SOM;
            if (rand.nextDouble() < 0.3)
                tempo += rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
            else if (rand.nextDouble() > 0.7)
                tempo -= rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
            buoy.setTempoDeteccao(Math.max(0, Math.round(tempo)));

            buoys.put(buoy.getNome(), buoy);

        }

        return new Individual(buoys);
    }

}
