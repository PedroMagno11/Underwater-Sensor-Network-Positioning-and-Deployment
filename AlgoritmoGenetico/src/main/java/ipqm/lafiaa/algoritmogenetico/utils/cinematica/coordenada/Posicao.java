package ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Classe responsável por representar a posição corrente do acompanhamento em
 * diferentes tipos de coordenada (quando aplicável).
 *
 * @author Pablo Rangel
 * @since 11/03/2011
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Posicao implements Cloneable, Serializable {

    private CoordenadaGeografica coordenadaGeografica;
    private CoordenadaPolar coordenadaPolar;
    private CoordenadaCartesiana coordenadaCartesiana;

    private Double precisao;

    public Posicao() {
    }

    /**
     * Construtor obrigatório da classe.
     *
     * @param coordenadaGeografica coordenada geográfica (latitude e longitude)
     * da posição.
     * @param coordenadaPolar coordenada polar (marcação e distância) da
     * posição.
     * @param coordenadaCartesiana coordenada cartesiana (x,y) em milhas
     * náuticas da posição.
     */
    public Posicao(CoordenadaGeografica coordenadaGeografica, CoordenadaPolar coordenadaPolar,
            CoordenadaCartesiana coordenadaCartesiana) {
        this.coordenadaCartesiana = coordenadaCartesiana;
        this.coordenadaGeografica = coordenadaGeografica;
        this.coordenadaPolar = coordenadaPolar;
    }

    /**
     * Obtem a posicao calculada atraves da posicao de uma coordenada geografica
     * e da posicao referencial.
     *
     * @param cg
     * @param referencial
     * @return
     */
    public static Posicao gerarPosicao(CoordenadaGeografica cg, CoordenadaCartesiana referencial) {

        CoordenadaCartesiana cc = CoordenadaCartesiana.converterCoordenadaGeografica(cg);

        CoordenadaPolar cp = CoordenadaPolar.converterCoordenadaCartesiana(referencial, cc);

        Posicao posicao = new Posicao(cg, cp, cc);

        return posicao;

    }

    /**
     * Obtém a coordenada geográfica (latitude e longitude) da posição.
     *
     * @return
     */
    @JsonProperty("geo")
    public CoordenadaGeografica getCoordenadaGeografica() {
        return coordenadaGeografica;
    }

    /**
     * Define a coordenada geográfica (latitude e longitude) da posição.
     *
     * @param coordenadaGeografica
     */
    synchronized public void setCoordenadaGeografica(CoordenadaGeografica coordenadaGeografica) {
        this.coordenadaGeografica = coordenadaGeografica;
    }

    /**
     * Obtém a coordenada polar (marcação e distância) da posição.
     *
     * @return
     */
    @JsonProperty("polar")
    public CoordenadaPolar getCoordenadaPolar() {
        return coordenadaPolar;
    }

    /**
     * Define a coordenada polar (marcação e distância) da posição.
     *
     * @param coordenadaPolar
     */
    synchronized public void setCoordenadaPolar(CoordenadaPolar coordenadaPolar) {
        this.coordenadaPolar = coordenadaPolar;
    }

    /**
     * Obtém a coordenada cartesiana (x,y) em milhas náuticas da posição.
     *
     * @return
     */
    @JsonProperty("cartesiana")
    public CoordenadaCartesiana getCoordenadaCartesiana() {
        return coordenadaCartesiana;
    }

    /**
     * Define a coordenada cartesiana (x,y) em milhas náuticas da posição.
     *
     * @param coordenadaCartesiana
     */
    synchronized public void setCoordenadaCartesiana(CoordenadaCartesiana coordenadaCartesiana) {
        this.coordenadaCartesiana = coordenadaCartesiana;
    }

    /**
     * Obtém a precisão da posição em MN
     *
     * @return
     */
    public Double getPrecisao() {
        return precisao;
    }

    /**
     * Define a precisão da posição em MN
     *
     * @param precisao
     */
    public void setPrecisao(Double precisao) {
        this.precisao = precisao;
    }

    @Override
    public Posicao clone() throws CloneNotSupportedException {
        return new Posicao(
                new CoordenadaGeografica(coordenadaGeografica.getLatitude(), coordenadaGeografica.getLongitude()),
                coordenadaPolar != null ? new CoordenadaPolar(coordenadaPolar.getMarcacao(), coordenadaPolar.getDistancia()) : null,
                coordenadaCartesiana != null ? new CoordenadaCartesiana(coordenadaCartesiana.getX(), coordenadaCartesiana.getY()) : null
        );
    }

    @Override
    public String toString() {
        return "Coordenada Geografica: " + coordenadaGeografica + "\nCoordenadaCartesiana: " + coordenadaCartesiana + "\nCoordenada Polar: " + coordenadaPolar;
    }

}
