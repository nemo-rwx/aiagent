
package com.company.agenticai.brdreview.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "brd_review_questions")
public class BrdClarificationQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private BrdReviewEntity review;

    protected BrdClarificationQuestionEntity() {
        // Required by JPA.
    }

    public BrdClarificationQuestionEntity(String question) {
        this.question = question;
    }

    void setReview(BrdReviewEntity review) {
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }
}
