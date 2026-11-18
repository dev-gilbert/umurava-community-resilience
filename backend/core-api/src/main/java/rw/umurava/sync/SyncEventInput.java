package rw.umurava.sync;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record SyncEventInput(
    @NotNull UUID id,
    @NotNull UUID communityId,
    @NotNull UUID deviceId,
    @NotBlank @Size(max = 80) String entityType,
    @NotNull UUID entityId,
    @NotBlank @Size(max = 20) String operation,
    @NotNull JsonNode payload,
    @Positive long eventVersion,
    @NotNull Instant eventCreatedAt
) {}
