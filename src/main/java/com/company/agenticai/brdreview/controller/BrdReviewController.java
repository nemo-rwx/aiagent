
package com.company.agenticai.brdreview.controller;

import com.company.agenticai.brdreview.dto.BrdReviewHistoryResponse;
import com.company.agenticai.brdreview.dto.BrdReviewRequest;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;
import com.company.agenticai.brdreview.service.BrdReviewService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/brd")
public class BrdReviewController {

    private final BrdReviewService brdReviewService;

    public BrdReviewController(BrdReviewService brdReviewService) {
        this.brdReviewService = brdReviewService;
    }

    @PostMapping("/review")
    @Operation(
            summary = "Review a Business Requirements Document",
            description = "Analyzes a BRD for gaps, inconsistencies, risks, "
                    + "and clarification questions.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "BRD review completed successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request or malformed JSON"),
            @ApiResponse(responseCode = "401",
                    description = "Missing or invalid API key"),
            @ApiResponse(responseCode = "413",
                    description = "Request body too large"),
            @ApiResponse(responseCode = "429",
                    description = "Rate limit exceeded"),
            @ApiResponse(responseCode = "502",
                    description = "AI provider connection error"),
            @ApiResponse(responseCode = "503",
                    description = "Service unavailable"),
            @ApiResponse(responseCode = "504",
                    description = "AI provider timeout")
    })
    public ResponseEntity<BrdReviewResponse> reviewBrd(
            @Valid @RequestBody BrdReviewRequest request) {

        BrdReviewResponse response =
                brdReviewService.reviewBrd(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/{id}")
    @Operation(
            summary = "Retrieve a saved BRD review",
            description = "Retrieves a previously saved BRD review, "
                    + "including its findings and clarification questions.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Saved BRD review retrieved successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid review ID"),
            @ApiResponse(responseCode = "401",
                    description = "Missing or invalid API key"),
            @ApiResponse(responseCode = "404",
                    description = "BRD review not found")
    })
    public ResponseEntity<BrdReviewHistoryResponse> getReviewById(
            @PathVariable Long id) {

        BrdReviewHistoryResponse response =
                brdReviewService.getReviewById(id);

        return ResponseEntity.ok(response);
    }
}
