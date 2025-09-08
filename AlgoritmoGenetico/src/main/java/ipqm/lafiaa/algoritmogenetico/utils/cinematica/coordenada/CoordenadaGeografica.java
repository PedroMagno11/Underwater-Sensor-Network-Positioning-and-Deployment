package ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada;


import com.fasterxml.jackson.annotation.JsonProperty;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.GeoCoord;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Geodesics;
import ipqm.gsa.rvt.algoritmogenetico.utils.coord.Mercator;
import java.util.Locale;

/**
 * Classe responsável por representar a posição corrente do acompanhamento em
 * termos de latitude e longitude.
 *
 * @author Pablo Rangel
 * @author Medeiros
 * @since 11/03/2011
 */
public class CoordenadaGeografica extends TipoCoordenada {

    private Double latitude;
    private Double longitude;

    /**
     * Construtor sem parâmetros.
     */
    public CoordenadaGeografica() {
        this.latitude = null;
        this.longitude = null;
    }

    /**
     * Construtor com parâmetros.
     *
     * @param latitude valor da latitude da coordenada geográfica.
     * @param longitude valor da longitude da coordenada geográfica.
     */
    public CoordenadaGeografica(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Método de conversão de coordenada cartesiana para coordenada geográfica.
     * Dado a abscissa e a ordenada de um ponto, este método retorna a latitude
     * e a longitude deste ponto. Este método utiliza a biblioteca PROJ4 para
     * conversão. Como a coordenada cartesiana adota por padrão a unidade de
     * milhas náuticas, este método converte o ponto para a unidade de metros.
     *
     * @param coordenadaCartesiana coordenada cartesiana que se deseja
     * converter.
     * @return CoordenadaGeografica coordenada cartesiana convertida para
     * coordenada geográfica.
     */
    public static CoordenadaGeografica converterCoordenadaCartesiana(CoordenadaCartesiana coordenadaCartesiana) {
        if (coordenadaCartesiana == null) {
            return null;
        }
        GeoCoord cg = Mercator.toGeo(coordenadaCartesiana.getX(), coordenadaCartesiana.getY());
        return new CoordenadaGeografica(cg.getLatitude(), cg.getLongitude());
    }

    /**
     * Método de conversão de coordenada polar para coordenada geográfica
     * Baseado na posição geográfica de um referencial e, dado a marcação e a
     * distância de um ponto de interesse, este método retorna a latitude e a
     * longitude deste ponto. O PONTO INCIAL NÃO PODE SER NO POLO!
     *
     * @param coordenadaGeograficaReferencial coordenada geográfica de
     * referência.
     * @param coordenadaPolarInteresse coordenada polar que se deseja converter.
     * @return CoordenadaGeografica coordenada polar convertida para coordenada
     * coordenada geográfica.
     */
    public static CoordenadaGeografica converterCoordenadaPolar(CoordenadaGeografica coordenadaGeograficaReferencial,
            CoordenadaPolar coordenadaPolarInteresse) {
        GeoCoord destino = Geodesics.destination(
                coordenadaGeograficaReferencial.getLongitude(), coordenadaGeograficaReferencial.getLatitude(),
                coordenadaPolarInteresse.getMarcacao(), coordenadaPolarInteresse.getDistancia()
        );
        return new CoordenadaGeografica(destino.getLatitude(), destino.getLongitude());
    }

    /**
     * Método de conversão de distancia Euclidiana para coordenada geográfica.
     * Dado duas distancias Euclidianas em X e em Y (distanciaX e distanciaY) e
     * uma coordenada geografica referencial, este método retorna a coordenada
     * cartesiana deste ponto após aplicar o deslocamento solicitado.
     *
     * @param coordenadaGeograficaReferencial Coordenada geografica de
     * referência (x,y) do ponto que se deseja transladar a posição.
     * @param distanciaX Distância Euclidiana na horizontal (marcação 90°).
     * @param distanciaY Distância Euclidiana na vertical (marcação 0°).
     * @return CoordenadaGeorafica coordenada geográfica (lat,lon) convertida.
     */
    public static CoordenadaGeografica converterDistanciaXY(CoordenadaGeografica coordenadaGeograficaReferencial,
            double distanciaX, double distanciaY) {
        double marcacao = Math.toDegrees(Math.atan2(distanciaX, distanciaY));
        double distancia = Math.hypot(distanciaX, distanciaY);
        CoordenadaPolar coordenadaPolarInteresse = new CoordenadaPolar(marcacao, distancia);
        return CoordenadaGeografica.converterCoordenadaPolar(coordenadaGeograficaReferencial, coordenadaPolarInteresse);
    }

    /**
     * Método que calcula a distância entre dois pontos baseados em suas
     * coordenadas geograficas.
     *
     * @param coordenadaGeografica1 latitude e longitude da coordenada
     * geográfica 1.
     * @param coordenadaGeografica2 latitude e longitude da coordenada
     * geográfica 2.
     * @return double distância em milhas nauticas.
     * @see #calcularDistancia(double, double, double, double)
     */
    public static double calcularDistancia(CoordenadaGeografica coordenadaGeografica1, CoordenadaGeografica coordenadaGeografica2) {
        if (coordenadaGeografica1 == null || coordenadaGeografica2 == null) {
            return Double.NaN;
        }

        return Geodesics.distance(
                coordenadaGeografica1.getLongitude(), coordenadaGeografica1.getLatitude(),
                coordenadaGeografica2.getLongitude(), coordenadaGeografica2.getLatitude()
        );
    }

    /**
     * Método que calcula a distância entre dois pontos baseados em suas
     * coordenadas geograficas.
     *
     * @param latitude1 latitude da coordenada geográfica 1 (em décimo de graus)
     * @param longitude1 longitude da coordenada geográfica 1 (em décimo de
     * graus)
     * @param latitude2 latitude da coordenada geográfica 2 (em décimo de graus)
     * @param longitude2 longitude da coordenada geográfica 2 (em décimo de
     * graus)
     * @return double distância em milhas nauticas
     */
    public static double calcularDistancia(double latitude1, double longitude1, double latitude2, double longitude2) {
        return Geodesics.distance(longitude1, latitude1, longitude2, latitude2);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof CoordenadaGeografica) {
            CoordenadaGeografica cg = (CoordenadaGeografica) o;

            if (cg.getClass().equals(getClass())
                    && cg.hashCode() == hashCode()
                    && cg.getLatitude() == getLatitude()
                    && cg.getLongitude() == getLongitude()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtém a latitude no formato decimal.
     */
    @JsonProperty("lat")
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Define a latitude no formato decimal.
     */
    synchronized public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Obtém longitude no formato decimal.
     */
    @JsonProperty("lng")
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Define a longitude no formato decimal.
     */
    synchronized public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        String lat = latitudeToString(latitude);
        String lon = longitudeToString(longitude);
        return lat + " " + lon;
    }

    /**
     * Converte a latitude DG (DÉCIMO DE GRAUS) para o formato GDM ou GMS (De
     * acorordo com a configuração da IHM)
     *
     * @param latitude em décimos de graus
     * @return latitude GMS
     */
    public static String latitudeToString(double latitude) {
        return latitudeDGtoGMS(latitude);
    }

    /**
     * Converte a longitude DG (DÉCIMO DE GRAUS) para o formato GDM ou GMS (De
     * acorordo com a configuração da IHM)
     *
     * @param longitude em décimos de graus
     * @return longitude GMS
     */
    public static String longitudeToString(double longitude) {
        return longitudeDGtoGMS(longitude);
    }

    /**
     * Converte a latitude DG (DÉCIMO DE GRAUS) para o formato GDM
     * (XX°XX.XXX'[N|S]).
     *
     * @param latitude latitude em décimos de graus
     * @return latitude em (XX°XX.XXX'[N|S])
     */
    public static String latitudeDGtoGDM(double latitude) {
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
     * @return longitude em (XXX°XX.XXX'[E|W])
     */
    public static String longitudeDGtoGDM(double longitude) {
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
     * Converte a latitude no formato String (XX°XX.XXX'[N|S]) ou
     * (XX°XX'XX''[E|W]) para o formato DG (DÉCIMO DE GRAUS).
     *
     * @param latitude latitude em décimos de graus
     * @return latitude em décimos de graus
     */
    public static double latitudeStringtoDG(String latitude) {
        if (latitude.matches("\\d\\d°\\d\\d.\\d\\d\\d'[Nn|Ss]")) {
            double graus = Double.parseDouble(latitude.substring(0, 2));
            double dMinutos = Double.parseDouble(latitude.substring(3, 9)) / 60;
            String sinal = latitude.substring(10, 11);
            if (sinal.equals("S")) {
                return (graus + dMinutos) * -1;
            } else {
                return (graus + dMinutos);
            }
        } else if (latitude.matches("\\d\\d°\\d\\d'\\d\\d\"[Nn|Ss]")) {
            double graus = Double.parseDouble(latitude.substring(0, 2));
            double dMinutos = Double.parseDouble(latitude.substring(3, 5)) / 60;
            double dSegundos = Double.parseDouble(latitude.substring(6, 8)) / 3600;
            String sinal = latitude.substring(9, 10);
            if (sinal.equals("S")) {
                return (graus + dMinutos + dSegundos) * -1;
            } else {
                return (graus + dMinutos + dSegundos);
            }
        }
        return 0;
    }

    /**
     * Converte a longitude no formato String (XXX°XX.XXX'[E|W]) ou
     * (XXX°XX'XX''[E|W]) para o formato DG (DÉCIMO DE GRAUS).
     *
     * @param longitude longitude em décimos de graus
     * @return longitude em décimos de graus
     */
    public static double longitudeStringtoDG(String longitude) {
        if (longitude.matches("\\d\\d\\d°\\d\\d.\\d\\d\\d'[Ee|Ww]")) {
            double graus = Double.parseDouble(longitude.substring(0, 3));
            double dMinutos = Double.parseDouble(longitude.substring(4, 10)) / 60;
            String sinal = longitude.substring(11, 12);
            if (sinal.equals("W")) {
                return (graus + dMinutos) * -1;
            } else {
                return (graus + dMinutos);
            }
        } else if (longitude.matches("\\d\\d\\d°\\d\\d'\\d\\d\"[Ee|Ww]")) {
            double graus = Double.parseDouble(longitude.substring(0, 3));
            double dMinutos = Double.parseDouble(longitude.substring(4, 6)) / 60;
            double dSegundos = Double.parseDouble(longitude.substring(7, 9)) / 3600;
            String sinal = longitude.substring(10, 11);
            if (sinal.equals("W")) {
                return (graus + dMinutos + dSegundos) * -1;
            } else {
                return (graus + dMinutos + dSegundos);
            }
        }
        return 0;
    }

    /**
     * Converte a latitude DG (DÉCIMO DE GRAUS) para o formato GMS
     * (XX°XX'XX''[E|W]).
     *
     * @param longitude latitude em décimos de graus
     * @return latitude em (XX°XX'XX''[E|W])
     */
    public static String longitudeDGtoGMS(double longitude) {
        int graus = (int) longitude;
        int minutos = (int) (60 * (longitude - graus));
        int segundos = (int) Math.round(((60 * (longitude - graus)) - minutos) * 60);

        if (segundos >= 60) {
            minutos += 1;
            segundos -= 60;
            if (minutos == 60) {
                graus += 1;
                minutos -= 60;
            }
        } else if (segundos == -60) {
            minutos -= 1;
            segundos += 60;
            if (minutos <= -60) {
                graus -= 1;
                minutos += 60;
            }
        }

        String str = String.format(Locale.US, "%03d\u00b0%02d\u0027%02d\"", Math.abs(graus), Math.abs(minutos), Math.abs(segundos));
        str += longitude >= 0.0 ? "E" : "W";

        return str;
    }

    /**
     * Converte a latitude DG (DÉCIMO DE GRAUS) para o formato GMS
     * (XX°XX'XX''[E|W]).
     *
     * @param latitude latitude em décimos de graus
     * @return latitude em (XX°XX'XX''[E|W])
     */
    public static String latitudeDGtoGMS(double latitude) {

        int graus = (int) latitude;
        int minutos = (int) (60 * (latitude - graus));
        int segundos = (int) Math.round(((60 * (latitude - graus)) - minutos) * 60);

        if (segundos >= 60) {
            minutos += 1;
            segundos -= 60;
            if (minutos == 60) {
                graus += 1;
                minutos -= 60;
            }
        } else if (segundos == -60) {
            minutos -= 1;
            segundos += 60;
            if (minutos <= -60) {
                graus -= 1;
                minutos += 60;
            }
        }

        String str = String.format(Locale.US, "%02d\u00b0%02d\u0027%02d\"", Math.abs(graus), Math.abs(minutos), Math.abs(segundos));
        str += latitude >= 0.0 ? "N" : "S";

        return str;
    }

    /**
     * Converte a latitude no formato GDM (GRAUS,DÉCIMO DE MINUTOS[N|S]) para o
     * formato DG (DÉCIMO DE GRAUS).
     *
     * @param latitude latitude em décimos de graus
     */
    public static double latitudeGDMtoDG(String latitude) {
        double graus = Double.parseDouble(latitude.substring(0, 2));
        double dMinutos = Double.parseDouble(latitude.substring(2, latitude.length() - 1)) / 60;
        String sinal = latitude.substring(latitude.length() - 1, latitude.length());
        if (sinal.equals("S")) {
            return (graus + dMinutos) * -1;
        } else {
            return (graus + dMinutos);
        }
    }

    public static double latitudeWgs84toDG(double latitude) {

        double graus = latitude / 100;
        double minutos = latitude - (100 * graus);
        double dMinutos = (minutos / 60);
        return graus + dMinutos;
    }

    /**
     * Converte a longitude no formato GDM (GRAUS,DÉCIMO DE MINUTOS[W|E]) para o
     * formato DG (DÉCIMO DE GRAUS).
     *
     * @param longitude longitude em décimos de graus
     */
    public static double longitudeGDMtoDG(String longitude) {
        double graus = Double.parseDouble(longitude.substring(0, 3));
        double dMinutos = Double.parseDouble(longitude.substring(3, longitude.length() - 1)) / 60;
        String sinal = longitude.substring(longitude.length() - 1, longitude.length());
        if (sinal.equals("W")) {
            return (graus + dMinutos) * -1;
        } else {
            return (graus + dMinutos);
        }
    }

    /**
     * Converte a latitude no formato formato DG (DÉCIMO DE GRAUS) para o
     * formato GDM (GRAUS,DÉCIMO DE MINUTOS[N|S]).
     *
     * @param latitude latitude em graus e décimo de minutos.
     */
    public static String latitudeDGtoDGM(double latitude) {
        double graus = (int) latitude;
        double minutos = Math.abs((latitude - graus) * 60.0);
        String str = String.format(Locale.US, "%2.0f%06.3f", Math.abs(graus), minutos);
        str += latitude > 0.0 ? "N" : "S";
        return str;
    }

    /**
     * Converte a longitude no formato formato DG (DÉCIMO DE GRAUS) para o
     * formato GDM (GRAUS,DÉCIMO DE MINUTOS[E|W]).
     *
     * @param longitude longitude em graus e décimo de minutos.
     */
    public static String longitudeDGtoDGM(double longitude) {
        double graus = (int) longitude;
        double minutos = Math.abs((longitude - graus) * 60.0);
        String str = String.format(Locale.US, "%3.0f%06.3f", Math.abs(graus), minutos);
        str = str.replaceAll(" ", "0");
        str += longitude > 0.0 ? "E" : "W";
        return str;
    }
}
