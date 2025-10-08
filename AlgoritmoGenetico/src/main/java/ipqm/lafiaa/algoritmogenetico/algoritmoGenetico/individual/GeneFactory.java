package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.TipoGranada;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.lafiaa.algoritmogenetico.utils.GeneUtils;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.lafiaa.algoritmogenetico.utils.coord.Geodesics;

import java.util.Random;

public class GeneFactory {

    private static final Alvo TARGET = Alvo.getInstance();

    public static final Random rand = new Random();

    private static Buoy generateRandomGene(){
        int posX = rand.nextInt(Parametros.DIMENSAO_RAIA);
        int posY = rand.nextInt(Parametros.DIMENSAO_RAIA);
        Buoy buoy = new Buoy("", posX, posY);
        CoordenadaGeografica coordGeoBoia = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(new CoordenadaCartesianaRVT(buoy.getPosX(), buoy.getPosY()), TARGET.getPosicao());
        buoy.setLatGeo(coordGeoBoia.getLatitude());
        buoy.setLonGeo(coordGeoBoia.getLongitude());

        return buoy;
    }

    private static void tempoDeteccao(Buoy b, Alvo alvo) {
        double distanciaEmMilhasNauticas = Geodesics.distance(b.getLonGeo(), b.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        double tempo = distanciaEmMetros / Parametros.VELOC_SOM;

        // De maneira aleatória, adiciona um atraso na detecção de até 3 ms
        if(rand.nextDouble() < 0.3){
            tempo += rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
        }else if (rand.nextDouble() > 0.7){
            tempo -= rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
        }

        long t = Math.max(0, Math.round(tempo));
        b.setTempoDeteccao(t);
    }

    public static Buoy generateValidRandomGene(){
        while (true){
            Buoy buoy = generateRandomGene();
            if(GeneUtils.detecta(buoy, TARGET, TipoGranada.GAE) && GeneUtils.detecta(buoy, TARGET, TipoGranada.EXSUP)){
                tempoDeteccao(buoy, TARGET);
                return buoy;
            }
        }
    }
}
