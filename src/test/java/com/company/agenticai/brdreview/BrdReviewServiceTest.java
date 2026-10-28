
package com.company.agenticai.brdreview.service;

import com.company.agenticai.brdreview.dto.BrdReviewRequest;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.company.agenticai.brdreview.persistence.BrdReviewRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrdReviewServiceTest {

    private OpenRouterClient openRouterClient;
    private BrdReviewService brdReviewService;
    private BrdReviewRepository brdReviewRepository;

    @BeforeEach
    void setUp() {
        openRouterClient = mock(OpenRouterClient.class);
        brdReviewRepository = mock(BrdReviewRepository.class);

        brdReviewService = new BrdReviewService(
                openRouterClient,
                new ObjectMapper(),
                brdReviewRepository
        );
    }

    @Test
    void shouldParseValidAiResponse() {
        String aiJson = """
                {
                  "status": "SUCCESS",
                  "summary": "BRD review completed.",
                  "completenessScore": 85,
                  "findings": [
                    {
                      "id": "BRD-001",
                      "category": "Acceptance Criteria",
                      "requirementReference": "",
                      "title": "Missing acceptance criteria",
                      "description": "Acceptance criteria are unclear.",
                      "severity": "MEDIUM",
                      "recommendation": "Add measurable acceptance criteria.",
                      "sourceText": "The system shall process payments."
                    }
                  ],
                  "clarificationQuestions": [
                    "What is the expected payment processing time?"
                  ]
                }
                """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        BrdReviewResponse response = brdReviewService.reviewBrd(
                new BrdReviewRequest(
                        "The system shall process payments."
                )
        );

        assertEquals("SUCCESS", response.status());
        assertEquals(85, response.completenessScore());
        assertEquals(1, response.findings().size());
        assertEquals("BRD-001", response.findings().get(0).id());
        assertEquals(
                "Acceptance Criteria",
                response.findings().get(0).category()
        );
        assertEquals(
                "Missing acceptance criteria",
                response.findings().get(0).title()
        );
        assertEquals(1, response.clarificationQuestions().size());

        verify(openRouterClient).chat(anyString());
    }

    @Test
    void shouldRejectBlankBrd() {
        BrdReviewRequest request = new BrdReviewRequest("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> brdReviewService.reviewBrd(request)
        );

        verifyNoInteractions(openRouterClient);
    }

    @Test
    void shouldRejectInvalidAiJson() {
        when(openRouterClient.chat(anyString()))
                .thenReturn("This is not valid JSON");

        BrdReviewRequest request = new BrdReviewRequest(
                "The system shall process payments."
        );

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(request)
        );
    }

    @Test
    void shouldRejectInvalidSeverity() {
        String aiJson = """
                {
                  "status": "SUCCESS",
                  "summary": "Review completed.",
                  "completenessScore": 80,
                  "findings": [
                    {
                      "id": "BRD-001",
                      "category": "Other",
                      "title": "Example finding",
                      "description": "Example description.",
                      "severity": "CRITICAL",
                      "recommendation": "Review this requirement.",
                      "sourceText": "Example requirement."
                    }
                  ],
                  "clarificationQuestions": []
                }
                """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        BrdReviewRequest request = new BrdReviewRequest(
                "The system shall process payments."
        );

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(request)
        );
    }

    @Test
    void shouldRejectDuplicateFindingIds() {
        String aiJson = """
                {
                  "status": "SUCCESS",
                  "summary": "Review completed.",
                  "completenessScore": 80,
                  "findings": [
                    {
                      "id": "BRD-001",
                      "category": "Completeness",
                      "title": "First finding",
                      "description": "First description.",
                      "severity": "HIGH",
                      "recommendation": "Clarify the requirement.",
                      "sourceText": "Example requirement."
                    },
                    {
                      "id": "BRD-001",
                      "category": "Ambiguity",
                      "title": "Second finding",
                      "description": "Second description.",
                      "severity": "LOW",
                      "recommendation": "Clarify the wording.",
                      "sourceText": "Example requirement."
                    }
                  ],
                  "clarificationQuestions": []
                }
                """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest("Example requirement.")
                )
        );
    }

    @Test
    void shouldRejectInvalidFindingCategory() {
        String aiJson = """
                {
                  "status": "SUCCESS",
                  "summary": "Review completed.",
                  "completenessScore": 80,
                  "findings": [
                    {
                      "id": "BRD-001",
                      "category": "SecurityRisk",
                      "title": "Example finding",
                      "description": "Example description.",
                      "severity": "HIGH",
                      "recommendation": "Review the requirement.",
                      "sourceText": "Example requirement."
                    }
                  ],
                  "clarificationQuestions": []
                }
                """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest("Example requirement.")
                )
        );
    }

    @Test
    void shouldRejectMalformedFindingId() {
        String aiJson = """
                {
                  "status": "SUCCESS",
                  "summary": "Review completed.",
                  "completenessScore": 80,
                  "findings": [
                    {
                      "id": "FINDING-1",
                      "category": "Completeness",
                      "title": "Example finding",
                      "description": "Example description.",
                      "severity": "HIGH",
                      "recommendation": "Clarify the requirement.",
                      "sourceText": "Example requirement."
                    }
                  ],
                  "clarificationQuestions": []
                }
                """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest("Example requirement.")
                )
        );
    }


    @Test
    void shouldRejectSourceTextNotPresentInBrd() {
        String aiJson = """
            {
              "status": "SUCCESS",
              "summary": "Review completed.",
              "completenessScore": 80,
              "findings": [
                {
                  "id": "BRD-001",
                  "category": "Completeness",
                  "title": "Missing requirement detail",
                  "description": "A requirement needs clarification.",
                  "severity": "HIGH",
                  "recommendation": "Provide more detail.",
                  "sourceText": "This text does not exist in the BRD."
                }
              ],
              "clarificationQuestions": []
            }
            """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        BrdReviewRequest request = new BrdReviewRequest(
                "The system shall process payments."
        );

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(request)
        );
    }


    @Test
    void shouldAcceptEmptySourceText() {
        String aiJson = """
            {
              "status": "SUCCESS",
              "summary": "Review completed.",
              "completenessScore": 80,
              "findings": [
                {
                  "id": "BRD-001",
                  "category": "Ambiguity",
                  "requirementReference": "",
                  "title": "Requirement is ambiguous",
                  "description": "The expected processing behavior is unclear.",
                  "severity": "MEDIUM",
                  "recommendation": "Specify the expected behavior.",
                  "sourceText": ""
                }
              ],
              "clarificationQuestions": []
            }
            """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        BrdReviewResponse response = brdReviewService.reviewBrd(
                new BrdReviewRequest(
                        "The system shall process payments."
                )
        );

        assertEquals(1, response.findings().size());
        assertEquals("", response.findings().get(0).sourceText());
    }


    @Test
    void shouldRejectUnsupportedRequirementReference() {
        String aiJson = """
            {
              "status": "SUCCESS",
              "summary": "Review completed.",
              "completenessScore": 80,
              "findings": [
                {
                  "id": "BRD-001",
                  "category": "Completeness",
                  "requirementReference": "FR-999",
                  "title": "Missing requirement detail",
                  "description": "The requirement needs clarification.",
                  "severity": "HIGH",
                  "recommendation": "Provide more detail.",
                  "sourceText": "The system shall process payments."
                }
              ],
              "clarificationQuestions": []
            }
            """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest(
                                "The system shall process payments."
                        )
                )
        );
    }

    @Test
    void shouldRejectCompletenessScoreAbove100() {
        String aiJson = """
        {
          "status": "SUCCESS",
          "summary": "Review completed.",
          "completenessScore": 101,
          "findings": [],
          "clarificationQuestions": []
        }
        """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest("The system shall process payments.")
                )
        );
    }

    @Test
    void shouldRejectNegativeCompletenessScore() {
        String aiJson = """
        {
          "status": "SUCCESS",
          "summary": "Review completed.",
          "completenessScore": -1,
          "findings": [],
          "clarificationQuestions": []
        }
        """;

        when(openRouterClient.chat(anyString())).thenReturn(aiJson);

        assertThrows(
                IllegalStateException.class,
                () -> brdReviewService.reviewBrd(
                        new BrdReviewRequest("The system shall process payments.")
                )
        );
    }



}
