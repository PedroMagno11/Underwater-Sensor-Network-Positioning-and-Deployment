package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.TipoGranada;
import ipqm.gsa.rvt.algoritmogenetico.domain.config.Parametros;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Buoy;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.PontoQueda;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import java.util.Random;

/**
 *
 * @author Pedro Magno
 */
public class Avaliador {
    private static final Random rand = new Random();

    private static double getRaioDeDetecaoDoSplash(TipoGranada t) {
        return (t == TipoGranada.GAE) ? Parametros.RAIO_DETECCAO_GAE
                                      : Parametros.RAIO_DETECCAO_EXSUP;
    }

    // Assumimos que ele acertou. O ponto do alvo é o mesmo do splash.
    private static boolean detecta(Buoy b, Alvo alvo, TipoGranada t) {
        return Math.hypot(b.getLatGeo() - alvo.getCoordenadaGeografica().getLatitude(), b.getLonGeo() - alvo.getCoordenadaGeografica().getLongitude()) <= getRaioDeDetecaoDoSplash(t);
    }
    
    private static long toMillis(double segundos) { return Math.round(segundos * 1000.0); }
    
    private static double tempoDeteccao(Buoy b, Alvo alvo) {
        double distancia = Math.hypot(b.getLatGeo() - alvo.getCoordenadaGeografica().getLatitude(), b.getLonGeo() - alvo.getCoordenadaGeografica().getLongitude());
        double tempo = distancia / Raia.VELOCSOM;
        if (Parametros.RUIDO_TEMPO_DETECCAO > 0) tempo += rand.nextGaussian() * Parametros.RUIDO_TEMPO_DETECCAO;
        return Math.max(0.0, tempo);
    }

    /**
     * Calcula o erro médio avaliando o mesmo arranjo para ambos os tipos de granadas.
     */
    public static double erroMedioArranjo(Individual individuo, Alvo alvo) throws Exception {

        int casos = 0;
        if (individuo.getGenes().size() < 3){
            // Penaliza muuuuuuito
            individuo.updateFitness(-1e12);
        }

        if(individuo.getGenes().size() > 5){
            individuo.updateFitness(-1e12);
        }

        for (TipoGranada tipo : TipoGranada.values()) {
               int deteccoes = 0;
               for (Buoy b : individuo.getGenes()) {
                   alvo.setCoordenadaCartesianaRVT(CoordenadaCartesianaRVT.converterCoordenadaGeograficaParaCartesiana(alvo.getCoordenadaGeografica(), new CoordenadaGeografica(b.getLatGeo(), b.getLonGeo())));
                   if (!detecta(b, alvo, tipo)) continue;
                   double t = tempoDeteccao(b, alvo);
                   Raia.getRaia().atualizarBoia(b.getNome(),
                                Math.round(b.getPosX()),
                                Math.round(b.getPosY()),
                                b.getLatGeo(), b.getLonGeo(),
                                toMillis(t));
               deteccoes++;
            }
            if (deteccoes < 3) { // não dá pra triangular
                individuo.updateFitness(-1e12); // penalidade
                continue;
            }

            PontoQueda pontoDeQuedaEstimado = Raia.getRaia().calcularPontoQueda();
            // Se seu triangulador retorna (x,y), use:
            // double[] estXY = tri.estimarSplashXY();
            // double erro = Math.hypot(estXY[0] - sx, estXY[1] - sy);

            double erro = Math.hypot(pontoDeQuedaEstimado.getPontoDeQueda().getX() - alvo.getCoordenadaCartesianaRVT().getX(), pontoDeQuedaEstimado.getPontoDeQueda().getY() - alvo.getCoordenadaCartesianaRVT().getY());
            casos++;
        }
        return (casos == 0) ? 1e12 : 1;
    }
}

