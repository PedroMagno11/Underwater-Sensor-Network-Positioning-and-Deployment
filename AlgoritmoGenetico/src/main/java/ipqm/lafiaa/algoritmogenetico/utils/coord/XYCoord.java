package ipqm.gsa.rvt.algoritmogenetico.utils.coord;

/**
 * Coordenadas cartesianas
 * @author medeiros
 */
public class XYCoord {
    private double x;
    private double y;

    public XYCoord() {
        x = 0.0;
        y = 0.0;
    }
       
    public XYCoord(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
    
    /**
     * Calcula a marcação de um ponto em relação ao próprio objeto
     * @param p ponto considerado
     * @return marcação do ponto
     */
    public double bearing(XYCoord p) {
        double dx = p.getX() - this.x;
        double dy = p.getY() - this.y;
        double bearing = Math.toDegrees(Math.atan2(dx, dy));
        if (bearing < 0.0) {
            bearing += 360.0;
        }
        return bearing;
    }
    
    /**
     * Calcula a marcação de um ponto em relação ao próprio objeto
     * @param x coordenada x do ponto considerado
     * @param y coordenada y do ponto considerado
     * @return marcação do ponto
     */
    public double bearing(double x, double y) {
        return bearing(new XYCoord(x,y));
    }
}
