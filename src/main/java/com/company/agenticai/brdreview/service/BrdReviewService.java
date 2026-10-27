
package com.company.agenticai.brdreview.service;

import com.company.agenticai.brdreview.dto.BrdReviewRequest;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;
import com.company.agenticai.brdreview.prompt.BrdReviewPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BrdReviewService {

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper;

    private static final List<String> VALID_SEVERITIES =
            List.of("HIGH", "MEDIUM", "LOW");

    private static final List<String> VALID_CATEGORIES =
            List.of(
                    "Completeness",
                    "Ambiguity",
                    "Stakeholders",
                    "Testability",
                    "Business Rules",
                    "Acceptance Criteria",
                    "Consistency",
                    "Other"
            );

    public BrdReviewService(
            OpenRouterClient openRouterClient,
            ObjectMapper objectMapper) {
        this.openRouterClient = openRouterClient;
        this.objectMapper = objectMapper;
    }

    public BrdReviewResponse reviewBrd(BrdReviewRequest request) {

        if (request == null
                || request.brdText() == null
                || request.brdText().isBlank()) {
            throw new IllegalArgumentException(
                    "BRD text cannot be empty."
            );
        }

        String brdText = request.brdText().trim();
        String prompt = BrdReviewPrompt.buildPrompt(brdText);
        String aiResponse = openRouterClient.chat(prompt);

        try {
            BrdReviewResponse response = objectMapper.readValue(
                    aiResponse,
                    BrdReviewResponse.class
            );

            validateResponse(response, brdText);
            return response;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to parse the AI response as a valid BRD review. "
                            + "Please try again.",
                    exception
            );
        }
    }

    private void validateResponse(
            BrdReviewResponse response,
            String brdText) {

        if (response == null
                || response.status() == null
                || response.status().isBlank()
                || response.summary() == null
                || response.summary().isBlank()
                || response.findings() == null
                || response.clarificationQuestions() == null) {
            throw new IllegalStateException(
                    "AI returned an incomplete BRD review."
            );
        }

        if (!"SUCCESS".equals(response.status())) {
            throw new IllegalStateException(
                    "AI returned an invalid review status."
            );
        }

        if (response.completenessScore() < 0
                || response.completenessScore() > 100) {
            throw new IllegalStateException(
                    "AI returned a completeness score outside 0-100."
            );
        }

        Set<String> findingIds = new HashSet<>();

        for (BrdReviewResponse.Finding finding : response.findings()) {

            if (finding == null
                    || finding.id() == null
                    || finding.category() == null
                    || finding.title() == null
                    || finding.title().isBlank()
                    || finding.description() == null
                    || finding.description().isBlank()
                    || finding.severity() == null
                    || finding.recommendation() == null
                    || finding.recommendation().isBlank()
                    || finding.sourceText() == null) {
                throw new IllegalStateException(
                        "AI returned a finding with missing or empty fields."
                );
            }

            validateFindingId(finding.id());

            if (!findingIds.add(finding.id())) {
                throw new IllegalStateException(
                        "AI returned duplicate finding IDs."
                );
            }

            if (!VALID_CATEGORIES.contains(finding.category())) {
                throw new IllegalStateException(
                        "AI returned an invalid finding category."
                );
            }

            if (!VALID_SEVERITIES.contains(finding.severity())) {
                throw new IllegalStateException(
                        "AI returned an invalid finding severity."
                );
            }

            if (finding.requirementReference() == null) {
                throw new IllegalStateException(
                        "AI returned a finding with a missing requirement reference."
                );
            }

            String reference = finding.requirementReference().trim();

            if (!reference.isEmpty()) {
                String[] references = reference.split(",");

                for (String item : references) {
                    String candidate = item.trim();

                    if (candidate.isEmpty()
                            || !brdText.contains(candidate)) {
                        throw new IllegalStateException(
                                "AI returned an unsupported requirement reference."
                        );
                    }
                }
            }


            // Ensure quoted source text occurs in the submitted BRD.
            // Empty source text is permitted.
            if (!finding.sourceText().isBlank()
                    && !brdText.contains(finding.sourceText())) {
                throw new IllegalStateException(
                        "AI returned source text that does not match the BRD."
                );
            }
        }

        for (String question : response.clarificationQuestions()) {
            if (question == null || question.isBlank()) {
                throw new IllegalStateException(
                        "AI returned an empty clarification question."
                );
            }
        }
    }

    private void validateFindingId(String id) {
        if (!id.matches("BRD-\\d{3,}")) {
            throw new IllegalStateException(
                    "AI response contains an invalid finding ID."
            );
        }
    }
}
