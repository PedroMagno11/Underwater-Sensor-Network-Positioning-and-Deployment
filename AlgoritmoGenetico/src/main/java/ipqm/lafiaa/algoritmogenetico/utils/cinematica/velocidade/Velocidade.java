package ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.velocidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Classe responsável por representar a velocidade corrente do acompanhamento em
 * diferentes tipos de percepção (quando aplicável).
 *
 * @author Pablo Rangel
 * @since 11/03/2011
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Velocidade implements Serializable {

    private VelocidadeSuperficie velocidadeSuperficie;
    private VelocidadeFundo velocidadeFundo;

    public Velocidade() {
    }

    /**
     * Construtor obrigatório da classe.
     *
     * @param velocidadeSuperficie velocidade de intenção do acompanhamento.
     * @param velocidadeFundo velocidade efetiva do acompanhamento.
     */
    public Velocidade(VelocidadeSuperficie velocidadeSuperficie, VelocidadeFundo velocidadeFundo) {
        this.velocidadeFundo = velocidadeFundo;
        this.velocidadeSuperficie = velocidadeSuperficie;
    }

    /**
     * Obtém a velocidade de superfície do acompanhamento.
     */
    @JsonIgnore
    public VelocidadeSuperficie getVelocidadeSuperficie() {
        return velocidadeSuperficie;
    }

    /**
     * Define a velocidade de superfície do acompanhamento.
     */
    synchronized public void setVelocidadeSuperficie(VelocidadeSuperficie velocidadeSuperficie) {
        this.velocidadeSuperficie = velocidadeSuperficie;
    }

    /**
     * Obtém a velocidade de fundo do acompanhamento.
     */
    @JsonIgnore
    public VelocidadeFundo getVelocidadeFundo() {
        return velocidadeFundo;
    }

    /**
     * Define a velocidade de fundo do acompanhamento.
     */
    synchronized public void setVelocidadeFundo(VelocidadeFundo velocidadeFundo) {
        this.velocidadeFundo = velocidadeFundo;
    }

    /**
     * Obtém a velocidade de fundo do acompanhamento
     * @return
     */
    public Double getFundo() {
        if (velocidadeFundo != null) {
            return velocidadeFundo.getVelocidade();
        }
        return null;
    }

    /**
     * Obtém a velocidade de superfície do acompanhamento
     * @return
     */
    public Double getSup() {
        if (velocidadeSuperficie != null) {
            return velocidadeSuperficie.getVelocidade();
        }
        return null;
    }

    /**
     * Define a velocidade de fundo do acompanhamento
     * @param fundo
     */
    public void setFundo(double fundo) {
        if (velocidadeFundo == null) {
            setVelocidadeFundo(new VelocidadeFundo());
        }
        velocidadeFundo.setVelocidade(fundo);
    }

    /**
     * Define a velocidade de superfície do acompanhamento
     * @param sup
     */
    public void setSup(double sup) {
        if (velocidadeSuperficie == null) {
            setVelocidadeSuperficie(new VelocidadeSuperficie());
        }
        velocidadeSuperficie.setVelocidade(sup);
    }
}
