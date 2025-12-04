package smartparking.util;

/**
 * Utility class to convert Singapore SVY21 coordinates to WGS84 (Lat/Lon).
 * Based on standard SVY21 projection formulas.
 */
public class SVY21Converter {

        // WGS84 Ellipsoid
        private static final double a = 6378137;
        private static final double f = 1 / 298.257223563;

        // SVY21 Projection
        private static final double oLat = 1.366666; // origin latitude in degrees
        private static final double oLon = 103.833333; // origin longitude in degrees
        private static final double oN = 38744.572; // false northing
        private static final double oE = 28001.642; // false easting
        private static final double k = 1.0; // scale factor

        public static class LatLon {
                public double lat;
                public double lon;

                public LatLon(double lat, double lon) {
                        this.lat = lat;
                        this.lon = lon;
                }
        }

        public static LatLon computeLatLon(double N, double E) {
                double b = a * (1 - f);
                double e2 = (2 * f) - (f * f);
                double e4 = e2 * e2;
                double e6 = e4 * e2;
                double A0 = 1 - (e2 / 4) - (3 * e4 / 64) - (5 * e6 / 256);
                double A2 = (3.0 / 8.0) * (e2 + (e4 / 4) + (15 * e6 / 128));
                double A4 = (15.0 / 256.0) * (e4 + (3 * e6 / 4));
                double A6 = 35.0 * e6 / 3072.0;

                double Nprime = N - oN;
                double Mo = calcM(oLat);
                double M = Mo + (Nprime / k);
                double n = (a - b) / (a + b);
                double n2 = n * n;
                double n3 = n2 * n;
                double n4 = n2 * n2;
                double G = a * (1 - n) * (1 - n2) * (1 + (9 * n2 / 4) + (225 * n4 / 64)) * (Math.PI / 180);

                // sigma in radians
                double sigma = (M * Math.PI) / (180 * G);

                double latPrimeT1 = ((3 * n / 2) - (27 * n3 / 32)) * Math.sin(2 * sigma);
                double latPrimeT2 = ((21 * n2 / 16) - (55 * n4 / 32)) * Math.sin(4 * sigma);
                double latPrimeT3 = (151 * n3 / 96) * Math.sin(6 * sigma);
                double latPrimeT4 = (1097 * n4 / 512) * Math.sin(8 * sigma);
                double latPrime = sigma + latPrimeT1 + latPrimeT2 + latPrimeT3 + latPrimeT4;

                double sinLatPrime = Math.sin(latPrime);
                double sin2LatPrime = sinLatPrime * sinLatPrime;

                double rhoPrime = a * (1 - e2) / Math.pow(1 - e2 * sin2LatPrime, 1.5);
                double vPrime = a / Math.sqrt(1 - e2 * sin2LatPrime);
                double psiPrime = vPrime / rhoPrime;
                double psiPrime2 = psiPrime * psiPrime;
                double psiPrime3 = psiPrime2 * psiPrime;
                double psiPrime4 = psiPrime3 * psiPrime;
                double tPrime = Math.tan(latPrime);
                double tPrime2 = tPrime * tPrime;
                double tPrime4 = tPrime2 * tPrime2;
                double tPrime6 = tPrime4 * tPrime2;
                double Eprime = E - oE;
                double x = Eprime / (k * vPrime);
                double x2 = x * x;
                double x3 = x2 * x;
                double x5 = x3 * x2;
                double x7 = x5 * x2;

                double latFactor = tPrime / (k * rhoPrime);
                double latTerm1 = latFactor * ((Eprime * x) / 2);
                double latTerm2 = latFactor * ((Eprime * x3) / 24)
                                * ((-4 * psiPrime2) + (9 * psiPrime) * (1 - tPrime2) + (12 * tPrime2));
                double latTerm3 = latFactor * ((Eprime * x5) / 720)
                                * ((8 * psiPrime4) * (11 - 24 * tPrime2) - (12 * psiPrime3) * (21 - 71 * tPrime2)
                                                + (15 * psiPrime2) * (15 - 98 * tPrime2 + 15 * tPrime4)
                                                + (180 * psiPrime) * (5 * tPrime2 - 3 * tPrime4) + 360 * tPrime4);
                double latTerm4 = latFactor * ((Eprime * x7) / 40320)
                                * (1385 - 3633 * tPrime2 + 4095 * tPrime4 + 1575 * tPrime6);
                double lat = latPrime - latTerm1 + latTerm2 - latTerm3 + latTerm4;

                double secLatPrime = 1 / Math.cos(latPrime);
                double lonTerm1 = x * secLatPrime;
                double lonTerm2 = ((x3 * secLatPrime) / 6) * (psiPrime + 2 * tPrime2);
                double lonTerm3 = ((x5 * secLatPrime) / 120) * ((-4 * psiPrime3) * (1 - 6 * tPrime2)
                                + psiPrime2 * (9 - 68 * tPrime2) + 72 * psiPrime * tPrime2 + 24 * tPrime4);
                double lonTerm4 = ((x7 * secLatPrime) / 5040) * (61 + 662 * tPrime2 + 1320 * tPrime4 + 720 * tPrime6);
                double lon = (oLon * Math.PI / 180) + lonTerm1 - lonTerm2 + lonTerm3 - lonTerm4;

                return new LatLon(lat * 180 / Math.PI, lon * 180 / Math.PI);
        }

        private static double calcM(double lat) {
                double latR = lat * Math.PI / 180;
                double b = a * (1 - f);
                double e2 = (2 * f) - (f * f);
                double e4 = e2 * e2;
                double e6 = e4 * e2;
                double A0 = 1 - (e2 / 4) - (3 * e4 / 64) - (5 * e6 / 256);
                double A2 = (3.0 / 8.0) * (e2 + (e4 / 4) + (15 * e6 / 128));
                double A4 = (15.0 / 256.0) * (e4 + (3 * e6 / 4));
                double A6 = 35.0 * e6 / 3072.0;

                double m = a * ((A0 * latR) - (A2 * Math.sin(2 * latR)) + (A4 * Math.sin(4 * latR))
                                - (A6 * Math.sin(6 * latR)));
                return m;
        }
}
