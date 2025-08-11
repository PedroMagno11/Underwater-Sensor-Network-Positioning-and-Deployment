package ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada;

import java.io.Serializable;

/**
 * Abstração dos tipos de coordenadas existentes.
 * @author Pablo Rangel
 * @since 11/03/2011
 */
public abstract class TipoCoordenada implements Cloneable,Serializable {

    @Override
    public TipoCoordenada clone() throws CloneNotSupportedException {
        return (TipoCoordenada) super.clone();
    }
}
