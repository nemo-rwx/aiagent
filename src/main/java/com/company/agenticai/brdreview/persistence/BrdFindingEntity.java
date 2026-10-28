
package com.company.agenticai.brdreview.persistence;

import jakarta.persistence.*;

@Entity
@Table(
        name = "brd_review_findings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_finding_id",
                columnNames = {"review_id", "finding_id"}
        )
)
public class BrdFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finding_id", nullable = false, length = 30)
    private String findingId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 1000)
    private String requirementReference;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 20)
    private String severity;

    @Lob
    @Column(nullable = false)
    private String recommendation;

    @Lob
    @Column(nullable = false)
    private String sourceText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private BrdReviewEntity review;

    protected BrdFindingEntity() {
        // Required by JPA.
    }

    public BrdFindingEntity(
            String findingId,
            String category,
            String requirementReference,
            String title,
            String description,
            String severity,
            String recommendation,
            String sourceText) {

        this.findingId = findingId;
        this.category = category;
        this.requirementReference = requirementReference;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.recommendation = recommendation;
        this.sourceText = sourceText;
    }

    void setReview(BrdReviewEntity review) {
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public String getFindingId() {
        return findingId;
    }

    public String getCategory() {
        return category;
    }

    public String getRequirementReference() {
        return requirementReference;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getSourceText() {
        return sourceText;
    }
}
