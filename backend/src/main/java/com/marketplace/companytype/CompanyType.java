package com.marketplace.companytype;

import com.marketplace.common.persistence.TimestampedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_types")
@Getter @Setter
public class CompanyType extends TimestampedEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 120)
    private String normalizedName;
    @Column(unique = true, length = 32)
    private String code;
    @Column(nullable = false)
    private boolean active = true;
    public boolean isOther() { return "OTHER".equals(code); }
}
