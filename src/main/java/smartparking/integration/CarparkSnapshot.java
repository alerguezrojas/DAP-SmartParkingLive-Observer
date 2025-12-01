package smartparking.integration;

import java.time.Instant;
import java.util.List;

public record CarparkSnapshot(
        String carparkNumber,
        Instant updatedAt,
        List<CarparkTypeInfo> types
) {
    public record CarparkTypeInfo(
            String lotType,
            int totalLots,
            int availableLots
    ) {}
}

