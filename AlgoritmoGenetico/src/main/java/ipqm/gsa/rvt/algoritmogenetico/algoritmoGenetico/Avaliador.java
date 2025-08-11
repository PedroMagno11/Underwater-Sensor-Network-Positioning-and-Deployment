package ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.TipoGranada;
import ipqm.gsa.rvt.algoritmogenetico.domain.config.Parametros;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Boia;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.PontoCalculado;
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
    
    private static boolean detectar(Boia b, CoordenadaCartesianaRVT splash, TipoGranada t) {
        return Math.hypot(b.getPosX() - splash.getX(), b.getPosY() - splash.getY()) <= getRaioDeDetecaoDoSplash(t);
    }
    
    private static long toMillis(double segundos) { return Math.round(segundos * 1000.0); }
    
    private static double tempoDeteccao(Boia b, double sx, double sy) {
        double d = Math.hypot(b.getPosX() - sx, b.getPosY() - sy);
        double t = d / Raia.VELOCSOM;
        if (Parametros.RUIDO_TEMPO_DETECCAO > 0) t += rand.nextGaussian() * Parametros.RUIDO_TEMPO_DETECCAO;
        return Math.max(0.0, t);
    }

    /** Erro médio (m) avaliando o mesmo arranjo para ambos os tipos de granadas. */
    public static double erroMedioArranjo(Individual individuo, Alvo alvo) throws Exception {
        if (individuo.getGenes().size() < 3){
            // Penaliza muuuuuuito
            individuo.setFitness(1e12);
        }

        double soma = 0.0; int casos = 0;


        // garantir lat/lon das boias coerente com x,y do ref local (p/ alimentar a raia)
        for (Boia b : individuo.getGenes()) {
           CoordenadaGeografica coordGeoBoia = CoordenadaGeografica.converterDistanciaXY(alvo.getCoordGeo(), b.getPosX(), b.getPosY());
           b.setLatGeo(coordGeoBoia.getLatitude()); 
           b.setLonGeo(coordGeoBoia.getLongitude());
        }

        CoordenadaGeografica coordBoia = new CoordenadaGeografica(individuo.getGenes().get(rand.nextInt(individuo.getGenes().size())).getLatGeo(), individuo.getGenes().get(rand.nextInt(individuo.getGenes().size())).getLonGeo());
        CoordenadaCartesianaRVT coordRVTAlvo = CoordenadaCartesianaRVT.converterCoordenadaGeograficaParaCartesiana(alvo.getCoordGeo(), coordBoia);

        for (TipoGranada tipo : TipoGranada.values()) {
               int det = 0;
               for (Boia b : individuo.getGenes()) {
               if (!detectar(b, coordRVTAlvo, tipo)) continue;
               double t = tempoDeteccao(b, coordRVTAlvo.getX(), coordRVTAlvo.getY());
               Raia.getRaia().atualizarBoia(b.getNome(),
                                Math.round(b.getPosX()),
                                Math.round(b.getPosY()),
                                b.getLatGeo(), b.getLonGeo(),
                                toMillis(t));
               det++;
            }
            if (det < 3) { // não dá pra triangular
                soma += 1e6; casos++; // penalidade
                continue;
            }

            PontoCalculado pontoDeQuedaEstimado = Raia.getRaia().calcularPontoQueda(); 
            // Se seu triangulador retorna (x,y), use:
            // double[] estXY = tri.estimarSplashXY();
            // double erro = Math.hypot(estXY[0] - sx, estXY[1] - sy);

            double erro = Math.hypot(pontoDeQuedaEstimado.getPontoDeQueda().getX() - coordRVTAlvo.getX(), pontoDeQuedaEstimado.getPontoDeQueda().getY() - coordRVTAlvo.getY());
            soma += erro; casos++;
        }
        return (casos == 0) ? 1e12 : soma / casos;
    }
}

