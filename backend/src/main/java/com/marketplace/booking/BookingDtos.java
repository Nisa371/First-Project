package com.marketplace.booking;
import java.time.Instant;
import jakarta.validation.constraints.*;
public final class BookingDtos {
    private BookingDtos() {}
    public record SlotRequest(@NotNull @Future Instant startTime, @NotNull @Future Instant endTime,
        @Min(1) @Max(20) int capacity) {}
    public record SlotView(Long id, Instant startTime, Instant endTime, int capacity, long remaining, boolean active) {}
    public record BookingRequest(@NotNull @Positive Long slotId, @NotNull BookingPurpose purpose, @Size(max=2000) String notes) {}
    public record BookingView(Long id, SlotView slot, String candidateName, BookingPurpose purpose, BookingStatus status, String notes) {}
}
