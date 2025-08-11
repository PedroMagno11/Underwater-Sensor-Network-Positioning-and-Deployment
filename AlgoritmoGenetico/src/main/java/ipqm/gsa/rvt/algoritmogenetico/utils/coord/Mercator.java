package ipqm.gsa.rvt.algoritmogenetico.utils.coord;

/**
 * Projeção Mercator utilizando o datum WGS84.
 * Baseado na biblioteca Proj4.
 * 
 * @author medeiros
 */
public class Mercator {

    private static final double A       = 6378137.0;
    private static final double E       = 0.08181919084262149;
    private static final double HALF_PI = 1.5707963267948966;
    private static final int    N_ITER  = 15;
    private static final double DEG2RAD = Math.PI / 180.0;  // graus para radianos
    private static final double RAD2DEG = 1.0/DEG2RAD;      // radianos para graus
    private static final double NM2M = 1852.0;              // milhas náuticas para metros
    private static final double M2NM = 1.0/NM2M;            // metros para milhas náuticas

    private Mercator() {}
    
    /**
     * Converte de coordenadas dstgeográficas para coordenadas cartesianas
     * @param lon longitude desejada (WGS84)
     * @param lat latitude desejada (WGS84)
     * @return coordenadas cartesianas (milhas náuticas)
     */
    public static XYCoord toXY(double lon, double lat) {
        double lon0 = lon * DEG2RAD;  // radianos
        double lat0 = lat * DEG2RAD;  // radianos
        XYCoord dst = project(lon0, lat0);  // dst em metros
        dst.setX(A * dst.getX() * M2NM);  // dst em milhas náuticas
        dst.setY(A * dst.getY() * M2NM);
        return dst;  // milhas nauticas
    }
    
    /**
     * Converte de coordenadas geográficas para coordenadas cartesianas
     * @param p ponto desejado (WGS84)
     * @return coordenadas cartesianas (milhas náuticas)
     */
    public static XYCoord toXY(GeoCoord p) {
        return toXY(p.getLongitude(), p.getLatitude());
    }

    /**
     * Converte de coordenadas cartesianas para coordenadas geográficas
     * @param x x em milhas náuticas
     * @param y y em milhas náuticas
     * @return coordenadas geográficas (WGS84)
     */
    public static GeoCoord toGeo(double x, double y) {
        double x0 = (x * NM2M) / A;  // metros
        double y0 = (y * NM2M) / A;  // metros
        GeoCoord dst = projectInverse(x0, y0);  // dst em radianos
        if (dst.getLongitude() < -Math.PI) {
            dst.setLongitude(-Math.PI);
        } else if (dst.getLongitude() > Math.PI) {
            dst.setLongitude(Math.PI);
        }
        dst.setLongitude(dst.getLongitude() * RAD2DEG);
        dst.setLatitude(dst.getLatitude() * RAD2DEG);
        return dst;
    }
    
    /**
     * Converte de coordenadas cartesianas para coordenadas geográficas
     * @param p ponto desejado (em milhas náuticas)
     * @return coordenadas geográficas (WGS84)
     */
    public static GeoCoord toGeo(XYCoord p) {
        return toGeo(p.getX(), p.getY());
    }

    /**
     * Converte ponto para coordenadas cartesianas.
     * @param lon longitude (radianos)
     * @param lat latitude (radianos)
     * @return (x,y) em metros
     */
    private static XYCoord project(double lon, double lat) {
        double x = lon;
        double y = -Math.log(tsfn(lat, Math.sin(lat), E));
        return new XYCoord(x, y);
    }

    /**
     * Converte um ponto para coordenadas geográficas
     * @param x x em metros
     * @param y y em metros
     * @return (lon,lat) em radianos
     */
    private static GeoCoord projectInverse(double x, double y) {
        double lon = x;
        double lat = phi2(Math.exp(-y), E);
        return new GeoCoord(lon, lat);
    }

    private static double tsfn(double phi, double sinphi, double e) {
        sinphi *= e;
        return (Math.tan(.5 * (Mercator.HALF_PI - phi))
                / Math.pow((1. - sinphi) / (1. + sinphi), .5 * e));
    }

    private static double phi2(double ts, double e) {
        double eccnth, phi, con, dphi;
        int i;

        eccnth = .5 * e;
        phi = HALF_PI - 2. * Math.atan(ts);
        i = N_ITER;
        do {
            con = e * Math.sin(phi);
            dphi = HALF_PI - 2. * Math.atan(ts * Math.pow((1. - con) / (1. + con), eccnth)) - phi;
            phi += dphi;
        } while (Math.abs(dphi) > 1e-10 && --i != 0);
        if (i <= 0) {
            throw new RuntimeException();
        }
        return phi;
    }
}
