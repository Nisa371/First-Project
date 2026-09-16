package com.marketplace.booking;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "appointment_slots", check = @CheckConstraint(constraint = "capacity > 0 and end_time > start_time"))
public class AppointmentSlot extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "evaluator_user_id", nullable = true)
    private User evaluatorUser;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private int capacity = 1;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

}

