package ipqm.gsa.rvt.algoritmogenetico.domain.rvt;

import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;

/**
 *
 * @author Pedro Magno
 * @laboratório LaFIAA (Laboratório de Fusão e Inteligência Artificial Aplicada)
 */
public class PontoQueda {

   private double menorCusto;
    
   private CoordenadaCartesianaRVT pontoDeQueda;


    public PontoQueda(double menorCusto, CoordenadaCartesianaRVT pontoDeQueda) {
        this.menorCusto = menorCusto;
        this.pontoDeQueda = pontoDeQueda;
    }

    public double getMenorCusto() {
        return menorCusto;
    }

    public CoordenadaCartesianaRVT getPontoDeQueda() {
        return pontoDeQueda;
    }

}
