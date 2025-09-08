package ipqm.lafiaa.algoritmogenetico.domain.rvt;

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
