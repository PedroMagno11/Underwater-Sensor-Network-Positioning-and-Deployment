package ipqm.lafiaa.algoritmogenetico.domain;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;

/**
 *
 * @author Pedro Magno
 * @laboratório LaFIAA (Laboratório de Fusão e Inteligência Artificial Aplicada)
 */
public class PontoQueda {

   private double custo;
    
   private CoordenadaCartesianaRVT pontoDeQueda;

   public PontoQueda(){}


    public PontoQueda(double custo, CoordenadaCartesianaRVT pontoDeQueda) {
        this.custo = custo;
        this.pontoDeQueda = pontoDeQueda;
    }

    public double getCusto() {
        return custo;
    }

    public CoordenadaCartesianaRVT getPontoDeQueda() {
        return pontoDeQueda;
    }

}
