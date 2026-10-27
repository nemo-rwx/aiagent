package com.company.agenticai.brdreview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrdReviewRequest(
        @NotBlank(message = "BRD text must not be empty")
        @Size(max = 100000, message = "BRD text must not exceed 100,000 characters")
        String brdText
) {
}
