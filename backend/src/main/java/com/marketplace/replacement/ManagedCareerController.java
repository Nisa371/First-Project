package com.marketplace.replacement;
import com.marketplace.placement.PlacementService;
import com.marketplace.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequiredArgsConstructor
public class ManagedCareerController {
    private final QueueService queue;
    private final PlacementService placements;
    private final ReplacementQueueManager replacements;
    private final NotificationService notifications;
    public record Join(@NotNull @Positive Long skillId) {}
    public record Hire(@NotNull @Positive Long jobId,@NotNull @Positive Long candidateId) {}
    public record Request(@NotNull @Positive Long placementId,@NotBlank @Size(max=2000) String reason) {}
    @GetMapping("/api/waiting-list/me") public List<QueueService.QueueView> queue() { return queue.mine(); }
    @PostMapping("/api/waiting-list/me") public QueueService.QueueView join(@Valid @RequestBody Join r) { return queue.join(r.skillId()); }
    @PostMapping("/api/waiting-list/{id}/leave") public void leave(@PathVariable Long id) { queue.leave(id); }
    @GetMapping("/api/admin/waiting-list") public List<QueueService.QueueView> allQueue() { return queue.all(); }
    @GetMapping("/api/placements/me") public List<PlacementService.PlacementView> placements() { return placements.mine(); }
    @PostMapping("/api/placements") public PlacementService.PlacementView hire(@Valid @RequestBody Hire r) { return placements.create(r.jobId(),r.candidateId()); }
    @PostMapping("/api/placements/{id}/complete") public PlacementService.PlacementView completePlacement(@PathVariable Long id) { return placements.end(id,true); }
    @PostMapping("/api/placements/{id}/terminate") public PlacementService.PlacementView terminate(@PathVariable Long id) { return placements.end(id,false); }
    @GetMapping({"/api/replacements","/api/admin/replacements"}) public List<ReplacementQueueManager.View> replacements() { return replacements.list(); }
    @GetMapping("/api/replacements/{id}") public ReplacementQueueManager.View replacement(@PathVariable Long id) { return replacements.get(id); }
    @PostMapping("/api/replacements") public ReplacementQueueManager.View request(@Valid @RequestBody Request r) { return replacements.request(r.placementId(),r.reason()); }
    @PostMapping("/api/replacements/{id}/accept") public ReplacementQueueManager.View accept(@PathVariable Long id) { return replacements.accept(id); }
    @PostMapping("/api/replacements/{id}/complete") public ReplacementQueueManager.View complete(@PathVariable Long id) { return replacements.complete(id); }
    @PostMapping("/api/replacements/{id}/cancel") public ReplacementQueueManager.View cancel(@PathVariable Long id) { return replacements.cancel(id); }
    @PostMapping("/api/replacements/{id}/retry") public ReplacementQueueManager.View retry(@PathVariable Long id) { return replacements.retry(id); }
    @GetMapping("/api/notifications/me") public List<NotificationService.View> notifications() { return notifications.mine(); }
    @PostMapping("/api/notifications/{id}/read") public void read(@PathVariable Long id) { notifications.read(id); }
    @PostMapping("/api/notifications/read-all") public void readAll() { notifications.readAll(); }
}
