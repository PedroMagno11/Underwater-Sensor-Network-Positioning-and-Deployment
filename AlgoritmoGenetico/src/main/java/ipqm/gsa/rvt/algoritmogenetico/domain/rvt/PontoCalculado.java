package ipqm.gsa.rvt.algoritmogenetico.domain.rvt;

/**
 *
 * @author dev
 */
public class PontoCalculado {

   private double menorCusto;
    
   private CoordenadaCartesianaRVT pontoDeQueda;

    public PontoCalculado(double menorCusto, CoordenadaCartesianaRVT pontoDeQueda) {
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
