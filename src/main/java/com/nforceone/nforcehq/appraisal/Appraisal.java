package com.nforceone.nforcehq.appraisal;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "appraisal_reviews")
@Getter
@Setter
public class Appraisal extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "cycle_name", nullable = false)
    private String cycleName;

    @Column(name = "overall_rating")
    private String overallRating;

    @Column(columnDefinition = "text")
    private String strengths;

    @Column(name = "areas_for_improvement", columnDefinition = "text")
    private String areasForImprovement;

    @Column(name = "goals_next_cycle", columnDefinition = "text")
    private String goalsNextCycle;

    /** DRAFT, SUBMITTED, or ACKNOWLEDGED — plain string rather than a native enum, matching notifications.type. */
    @Column(nullable = false)
    private String status = "DRAFT";
}
