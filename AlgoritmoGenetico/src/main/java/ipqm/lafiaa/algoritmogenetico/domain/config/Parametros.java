package ipqm.lafiaa.algoritmogenetico.domain.config;

import ipqm.lafiaa.algoritmogenetico.domain.TipoGranada;

/**
 *
 * @author Pedro Magno
 */
public class Parametros {
    public static final double RAIO_DETECCAO_GAE = 2500.0; // m
    public static final double RAIO_DETECCAO_EXSUP = 1000.0; // m
    public static final double RUIDO_TEMPO_DETECCAO = 0.0003; // 0.3 ms

    public static double getRaioDeDetecaoDoSplash(TipoGranada t) {
        return (t == TipoGranada.GAE) ? Parametros.RAIO_DETECCAO_GAE
                : Parametros.RAIO_DETECCAO_EXSUP;
    }

}



//// Boia.java
//public class Boia {
//    public final String nome;
//    public double x, y;     // metros (no ref local)
//    public double lat, lon; // graus (alimentar seu triangulador)
//
//    public Boia(String nome, double x, double y) {
//        this.nome = nome; this.x = x; this.y = y;
//    }
//}

//// Alvo.java
//public class Alvo {
//    public final double lat, lon; // graus
//    public Alvo(double lat, double lon) { this.lat = lat; this.lon = lon; }
//}

//// Parametros.java
//public class Parametros {
//    public static final double V_SOM = 1503.0;     // m/s
//    public static final double RAIO_DETECCAO_GAE   = 4500.0; // m (maior)
//    public static final double RAIO_DETECCAO_EXSUP = 1500.0; // m (menor)
//    public static final double NOISE_TOA_STD_S = 0.0;        // ruído opcional (0 = off)
//    public static final int    BOIAS_MIN = 3, BOIAS_MAX = 5;
//    public static final double AREA_X = 9000, AREA_Y = 9000; // caixa de busca do GA (m)
//}
