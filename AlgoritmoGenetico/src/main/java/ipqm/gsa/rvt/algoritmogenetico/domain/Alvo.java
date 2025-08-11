package ipqm.gsa.rvt.algoritmogenetico.domain;

import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;

/**
 *
 * @author Pedro Magno
 */
public class Alvo {

    private CoordenadaGeografica coordGeo;

    public Alvo(CoordenadaGeografica coordGeo){
        this.coordGeo = coordGeo;
    } 
    public Alvo(double latitude, double longitude) {
        coordGeo = new CoordenadaGeografica(latitude, longitude);
    }

    public CoordenadaGeografica getCoordGeo() {
        return coordGeo;
    }
    
    public Double getLatitude(){
        return coordGeo.getLatitude();
    }
    
    public Double getLongitude(){
        return coordGeo.getLongitude();
    }
}

