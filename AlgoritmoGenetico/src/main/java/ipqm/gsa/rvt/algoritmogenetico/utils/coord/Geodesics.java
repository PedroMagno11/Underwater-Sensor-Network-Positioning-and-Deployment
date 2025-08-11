/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
 /* Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2012             */
 /*                                                                                                */
 /* from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the */
 /*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    */
 /*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             */
 /*                                                                                                */
 /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
 /* Vincenty Direct Solution of Geodesics on the Ellipsoid (c) Chris Veness 2005-2012              */
 /*                                                                                                */
 /* from: Vincenty direct formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the  */
 /*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    */
 /*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             */
 /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */

package ipqm.gsa.rvt.algoritmogenetico.utils.coord;

/**
 * Vincenty Inverse Solution of Geodesics on the Ellipsoid.
 *
 * @author Chris Veness
 * @author medeiros
 */
public class Geodesics {

    // WGS-84 ellipsoid params
    private static final double EARTH_A = 6378137.0;
    private static final double EARTH_B = 6356752.314245;
    private static final double EARTH_F = 1 / 298.257223563;
    private static final double EARTH_E = Math.sqrt(1 - (EARTH_B * EARTH_B) / (EARTH_A * EARTH_A));

    private Geodesics() {
    }

    /**
     * Calcula a menor distância (GREAT CIRCLE) entre dois pontos.
     *
     * @param lon1 first longitude in decimal degree
     * @param lat1 first latitude in decimal degree
     * @param lon2 second latitude in decimal degree
     * @param lat2 second latitude in decimal degree
     * @return distance in nautical miles between points
     */
    public static double distance(double lon1, double lat1, double lon2, double lat2) {
        return Geodesics.distanceGC(lon1, lat1, lon2, lat2);
    }

    /**
     * Calcula a distância entre dois pontos seguindo uma rhumb line. Sources:
     * https://planetcalc.com/713/ V.S. Mikhailov, Navigation and Pilot book]]
     * Miljenko Petrović DIFFERENTIAL EQUATION OF A LOXODROME ON THE SPHEROID
     *
     * @param lon1 first longitude in decimal degree
     * @param lat1 first latitude in decimal degree
     * @param lon2 second latitude in decimal degree
     * @param lat2 second latitude in decimal degree
     * @return distance in nautical miles between points
     */
    public static double distanceRL(double lon1, double lat1, double lon2, double lat2) {
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double eSinPhi1 = EARTH_E * Math.sin(Math.toRadians(lat1));
        double eSinPhi2 = EARTH_E * Math.sin(Math.toRadians(lat2));
        double x1 = Math.tan(Math.PI / 4.0 + Math.toRadians(lat1 / 2.0));
        double y1 = Math.pow((1.0 - eSinPhi1) / (1.0 + eSinPhi1), EARTH_E / 2.0);
        double x2 = Math.tan(Math.PI / 4.0 + Math.toRadians(lat2 / 2.0));
        double y2 = Math.pow((1.0 - eSinPhi2) / (1.0 + eSinPhi2), EARTH_E / 2.0);
        double alpha = Math.atan(deltaLambda / (Math.log(x2 * y2) - Math.log(x1 * y1)));
        double s1 = (1.0 - (EARTH_E * EARTH_E) / 4.0) * deltaPhi;
        double sin2Phi1 = Math.sin(Math.toRadians(2 * lat1));
        double sin2Phi2 = Math.sin(Math.toRadians(2 * lat2));
        double s2 = (3.0 * EARTH_E * EARTH_E / 8.0) * (sin2Phi2 - sin2Phi1);
        double s = EARTH_A * (1.0 / Math.cos(alpha)) * (s1 - s2);
        return s / 1852.0;  // nautical miles
    }

    /**
     * Calculates geodetic distance between two points specified by
     * latitude/longitude using Vincenty inverse formula for ellipsoids
     *
     * @param lon1 first longitude in decimal degree
     * @param lat1 first latitude in decimal degree
     * @param lon2 second latitude in decimal degree
     * @param lat2 second latitude in decimal degree
     * @return distance in nautical miles between points
     */
    public static double distanceGC(double lon1, double lat1, double lon2, double lat2) {
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - EARTH_F) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - EARTH_F) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1);
        double cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2);
        double cosU2 = Math.cos(U2);
        double lambda = L;
        double lambdaP;
        int iterLimit = 100;
        double sinLambda;
        double cosLambda;
        double sinSigma;
        double cosSqAlpha;
        double cosSigma;
        double sigma;
        double cos2SigmaM;
        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda) + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0) {
                return 0;  // co-incident points
            }
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            if (Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (§6)
            }
            double C = EARTH_F / 16 * cosSqAlpha * (4 + EARTH_F * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1 - C) * EARTH_F * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0) {
            return Double.NaN;  // formula failed to converge
        }
        double uSq = cosSqAlpha * (EARTH_A * EARTH_A - EARTH_B * EARTH_B) / (EARTH_B * EARTH_B);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double s = EARTH_B * A * (sigma - deltaSigma);

//        // note: to return initial/final bearings in addition to distance, use something like:
//        double fwdAz = Math.toDegrees(Math.atan2(cosU2*sinLambda,  cosU1*sinU2-sinU1*cosU2*cosLambda));
//        double revAz = Math.toDegrees(Math.atan2(cosU1*sinLambda, -sinU1*cosU2+cosU1*sinU2*cosLambda));
        return s / 1852.0;  // nautical miles
    }

    /**
     *
     * @param lon first point longitude in decimal degrees
     * @param lat first point latitude in decimal degrees
     * @param bearing initial bearing in decimal degrees
     * @param distance istance along bearing in nautical miles
     * @return destination point
     */
    public static GeoCoord destination(double lon, double lat, double bearing, double distance) {
        double s = distance * 1852.0;  // meters
        double alpha1 = Math.toRadians(bearing);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        double tanU1 = (1 - EARTH_F) * Math.tan(Math.toRadians(lat));
        double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
        double sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (EARTH_A * EARTH_A - EARTH_B * EARTH_B) / (EARTH_B * EARTH_B);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double sigma = s / (EARTH_B * A);
        double sinSigma = Math.sin(sigma);
        double cosSigma = Math.cos(sigma);
        double cos2SigmaM = Math.cos(2 * sigma1 + sigma);
        double sigmaP = 2 * Math.PI;
        while (Math.abs(sigma - sigmaP) > 1e-12) {
            cos2SigmaM = Math.cos(2 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
            sigmaP = sigma;
            sigma = s / (EARTH_B * A) + deltaSigma;
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double lat2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1, (1 - EARTH_F) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda = Math.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double C = EARTH_F / 16 * cosSqAlpha * (4 + EARTH_F * (4 - 3 * cosSqAlpha));
        double L = lambda - (1 - C) * EARTH_F * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        double lon2 = (Math.toRadians(lon) + L + 3 * Math.PI) % (2 * Math.PI) - Math.PI;  // normalise to -180...+180

        //double revAz = Math.atan2(sinAlpha, -tmp);  // final bearing, if required
        return new GeoCoord(Math.toDegrees(lon2), Math.toDegrees(lat2));
    }
}
