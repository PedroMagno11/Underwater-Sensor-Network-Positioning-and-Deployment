package ipqm.lafiaa.algoritmogenetico.domain;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.Posicao;

/**
 *
 * @author Pedro Magno
 */
public class Alvo {

    private static Alvo instance = null;

    private Posicao posicao;
    private CoordenadaCartesianaRVT coordenadaCartesianaRVT;

    private Alvo() {
        coordenadaCartesianaRVT = new CoordenadaCartesianaRVT(500, 500);
    }

    public static Alvo getInstance() {
        if (instance == null) {
            instance = new Alvo();
        }
        return instance;
    }

    public Posicao getPosicao() {
        return posicao;
    }

    public void setPosicao(Posicao posicao) {
        this.posicao = posicao;
    }

    public CoordenadaGeografica getCoordenadaGeografica() {
        return this.posicao.getCoordenadaGeografica();
    }

    public void setCoordenadaGeografica(CoordenadaGeografica c) {
        this.posicao.setCoordenadaGeografica(c);
    }

    public CoordenadaCartesianaRVT getCoordenadaCartesianaRVT() {
        return coordenadaCartesianaRVT;
    }

    public void setCoordenadaCartesianaRVT(CoordenadaCartesianaRVT coordenadaCartesianaRVT) {
        this.coordenadaCartesianaRVT = coordenadaCartesianaRVT;
    }
}


