package ipqm.gsa.rvt.algoritmogenetico.utils.coord;

/**
 * Coordenadas polares.
 *
 * @author Marcelo Medeiros <medeiros@ipqm.mar.mil.br>
 */
public class PolarCoord {

    private double distance;
    private double bearing;

    public PolarCoord() {
    }

    public PolarCoord(double distance, double bearing) {
        this.distance = distance;
        this.bearing = bearing;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }
}
