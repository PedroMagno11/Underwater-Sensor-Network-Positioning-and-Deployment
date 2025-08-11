package ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.velocidade;

import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaCartesiana;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaPolar;
import java.io.Serializable;

/**
 * Abstração dos tipos de velocidades existentes.
 *
 * @author Pablo Rangel
 * @since 11/03/2011
 */
public abstract class TipoVelocidade implements Serializable{

    protected Double vx;
    protected Double vy;
    protected Double velocidade;

    public TipoVelocidade() {
        velocidade = null;
        vx = null;
        vy = null;
    }

    /**
     * Obtém a velocidade em nós (milhas náuticas por hora).
     */
    public Double getVelocidade() {
        return velocidade;
    }

    /**
     * Define a velocidade em nós (milhas náuticas por hora).
     */
    synchronized public void setVelocidade(Double velocidade) {
        if (velocidade == null) {
            velocidade = null;
        }
        this.velocidade = velocidade;
    }

    @Override
    public String toString() {
        return String.format("%.1f", velocidade) + " MN";
    }

    /**
     * Calcula a velocidade com base no vetor velocidade.
     */
    synchronized public void calcularVelocidade() {
        velocidade = Math.sqrt(Math.pow(this.getVX(), 2) + Math.pow(this.getVY(), 2));
    }

    /**
     * Decompõe a velocidade em um vetor velocidade.
     */
    synchronized public void decomporVelocidade(Double rumo) throws Exception {
        CoordenadaPolar cp = new CoordenadaPolar();
        cp.setDistancia(velocidade);
        cp.setMarcacao(rumo);
        CoordenadaCartesiana ccr = new CoordenadaCartesiana();
        ccr.setX(0);
        ccr.setY(0);
        if (velocidade >= 0) {
            try {
                CoordenadaCartesiana cc = CoordenadaCartesiana.converterCoordenadaPolar(ccr, cp);
                vx = cc.getX();
                vy = cc.getY();
            } catch (Exception e) {
                vx = Double.NaN;
                vy = Double.NaN;
                throw e;
            }
        } else {
            vx = Double.NaN;
            vy = Double.NaN;
        }
    }

    /**
     * Obtém o valor da abscissa do vetor velocidade.
     */
    public Double getVX() {
        return vx;
    }

    /**
     * Define o valor da abscissa do vetor velocidade.
     */
    synchronized public void setVX(Double vx) {
        this.vx = vx;
    }

    /**
     * Obtém o valor da ordenada do vetor velocidade.
     */
    public Double getVY() {
        return vy;
    }

    /**
     * Define o valor da ordenada do vetor velocidade.
     */
    synchronized public void setVY(Double vy) {
        this.vy = vy;
    }

}
