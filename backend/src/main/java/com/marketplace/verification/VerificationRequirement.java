package com.marketplace.verification;
import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.companytype.CompanyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="verification_requirements") @Getter @Setter
public class VerificationRequirement extends TimestampedEntity {
    @Column(nullable=false, length=160) private String name;
    @Column(length=2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private VerificationTarget targetType;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_type_id") private CompanyType companyType;
    @Column(nullable=false) private boolean required = true;
    @Column(nullable=false) private boolean active = true;
    @Column(unique=true, length=40) private String code;
}
