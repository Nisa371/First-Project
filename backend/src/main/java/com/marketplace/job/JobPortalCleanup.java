package com.marketplace.job;

@org.springframework.stereotype.Component
@org.springframework.scheduling.annotation.EnableScheduling
@lombok.RequiredArgsConstructor
public class JobPortalCleanup {
    private final com.marketplace.placement.PlacementService placements;
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void upgradeLegacyPortals() { placements.upgradeLegacyPortals(); }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=30000, initialDelay=10000)
    public void closeExpiredPortals() { placements.expirePortals(); }
}
