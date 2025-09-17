package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class IndividualFactory {
    private static final Random rand = new Random();

    public static Individual generateRandomIndividual() throws IOException, URISyntaxException, InterruptedException {
        Map<String, Buoy> buoys = new HashMap<>();
        int numberOfBuoys = Parametros.QUANT_MIN_BOIAS + rand.nextInt(Parametros.QUANT_MAX_BOIAS - Parametros.QUANT_MIN_BOIAS + 1);
        for(int i = 0; i < numberOfBuoys; i++) {
            Buoy buoy = GeneFactory.generateValidRandomGene();
            buoy.setNome("buoy" + (i + 1));
            buoys.put(buoy.getNome(), buoy);
        }
        return new Individual(buoys);
    }


}
