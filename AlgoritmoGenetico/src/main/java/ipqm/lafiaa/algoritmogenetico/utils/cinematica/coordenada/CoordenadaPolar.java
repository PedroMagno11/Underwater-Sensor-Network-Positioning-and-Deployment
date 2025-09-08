package ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada;


import java.util.Locale;

/**
 * Classe responsável por representar a posição corrente do acompanhamento em
 * termos de marcação e distância.
 *
 * @author Pablo Rangel
 * @author Medeiros
 * @since 11/03/2011
 */
public class CoordenadaPolar extends TipoCoordenada {

    private double marcacao;
    private double distancia;

    /**
     * Construtor sem parâmetros.
     */
    public CoordenadaPolar() {
        this.marcacao = 0;
        this.distancia = 0;
    }

    /**
     * Construtor com parâmetros.
     *
     * @param marcacao marcação relativa em graus da coordenada polar.
     * @param distancia distancia relativa em milhas náuticas da coordenada
     * polar.
     */
    public CoordenadaPolar(double marcacao, double distancia) {
        this.marcacao = marcacao;
        this.distancia = distancia;
    }

    /**
     * Método de conversão de coordenada cartesiana para coordenada polar. Dado
     * um ponto cartesiano como referencial (coordenadaCartesianaReferencial),
     * este método retorna a marcação e a distância (coordenada polar) de uma
     * coordenada cartesiana em ponto qualquer (coordenadaCartesianaInteresse).
     * A distância é calculada com a fórmula da distância euclidiana. A marcação
     * consiste no ângulo formado entre a reta que parte do ponto referencial
     * até o ponto de interesse.
     *
     * @param coordenadaCartesianaReferencial coordenada cartesiana de
     * referência.
     * @param coordenadaCartesianaInteresse coordenada cartesiana que se deseja
     * converter.
     * @return CoordenadaPolar coordenada cartesiana de interesse convertida em
     * coordenada polar.
     */
    public static CoordenadaPolar converterCoordenadaCartesiana(CoordenadaCartesiana coordenadaCartesianaReferencial,
            CoordenadaCartesiana coordenadaCartesianaInteresse) {
        if (coordenadaCartesianaReferencial == null || coordenadaCartesianaInteresse == null) {
            return null;
        }
        double marcacao = calcularMarcacao(coordenadaCartesianaReferencial, coordenadaCartesianaInteresse);
        double distancia = CoordenadaCartesiana.calcularDistancia(coordenadaCartesianaReferencial, coordenadaCartesianaInteresse);

        return new CoordenadaPolar(marcacao, distancia);
    }

    /**
     * Método de conversão de coordenada geográfica para coordenada polar. Dado
     * a latitude e a longitude de um ponto de interesse
     * (coordenadaGeograficaInteresse) e uma latitude e longitude referencial
     * (coordenadaGeograficaReferencial), este método retorna a coordenada polar
     * deste ponto.
     *
     * @param coordenadaGeograficaReferencial coordenada geográfica de
     * referência.
     * @param coordenadaGeograficaInteresse coordenada geográfica que se deseja
     * converter
     * @return CoordenadaPolar coordenada geográfica de interesse convertida em
     * coordenada polar.
     */
    public static CoordenadaPolar converterCoordenadaGeografica(CoordenadaGeografica coordenadaGeograficaReferencial,
            CoordenadaGeografica coordenadaGeograficaInteresse) {
        if (coordenadaGeograficaReferencial == null || coordenadaGeograficaInteresse == null) {
            return null;
        }
        double distancia = CoordenadaGeografica.calcularDistancia(coordenadaGeograficaReferencial, coordenadaGeograficaInteresse);
        double marcacao = calcularMarcacao(coordenadaGeograficaReferencial, coordenadaGeograficaInteresse);

        return new CoordenadaPolar(marcacao, distancia);
    }

    public static double calcularMarcacao(CoordenadaCartesiana coordenadaCartesianaReferencial,
        CoordenadaCartesiana coordenadaCartesianaInteresse) {
        double dx = coordenadaCartesianaInteresse.getX() - coordenadaCartesianaReferencial.getX();
        double dy = coordenadaCartesianaInteresse.getY() - coordenadaCartesianaReferencial.getY();

        double marcacao = Math.toDegrees(Math.atan2(dx, dy));
        if (marcacao < 0) {
            marcacao += 360.0;
        }
        return marcacao;
    }

    public static double calcularMarcacao(CoordenadaGeografica coordenadaGeograficaReferencial,
            CoordenadaGeografica coordenadaGeograficaInteresse) {
        CoordenadaCartesiana coordenadaCartesianaReferencial = CoordenadaCartesiana.
                converterCoordenadaGeografica(coordenadaGeograficaReferencial);
        CoordenadaCartesiana coordenadaCartesianaInteresse = CoordenadaCartesiana.
                converterCoordenadaGeografica(coordenadaGeograficaInteresse);
        return calcularMarcacao(coordenadaCartesianaReferencial, coordenadaCartesianaInteresse);
    }

    /**
     * Obtém a marcação do acompanhamento em graus. Sempre relativa.
     */
    public double getMarcacao() {
        return marcacao;
    }

    /**
     * Define a marcação do acompanhamento em graus. Sempre relativa.
     */
    synchronized public void setMarcacao(double marcacao) {
        this.marcacao = marcacao;
    }

    /**
     * Obtém a distância do acompanhamento em milhas náuticas. Sempre relativa.
     */
    public double getDistancia() {
        return distancia;
    }

    /**
     * Define a distância do acompanhamento em milhas náuticas. Sempre relativa.
     *
     * @param distancia
     */
    synchronized public void setDistancia(double distancia) {
        this.distancia = distancia;
    }

    @Override
    public String toString() {
        String grau = "ᵒ";

        long marcacaoCorrigida = Math.round(getMarcacao());
        if (marcacaoCorrigida >= 360) {
            marcacaoCorrigida -= 360;
        }

        return String.format(Locale.US, "%03d" + grau + " %.2f " + "MN",
                marcacaoCorrigida, getDistancia());

    }

}
