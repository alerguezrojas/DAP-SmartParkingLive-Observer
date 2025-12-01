package smartparking.kdd.model;

public record Location(double lat, double lon) {
    public double distanceTo(Location other) {
        // Haversine formula approximation or simple euclidean for small distances
        // Using simple euclidean for simplicity as per original Kdd logic if it was simple, 
        // but let's do a proper Haversine since it was "RANGO_KM_SUSCRIPCION"
        double R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
