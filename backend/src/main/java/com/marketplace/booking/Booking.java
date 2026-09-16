package com.marketplace.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.candidate.CandidateProfile;
import com.marketplace.employer.EmployerProfile;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends CreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private AppointmentSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employer_id", nullable = true)
    private EmployerProfile employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(nullable = true, length = 2000)
    private String notes;

}

