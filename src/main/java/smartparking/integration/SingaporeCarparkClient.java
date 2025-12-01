package smartparking.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SingaporeCarparkClient {

    private static final Logger log = LoggerFactory.getLogger(SingaporeCarparkClient.class);
    private static final String ENDPOINT = "https://api.data.gov.sg/v1/transport/carpark-availability";

    private final RestClient restClient;

    public SingaporeCarparkClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(ENDPOINT).build();
    }

    public List<CarparkSnapshot> fetchAll() {
        try {
            CarparkAvailabilityResponse response = restClient.get()
                    .retrieve()
                    .body(CarparkAvailabilityResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                return List.of();
            }

            List<CarparkData> data = response.items().get(0).carparkData();
            if (data == null || data.isEmpty()) {
                return List.of();
            }

            return data.stream()
                    .map(this::mapToSnapshot)
                    .toList();

        } catch (Exception ex) {
            log.warn("No se pudo obtener disponibilidad en vivo: {}", ex.getMessage());
            return List.of();
        }
    }

    private CarparkSnapshot mapToSnapshot(CarparkData data) {
        Instant updatedAt = parseInstant(data.updateDateTime());
        List<CarparkSnapshot.CarparkTypeInfo> types = data.carparkInfo().stream()
                .map(info -> new CarparkSnapshot.CarparkTypeInfo(
                        info.lotType(),
                        toInt(info.totalLots()),
                        toInt(info.lotsAvailable())
                ))
                .toList();

        return new CarparkSnapshot(
                data.carparkNumber(),
                updatedAt,
                types
        );
    }

    private int toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CarparkAvailabilityResponse(List<Item> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Item(@JsonProperty("carpark_data") List<CarparkData> carparkData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CarparkData(
            @JsonProperty("carpark_number") String carparkNumber,
            @JsonProperty("update_datetime") String updateDateTime,
            String status,
            @JsonProperty("carpark_info") List<CarparkInfo> carparkInfo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CarparkInfo(
            @JsonProperty("total_lots") String totalLots,
            @JsonProperty("lots_available") String lotsAvailable,
            @JsonProperty("lot_type") String lotType
    ) {}
}
