package ipqm.lafiaa.algoritmogenetico.domain.config;

import ipqm.lafiaa.algoritmogenetico.domain.TipoGranada;

/**
 *
 * @author Pedro Magno
 */
public class Parametros {
    public static final double RAIO_DETECCAO_GAE = 2500.0; // m
    public static final double RAIO_DETECCAO_EXSUP = 1000.0; // m
    public static final double RUIDO_TEMPO_DETECCAO = 3; // 3 ms
    public static final int QUANT_MAX_BOIAS = 5;
    public static final int QUANT_MIN_BOIAS = 3;
    public static final int DIMENSAO_RAIA = 1016;
    public static final double VELOC_SOM = 1.531;
    public static final int RESOLUCAO_GRID = 9;

    public static double getRaioDeDetecaoDoSplash(TipoGranada t) {
        return (t == TipoGranada.GAE) ? Parametros.RAIO_DETECCAO_GAE
                : Parametros.RAIO_DETECCAO_EXSUP;
    }
}