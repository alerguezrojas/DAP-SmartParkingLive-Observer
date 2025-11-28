package smartparking.integration;

import java.time.Instant;

public record CarparkSnapshot(
        String carparkNumber,
        int totalLots,
        int availableLots,
        boolean open,
        Instant updatedAt
) {
}

