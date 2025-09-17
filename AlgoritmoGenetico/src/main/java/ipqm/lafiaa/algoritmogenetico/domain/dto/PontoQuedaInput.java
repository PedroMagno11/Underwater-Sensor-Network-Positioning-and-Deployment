package ipqm.lafiaa.algoritmogenetico.domain.dto;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;

public class PontoQuedaInput {
    private double custo;
    private CoordenadaCartesianaRVT coordCartesiana;


    public PontoQuedaInput(){

    }


    public PontoQuedaInput(CoordenadaCartesianaRVT coordenadaCartesiana){
        this.coordCartesiana = coordenadaCartesiana;
    }

    public PontoQuedaInput(CoordenadaCartesianaRVT coordCartesiana, double custo){
        this(coordCartesiana);
        this.custo = custo;
    }

    public double getCusto() {
        return custo;
    }

    public void setCusto(double custo) {
        this.custo = custo;
    }

    public CoordenadaCartesianaRVT getCoordCartesiana() {
        return coordCartesiana;
    }

    public void setCoordCartesiana(CoordenadaCartesianaRVT coordCartesiana) {
        this.coordCartesiana = coordCartesiana;
    }
}
