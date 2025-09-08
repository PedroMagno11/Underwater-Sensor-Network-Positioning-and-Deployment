package ipqm.lafiaa.algoritmogenetico.utils.coord;

/**
 * Cálculador de distâncias e rumos.
 *
 * @author medeiros
 * @author Chris Veness
 */
public class DistanceAndBearings {

    public enum LegType {
        RL, // Rhumb Line
        GC   // Great Circle
    }

    // WGS-84 ellipsoid params
    private static final double EARTH_A = 6378137.0;
    private static final double EARTH_B = 6356752.314245;
    private static final double EARTH_F = 1 / 298.257223563;
    private static final double EARTH_E = Math.sqrt(1 - (EARTH_B * EARTH_B) / (EARTH_A * EARTH_A));

    private GeoCoord p1;
    private GeoCoord p2;
    private LegType legType;
    private double distance;
    private double initialBearing;
    private double finalBearing;

    public DistanceAndBearings() {
        p1 = null;
        p2 = null;
        legType = LegType.GC;
    }

    public DistanceAndBearings(GeoCoord p1, GeoCoord p2, LegType legType) {
        this.p1 = p1;
        this.p2 = p2;
        this.legType = legType;
        compute(p1.getLongitude(), p1.getLatitude(), p2.getLongitude(), p2.getLatitude(), legType);
    }

    public DistanceAndBearings(GeoCoord p1, GeoCoord p2) {
        this(p1, p2, LegType.GC);
    }

    public GeoCoord getP1() {
        return p1;
    }

    public void setP1(GeoCoord p1) {
        this.p1 = p1;
    }

    public GeoCoord getP2() {
        return p2;
    }

    public void setP2(GeoCoord p2) {
        this.p2 = p2;
    }

    public LegType getLegType() {
        return legType;
    }

    public void setLegType(LegType legType) {
        this.legType = legType;
    }

    public void compute() {
        if (p1 != null && p2 != null) {
            compute(p1.getLongitude(), p1.getLatitude(), p2.getLongitude(), p2.getLatitude(), legType);
        }
    }

    public double getDistance() {
        return distance;
    }

    public double getInitialBearing() {
        return initialBearing;
    }

    public double getFinalBearing() {
        return finalBearing;
    }

    private void compute(double lon1, double lat1, double lon2, double lat2, LegType legType) {
        if (legType == LegType.GC) {
            computeGC(lon1, lat1, lon2, lat2);
        } else {
            computeRL(lon1, lat1, lon2, lat2);
        }
    }

    private void computeGC(double lon1, double lat1, double lon2, double lat2) {
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
                this.distance = 0.0;
                this.initialBearing = 0.0;
                this.finalBearing = 0.0;
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
            this.distance = Double.NaN;  // formula failed to converge
            this.initialBearing = Double.NaN;
            this.finalBearing = Double.NaN;
        }
        double uSq = cosSqAlpha * (EARTH_A * EARTH_A - EARTH_B * EARTH_B) / (EARTH_B * EARTH_B);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double s = EARTH_B * A * (sigma - deltaSigma);

        // note: to return initial/final bearings in addition to distance, use something like:
        double fwdAz = Math.toDegrees(Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
        double revAz = Math.toDegrees(Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda));
        if (fwdAz < 0) {
            fwdAz += 360.0;
        }
        if (revAz < 0) {
            revAz += 360.0;
        }
        this.distance = s / 1852.0;  // nautical miles
        this.initialBearing = fwdAz;
        this.finalBearing = revAz;
    }

    private void computeRL(double lon1, double lat1, double lon2, double lat2) {
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
        this.distance = s / 1852.0;  // nautical miles
        this.initialBearing = Math.toDegrees(alpha);
        this.finalBearing = initialBearing;
    }
}
