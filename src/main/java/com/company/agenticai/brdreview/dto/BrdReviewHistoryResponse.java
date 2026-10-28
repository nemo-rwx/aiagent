
package com.company.agenticai.brdreview.dto;

import java.time.Instant;
import java.util.List;

public record BrdReviewHistoryResponse(
        Long id,
        String status,
        String summary,
        int completenessScore,
        String brdText,
        Instant createdAt,
        List<FindingHistory> findings,
        List<String> clarificationQuestions
) {
    public record FindingHistory(
            String id,
            String category,
            String requirementReference,
            String title,
            String description,
            String severity,
            String recommendation,
            String sourceText
    ) {}
}
