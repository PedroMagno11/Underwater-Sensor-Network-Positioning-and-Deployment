package ipqm.lafiaa.algoritmogenetico.utils;

import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.TipoGranada;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.lafiaa.algoritmogenetico.utils.coord.Geodesics;

public class GeneUtils {
    public static boolean detecta(Buoy buoy, Alvo alvo, TipoGranada tipo){
        double distanciaEmMetros = calcularDistanciaEntreBoiaSplash(buoy, alvo);
        return distanciaEmMetros <= Parametros.getRaioDeDetecaoDoSplash(tipo);
    }

    private static double calcularDistanciaEntreBoiaSplash(Buoy buoy, Alvo alvo) {
        double distanciaEmMilhasNauticas = Geodesics.distance(buoy.getLonGeo(), buoy.getLatGeo(), alvo.getCoordenadaGeografica().getLongitude(), alvo.getCoordenadaGeografica().getLatitude());
        double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
        return distanciaEmMetros;
    }
}
