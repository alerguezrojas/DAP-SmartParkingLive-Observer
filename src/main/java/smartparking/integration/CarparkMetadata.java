package smartparking.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CarparkMetadata(
    @JsonProperty("car_park_no") String carparkNumber,
    @JsonProperty("address") String address,
    @JsonProperty("x_coord") String xCoord,
    @JsonProperty("y_coord") String yCoord,
    @JsonProperty("car_park_type") String carparkType,
    @JsonProperty("type_of_parking_system") String typeOfParkingSystem,
    @JsonProperty("short_term_parking") String shortTermParking,
    @JsonProperty("free_parking") String freeParking,
    @JsonProperty("night_parking") String nightParking,
    @JsonProperty("car_park_decks") String carparkDecks,
    @JsonProperty("gantry_height") String gantryHeight,
    @JsonProperty("car_park_basement") String carparkBasement
) {}
