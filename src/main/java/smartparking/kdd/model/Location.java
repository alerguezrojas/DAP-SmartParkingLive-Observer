package smartparking.kdd.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Location {
    private double lat;
    private double lon;

    public Location() {} // Constructor vacio requerido por JPA

    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }

    public double distanceTo(Location other) {
        double R = 6371; 
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
