package com.marketplace.admin;
import com.marketplace.job.*;
import com.marketplace.booking.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminOperationsController {
    private final JobService jobs;
    private final JobApplicationService applications;
    private final BookingService bookings;
    public record NewJob(@NotNull Long employerId,@NotNull @Valid JobDtos.JobRequest job) {}
    @PostMapping("/jobs") public JobDtos.JobView create(@Valid @RequestBody NewJob input) { return jobs.adminCreate(input.employerId(),input.job()); }
    @PutMapping("/jobs/{id}") public JobDtos.JobView update(@PathVariable Long id,@Valid @RequestBody JobDtos.JobRequest input) { return jobs.adminUpdate(id,input); }
    @PostMapping("/jobs/{id}/close") public JobDtos.JobView close(@PathVariable Long id) { return jobs.adminClose(id); }
    @PostMapping("/jobs/{id}/activate") public JobDtos.JobView activate(@PathVariable Long id) { return jobs.adminActivate(id); }
    // Removal is deliberately non-destructive, preserving all applications and payment history.
    @DeleteMapping("/jobs/{id}") public JobDtos.JobView remove(@PathVariable Long id) { return jobs.adminClose(id); }
    @GetMapping("/applications/{id}") public JobDtos.Applicant application(@PathVariable Long id) { return applications.adminView(id); }
    @PatchMapping("/applications/{id}/status") public JobDtos.Applicant status(@PathVariable Long id,@Valid @RequestBody JobDtos.StatusRequest input) { return applications.adminStatus(id,input.status()); }
    @PostMapping("/bookings/{id}/cancel") public BookingDtos.BookingView cancel(@PathVariable Long id) { return bookings.adminCancel(id); }
}
