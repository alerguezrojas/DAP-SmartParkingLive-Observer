package smartparking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "carparks")
public class Carpark {

    @Id
    @Column(name = "carpark_number")
    private String carparkNumber;

    private String address;
    
    @Column(name = "x_coord")
    private String xCoord;
    
    @Column(name = "y_coord")
    private String yCoord;
    
    @Column(name = "car_park_type")
    private String carparkType;
    
    @Column(name = "type_of_parking_system")
    private String typeOfParkingSystem;
    
    @Column(name = "short_term_parking")
    private String shortTermParking;
    
    @Column(name = "free_parking")
    private String freeParking;
    
    @Column(name = "night_parking")
    private String nightParking;
    
    @Column(name = "car_park_decks")
    private String carparkDecks;
    
    @Column(name = "gantry_height")
    private String gantryHeight;
    
    @Column(name = "car_park_basement")
    private String carparkBasement;

    public Carpark() {}

    public Carpark(String carparkNumber, String address, String xCoord, String yCoord, String carparkType, String typeOfParkingSystem, String shortTermParking, String freeParking, String nightParking, String carparkDecks, String gantryHeight, String carparkBasement) {
        this.carparkNumber = carparkNumber;
        this.address = address;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.carparkType = carparkType;
        this.typeOfParkingSystem = typeOfParkingSystem;
        this.shortTermParking = shortTermParking;
        this.freeParking = freeParking;
        this.nightParking = nightParking;
        this.carparkDecks = carparkDecks;
        this.gantryHeight = gantryHeight;
        this.carparkBasement = carparkBasement;
    }

    public String getCarparkNumber() { return carparkNumber; }
    public void setCarparkNumber(String carparkNumber) { this.carparkNumber = carparkNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getxCoord() { return xCoord; }
    public void setxCoord(String xCoord) { this.xCoord = xCoord; }

    public String getyCoord() { return yCoord; }
    public void setyCoord(String yCoord) { this.yCoord = yCoord; }

    public String getCarparkType() { return carparkType; }
    public void setCarparkType(String carparkType) { this.carparkType = carparkType; }

    public String getTypeOfParkingSystem() { return typeOfParkingSystem; }
    public void setTypeOfParkingSystem(String typeOfParkingSystem) { this.typeOfParkingSystem = typeOfParkingSystem; }

    public String getShortTermParking() { return shortTermParking; }
    public void setShortTermParking(String shortTermParking) { this.shortTermParking = shortTermParking; }

    public String getFreeParking() { return freeParking; }
    public void setFreeParking(String freeParking) { this.freeParking = freeParking; }

    public String getNightParking() { return nightParking; }
    public void setNightParking(String nightParking) { this.nightParking = nightParking; }

    public String getCarparkDecks() { return carparkDecks; }
    public void setCarparkDecks(String carparkDecks) { this.carparkDecks = carparkDecks; }

    public String getGantryHeight() { return gantryHeight; }
    public void setGantryHeight(String gantryHeight) { this.gantryHeight = gantryHeight; }

    public String getCarparkBasement() { return carparkBasement; }
    public void setCarparkBasement(String carparkBasement) { this.carparkBasement = carparkBasement; }
}
