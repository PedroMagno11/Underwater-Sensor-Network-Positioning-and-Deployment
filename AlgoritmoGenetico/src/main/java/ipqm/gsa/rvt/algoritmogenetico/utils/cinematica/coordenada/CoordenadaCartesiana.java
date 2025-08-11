package ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada;


import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.velocidade.VelocidadeFundo;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.GeoCoord;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Mercator;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.XYCoord;

import java.time.Instant;
import java.util.Locale;

/**
 * Classe responsável por representar a posição corrente do acompanhamento no
 * plano cartesiano.
 *
 * @author Pablo Rangel
 * @author Medeiros
 * @since 11/03/2011
 */
public class CoordenadaCartesiana extends TipoCoordenada {

    private double x;
    private double y;

    /**
     * Construtor sem parâmetros.
     */
    public CoordenadaCartesiana() {
        x = 0.0;
        y = 0.0;
    }

    /**
     * Construtor com parâmetros.
     *
     * @param x valor da abscissa.
     * @param y valor da ordenada.
     */
    public CoordenadaCartesiana(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Método de conversão de coordenada geográfica para coordenada cartesiana.
     * Dado a latitude e a longitude de um ponto, este método retorna a
     * coordenada cartesiana deste ponto. Este método utiliza a biblioteca PROJ4
     * para conversão. A resposta da biblioteca PROJ4 é em metros, mas o
     * resultado final deste método é dado em milhas náuticas.
     *
     * @param coordenadaGeografica CoordenadaGeografica coordenada geografica
     * (latitude e longitude) do ponto que deseja se converter para coordenada
     * cartesiana.
     * @return CoordenadaCartesiana coordenada cartesiana (x,y) convertida.
     */
    public static CoordenadaCartesiana converterCoordenadaGeografica(CoordenadaGeografica coordenadaGeografica) {
        if (coordenadaGeografica == null) {
            return null;
        }
        XYCoord cc = Mercator.toXY(coordenadaGeografica.getLongitude(), coordenadaGeografica.getLatitude());
        return new CoordenadaCartesiana(cc.getX(), cc.getY());
    }

    /**
     * Método de conversão de coordenada polar para coordenada cartesiana. Dado
     * a marcação e a distãncia de um ponto (coordenadaPolarInteresse), este
     * método retorna a coordenada cartesiana deste ponto. O ponto cartesiano
     * consiste nos catetos do triângulo formado pela reta e sua inclinação no
     * plano.
     *
     * @param coordenadaCartesianaReferencial CoordenadaCartesiana coordenada
     * cartesiana de referência (x,y) do ponto que se deseja transladar a
     * posição, cuja distância relativa da coordenada polar se refere.
     * @param coordenadaPolarInteresse CoordenadaPolar coordenada polar do ponto
     * que se deseja obter a coordenada cartesiana.
     * @return CoordenadaCartesiana coordenada cartesiana (x,y) convertida.
     */
    public static CoordenadaCartesiana converterCoordenadaPolar(CoordenadaCartesiana coordenadaCartesianaReferencial,
            CoordenadaPolar coordenadaPolarInteresse) {
        if (coordenadaCartesianaReferencial == null) {
            return null;
        }
        if (coordenadaPolarInteresse == null) {
            return null;
        }

        CoordenadaGeografica refGeo = CoordenadaGeografica.converterCoordenadaCartesiana(coordenadaCartesianaReferencial);
        CoordenadaGeografica destGeo = CoordenadaGeografica.converterCoordenadaPolar(refGeo, coordenadaPolarInteresse);
        return CoordenadaCartesiana.converterCoordenadaGeografica(destGeo);
    }

    /**
     * Método de conversão de distancia Euclidiana para coordenada cartesiana.
     * Dado duas distancias Euclidianas em X e em Y (distanciaX e distanciaY) e
     * uma coordenada cartesiana referencial, este método retorna a coordenada
     * cartesiana deste ponto após aplicar o deslocamento solicitado.
     *
     * @param coordenadaCartesianaReferencial CoordenadaCartesiana coordenada
     * cartesiana de referência (x,y) do ponto que se deseja transladar a
     * posição.
     * @param distanciaX Distância Euclidiana na horizontal (marcação 90°).
     * @param distanciaY Distância Euclidiana na vertical (marcação 0°).
     * @return CoordenadaCartesiana coordenada cartesiana (x,y) convertida.
     */
    public static CoordenadaCartesiana converterDistanciaXY(CoordenadaCartesiana coordenadaCartesianaReferencial,
            double distanciaX, double distanciaY) {
        double marcacao = Math.toDegrees(Math.atan2(distanciaX, distanciaY));
        double distancia = Math.hypot(distanciaX, distanciaY);
        CoordenadaPolar coordenadaPolarInteresse = new CoordenadaPolar(marcacao, distancia);
        return CoordenadaCartesiana.converterCoordenadaPolar(coordenadaCartesianaReferencial, coordenadaPolarInteresse);
    }

    /**
     * Este método calcula a distancia entre dois pontos utilizando o método de
     * Vicenty
     *
     * @param coordenadaCartesiana1 coordenada cartesiana do ponto 1.
     * @param coordenadaCartesiana2 coordenada cartesiana do ponto 2.
     * @return double distância em milhas náuticas.
     * @see #calcularDistancia(double, double, double, double)
     */
    public static double calcularDistancia(CoordenadaCartesiana coordenadaCartesiana1, CoordenadaCartesiana coordenadaCartesiana2) {
        if (coordenadaCartesiana1 == null || coordenadaCartesiana2 == null) {
            return Double.NaN;
        }

        GeoCoord cg1 = Mercator.toGeo(coordenadaCartesiana1.getX(), coordenadaCartesiana1.getY());
        GeoCoord cg2 = Mercator.toGeo(coordenadaCartesiana2.getX(), coordenadaCartesiana2.getY());

        return CoordenadaGeografica.calcularDistancia(
                cg1.getLatitude(), cg1.getLongitude(),
                cg2.getLatitude(), cg2.getLongitude()
        );
    }

    /**
     * Este método calcula a distancia entre dois pontos utilizando o método de
     * Vicenty
     *
     * @param x1 abscissa do ponto 1.
     * @param y1 ordenada do ponto 1.
     * @param x2 abscissa do ponto 2.
     * @param y2 ordenada do ponto 2.
     * @return double distância em milhas náuticas.
     */
    public static double calcularDistancia(double x1, double y1, double x2, double y2) {
        return CoordenadaCartesiana.calcularDistancia(new CoordenadaCartesiana(x1, y1), new CoordenadaCartesiana(x2, y2));
    }

    /**
     * Método para estimar a posição de um acompanhamento. S = S0 + V.T, onde: S
     * (x,y) -> posição atual em milhas náuticas em coordenadas cartesianas;
     * S0(x,y) -> posição inicial em milhas náuticas em coordenadas cartesianas;
     * V (x,y) -> velocidade do alvo em milha náuticas por segundo em
     * coordenadas cartesianas; T -> tempo decorrido (em segundos) entre a
     * última atualização e o momento atual.
     *
     * @param posicaoAtual posicao atual do acompanhamento.
     * @param tempoInicial timestamp da última posição em segundos.
     * @param velocidade velocidade em nós do objeto a se estimar. Assume-se que
     * a velocidade já está decomposta.
     * @return CoordenadaCartesiana coordenada cartesiana da nova posição.
     */
    public static CoordenadaCartesiana obterPosicaoEstimada(CoordenadaCartesiana posicaoAtual, long tempoInicial,
            VelocidadeFundo velocidade) {
        if (posicaoAtual == null) {
            return null;
        }
        if (velocidade == null) {
            return null;
        }
        //De milisegundos -> segundos.
        long timeStamp = Instant.now().getEpochSecond();

        double t = Math.abs(timeStamp - tempoInicial);
        double sx = posicaoAtual.getX() + (velocidade.getVX() / 3600) * t;
        double sy = posicaoAtual.getY() + (velocidade.getVY() / 3600) * t;
        CoordenadaCartesiana novaPosicaoCartesiana = new CoordenadaCartesiana();
        novaPosicaoCartesiana.setX(sx);
        novaPosicaoCartesiana.setY(sy);
        return novaPosicaoCartesiana;
    }

    /**
     * Método para estimar a posição de um acompanhamento com base no tempo
     * final. S = S0 + V.T, onde: S (x,y) -> posição atual em milhas náuticas em
     * coordenadas cartesianas; S0(x,y) -> posição inicial em milhas náuticas em
     * coordenadas cartesianas; V (x,y) -> velocidade do alvo em milha náuticas
     * por segundo em coordenadas cartesianas; T -> tempo decorrido (em
     * segundos) entre a última atualização e o momento atual.
     *
     * @param posicaoAtual posicao atual do acompanhamento.
     * @param tempoFinal tempo que será calculada a estimativa.
     * @param velocidade velocidade em nós do objeto a se estimar. Assume-se que
     * a velocidade já está decomposta.
     * @return CoordenadaCartesiana coordenada cartesiana da nova posição.
     */
    public static CoordenadaCartesiana obterPosicaoEstimada(double tempoFinal, CoordenadaCartesiana posicaoAtual,
            VelocidadeFundo velocidade) {
        if (posicaoAtual == null) {
            return null;
        }
        if (velocidade == null) {
            return null;
        }
        //De milisegundos -> segundos.

//        tempoFinal *= 3600;
        double sx = posicaoAtual.getX() + (velocidade.getVX() / 3600) * tempoFinal;
        double sy = posicaoAtual.getY() + (velocidade.getVY() / 3600) * tempoFinal;
        CoordenadaCartesiana novaPosicaoCartesiana = new CoordenadaCartesiana();
        novaPosicaoCartesiana.setX(sx);
        novaPosicaoCartesiana.setY(sy);
        return novaPosicaoCartesiana;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof CoordenadaCartesiana) {
            CoordenadaCartesiana cc = (CoordenadaCartesiana) o;

            if (cc.getClass().equals(getClass())
                    && cc.hashCode() == hashCode()
                    && cc.getX() == getX() && cc.getY() == getY()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Define o valor da abscissa no plano cartesiano.
     */
    public double getX() {
        return x;
    }

    /**
     * Obtém o valor da abscissa no plano cartesiano.
     */
    synchronized public void setX(double x) {
        this.x = x;
    }

    /**
     * Obtém o valor da ordenada no plano cartesiano.
     */
    public double getY() {
        return y;
    }

    /**
     * Define o valor da ordenada no plano cartesiano.
     */
    synchronized public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%-10.2f %-10.2f MN", getX(), getY());
    }

}

