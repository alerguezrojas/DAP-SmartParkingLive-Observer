package smartparking.util;

public class SVY21Test {
    public static void main(String[] args) {
        // Example coordinates for Plaza Singapura (approx)
        // X: 29586.836, Y: 31056.545 (SVY21) -> Lat: 1.299, Lon: 103.845 (approx)

        // Let's try some known coordinates.
        // Changi Airport: N 35832.6562, E 44347.0117
        double N = 35832.6562;
        double E = 44347.0117;

        SVY21Converter.LatLon latLon = SVY21Converter.computeLatLon(N, E);
        System.out.println("N: " + N + ", E: " + E);
        System.out.println("Lat: " + latLon.lat);
        System.out.println("Lon: " + latLon.lon);

        if (latLon.lat > 1.2 && latLon.lat < 1.5 && latLon.lon > 103.6 && latLon.lon < 104.1) {
            System.out.println("Coordinates look valid for Singapore.");
        } else {
            System.out.println("Coordinates look INVALID.");
        }
    }
}
