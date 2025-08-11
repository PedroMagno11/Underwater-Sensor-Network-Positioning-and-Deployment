package ipqm.gsa.rvt.algoritmogenetico.utils.coord;


import java.util.Locale;

/**
 * Coordenadas geográficas
 *
 * @author medeiros
 */
public class GeoCoord {

    private double longitude;
    private double latitude;
    private XYCoord xy; // projeção do ponto no sistema cartesiano

    public GeoCoord(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        xy = null;
    }

    public GeoCoord() {
        longitude = 0.0;
        latitude = 0.0;
        xy = null;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        xy = null;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        xy = null;
    }

    public void set(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        xy = null;
    }

    public double getX() {
        updateXY();
        return xy.getX();
    }

    public double getY() {
        updateXY();
        return xy.getY();
    }

    public XYCoord getXY() {
        updateXY();
        return xy;
    }

    /**
     * Calcula a marcação de um ponto em relação ao próprio objeto
     *
     * @param p ponto considerado
     * @return marcação do ponto (graus)
     */
    public double bearing(GeoCoord p) {
        updateXY();
        return xy.bearing(p.getXY());
    }

    /**
     * Calcula a marcação de um ponto em relação ao próprio objeto
     *
     * @param longitude longitude do ponto considerado
     * @param latitude latitude do ponto considerado
     * @return marcação do ponto (graus)
     */
    public double bearing(double longitude, double latitude) {
        return bearing(new GeoCoord(longitude, latitude));
    }

    /**
     * Calcula a distância de um ponto ao próprio objeto.
     *
     * @param p ponto considerado
     * @return distância até o ponto (milhas náuticas)
     */
    public double distance(GeoCoord p) {
        return Geodesics.distance(longitude, latitude, p.getLongitude(), p.getLatitude());
    }

    /**
     * Calcula a distância de um ponto ao próprio objeto.
     *
     * @param longitude2 longitude do ponto considerado
     * @param latitude2 latitude do ponto considerado
     * @return distância até o ponto (milhas náuticas)
     */
    public double distance(double longitude2, double latitude2) {
        return Geodesics.distance(longitude, latitude, longitude2, latitude2);
    }

    /**
     * Calcula o ponto que está a uma determinada marcação e distância do
     * próprio objeto.
     *
     * @param p marcação e distância desejada
     * @return posição do ponto calculado
     */
    public GeoCoord destination(PolarCoord p) {
        return destination(p.getBearing(), p.getDistance());
    }

    /**
     * Calcula o ponto que está a uma determinada marcação e distância do
     * próprio objeto.
     *
     * @param bearing marcação desejada (graus)
     * @param distance distância desejada (milhas náuticas)
     * @return posição do ponto calculado
     */
    public GeoCoord destination(double bearing, double distance) {
        return Geodesics.destination(longitude, latitude, bearing, distance);
    }

    @Override
    public String toString() {
        String lat = latitudeToString(latitude);
        String lon = longitudeToString(longitude);
        return "(" + lat + ", " + lon + ")";
    }

    /**
     * Converte a latitude DG (DÉCIMO DE GRAUS) para o formato GDM
     * (XX°XX.XXX'[N|S]).
     *
     * @param latitude latitude em décimos de graus
     */
    private static String latitudeToString(double latitude) {
        double graus = (int) latitude;
        double minutos = Math.abs((latitude - graus) * 60.0);
        if (minutos > 59.999) {  // não permite 60 minutos
            minutos = 0.0;
            graus += Math.signum(graus);
        }
        String str = String.format(Locale.US, "%02.0f\u00b0%06.3f\u0027", Math.abs(graus), minutos);
        str += latitude >= 0.0 ? "N" : "S";
        return str;
    }

    /**
     * Converte a longitude DG (DÉCIMO DE GRAUS) para o formato GDM
     * (XXX°XX.XXX'[E|W]).
     *
     * @param longitude longitude em décimos de graus
     */
    private static String longitudeToString(double longitude) {
        double graus = (int) longitude;
        double minutos = Math.abs((longitude - graus) * 60.0);
        if (minutos > 59.999) {  // não permite 60 minutos
            minutos = 0.0;
            graus += Math.signum(graus);
        }
        String str = String.format(Locale.US, "%03.0f\u00b0%06.3f\u0027", Math.abs(graus), minutos);
        str += longitude >= 0.0 ? "E" : "W";
        return str;
    }

    /**
     * projeta automaticamente o ponto no sistema cartesiano utilizado para
     * aumentar o desempenho dos serviços de carta
     */
    private void updateXY() {
        if (xy == null) {
            xy = Mercator.toXY(longitude, latitude);
        }
    }
}
