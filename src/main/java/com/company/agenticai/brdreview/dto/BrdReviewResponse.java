
package com.company.agenticai.brdreview.dto;

import java.util.List;

public record BrdReviewResponse(
        String status,
        String summary,
        int completenessScore,
        List<Finding> findings,
        List<String> clarificationQuestions
) {
    public record Finding(
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
