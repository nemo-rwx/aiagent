
package com.company.agenticai.brdreview.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "brd_reviews")
public class BrdReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(nullable = false)
    private int completenessScore;

    @Lob
    @Column(nullable = false)
    private String brdText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BrdFindingEntity> findings = new ArrayList<>();

    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BrdClarificationQuestionEntity> clarificationQuestions =
            new ArrayList<>();

    protected BrdReviewEntity() {
        // Required by JPA.
    }

    public BrdReviewEntity(
            String status,
            String summary,
            int completenessScore,
            String brdText) {
        this.status = status;
        this.summary = summary;
        this.completenessScore = completenessScore;
        this.brdText = brdText;
        this.createdAt = Instant.now();
    }

    public void addFinding(BrdFindingEntity finding) {
        findings.add(finding);
        finding.setReview(this);
    }

    public void addClarificationQuestion(
            BrdClarificationQuestionEntity question) {
        clarificationQuestions.add(question);
        question.setReview(this);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public int getCompletenessScore() {
        return completenessScore;
    }

    public String getBrdText() {
        return brdText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<BrdFindingEntity> getFindings() {
        return findings;
    }

    public List<BrdClarificationQuestionEntity>
    getClarificationQuestions() {
        return clarificationQuestions;
    }
}
