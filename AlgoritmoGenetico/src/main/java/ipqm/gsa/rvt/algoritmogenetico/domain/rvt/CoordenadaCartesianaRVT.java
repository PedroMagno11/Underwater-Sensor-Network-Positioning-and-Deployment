package ipqm.gsa.rvt.algoritmogenetico.domain.rvt;

import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaPolar;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.Posicao;
import ipqm.gsa.rvt.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.GeoCoord;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Geodesics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordenadaCartesianaRVT{
    private final static Logger LOGGER = LoggerFactory.getLogger("CoordenadaCartesianaRVT");

    private int x;
    private int y;

    /**
     * Cria um ponto na coordenada cartesiana bidimensional [x,y]
     *
     * @param x - coordenada x do ponto
     * @param y - coordenada y do ponto
     */
    public CoordenadaCartesianaRVT(int x, int y) { // cria coordenada 
        this.x = x;
        this.y = y;
    }

    /**
     * retorna a coordenada x do ponto
     *
     * @return - coordenada x do ponto
     */
    public int getX() {
        return x;
    }

    /**
     * retorna a coordenada y do ponto
     *
     * @return - coordenada y do ponto
     */
    public int getY() {
        return y;
    }

    /**
     * altera a coordenada x do ponto
     *
     * @param x - coordenada x do ponto
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * altera a coordenada y do ponto
     *
     * @param y - coordenada y do ponto
     */
    public void setY(int y) {
        this.y = y;
    }

    public static CoordenadaCartesianaRVT converterCoordenadaGeograficaParaCartesiana(CoordenadaGeografica coordenadaGeograficaInteresse, CoordenadaGeografica coordenadaGeograficaReferencial) {

        int posXAlvo = Raia.DIMMAX / 2;
        int posYAlvo = Raia.DIMMAX / 2;

        /*Calculando a distancia deste modo aumenta a precisão do resultado*/
        double distXMN = CoordenadaGeografica.calcularDistancia(coordenadaGeograficaReferencial.getLatitude(), coordenadaGeograficaReferencial.getLongitude(),
                coordenadaGeograficaReferencial.getLatitude(), coordenadaGeograficaInteresse.getLongitude());
        double distYMN = CoordenadaGeografica.calcularDistancia(coordenadaGeograficaReferencial.getLatitude(), coordenadaGeograficaReferencial.getLongitude(),
                coordenadaGeograficaInteresse.getLatitude(), coordenadaGeograficaReferencial.getLongitude());

        double distXmetros = ConversorUnidades.milhasNauticasParaMetros(distXMN);
        double distYmetros = ConversorUnidades.milhasNauticasParaMetros(distYMN);

        int distXGrid = Math.round((float) (distXmetros / Raia.RESOLUCAOGRID));
        int distYGrid = Math.round((float) (distYmetros / Raia.RESOLUCAOGRID));

        
        int x;
        int y;
        CoordenadaPolar cp = CoordenadaPolar.converterCoordenadaGeografica(coordenadaGeograficaReferencial, coordenadaGeograficaInteresse);
        if (cp.getMarcacao() < 180) {
            x = posXAlvo + distXGrid;
        } else {
            x = posXAlvo - distXGrid;
        }

        if (cp.getMarcacao() > 90 && cp.getMarcacao() < 270) {
            y = posYAlvo + distYGrid;
        } else {
            y = posYAlvo - distYGrid;
        }

        return new CoordenadaCartesianaRVT(x, y);
    }

    public static CoordenadaGeografica converterCoordenadaCartesianaParaGeografica(CoordenadaCartesianaRVT CoordCartesianaInteresse, Posicao posicaoReferencial) {

        int posXAlvo = Raia.DIMMAX / 2;
        int posYAlvo = Raia.DIMMAX / 2;

        double distXmetros = Math.abs(posXAlvo - CoordCartesianaInteresse.getX()) * Raia.RESOLUCAOGRID;
        double distYmetros = Math.abs(posYAlvo - CoordCartesianaInteresse.getY()) * Raia.RESOLUCAOGRID;

        double distXMN = ConversorUnidades.metrosParaMilhas(distXmetros);
        double distYMN = ConversorUnidades.metrosParaMilhas(distYmetros);

        if (CoordCartesianaInteresse.getX() < posXAlvo) {
            distXMN *= -1;
        }
        if (CoordCartesianaInteresse.getY() > posYAlvo) {
            distYMN *= -1;
        }
        
        LOGGER.info("SPLASH (CALCULADO)-> Coordenada Cartesiana X (relativa ao Alvo Teórico) em metros: " + distXmetros + " metros ", CoordCartesianaInteresse.getClass());
        LOGGER.info("SPLASH (CALCULADO)-> Coordenada Cartesiana Y (relativa ao Alvo Teórico) em metros: " + distYmetros + " metros ", CoordCartesianaInteresse.getClass());

        double distance = Math.sqrt(distXMN * distXMN + distYMN * distYMN);
        double bearing = Math.toDegrees(Math.atan2(distXMN, distYMN));
        if (bearing > 360.0) {
            bearing -= 360.0;
        }

        
        
        LOGGER.info("SPLASH (CALCULADO)-> Distância (relativa ao Alvo Teórico): " + ConversorUnidades.milhasNauticasParaMetros(distance) + " metros (" + distance + " MN)", CoordCartesianaInteresse.getClass());

        GeoCoord geoCord = Geodesics.destination(posicaoReferencial.getCoordenadaGeografica().getLongitude(), posicaoReferencial.getCoordenadaGeografica().getLatitude(), bearing, distance);
        double distanciaGeodesics = Geodesics.distance(geoCord.getLongitude(), geoCord.getLatitude(), posicaoReferencial.getCoordenadaGeografica().getLongitude(), posicaoReferencial.getCoordenadaGeografica().getLatitude());

        LOGGER.info("SPLASH (CALCULADO)-> Distância (relativa ao Alvo Teórico) (GEODESICS): " + ConversorUnidades.milhasNauticasParaMetros(distanciaGeodesics), CoordCartesianaInteresse.getClass());

        return (new CoordenadaGeografica(geoCord.getLatitude(), geoCord.getLongitude()));
    }

    @Override
    public String toString() {
        return "X: " + x + "  Y: " + y;
    }   
}