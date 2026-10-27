
package com.company.agenticai.brdreview.controller;

import com.company.agenticai.brdreview.dto.BrdReviewRequest;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;
import com.company.agenticai.brdreview.service.BrdReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/brd")
public class BrdReviewController {

    private final BrdReviewService brdReviewService;

    public BrdReviewController(BrdReviewService brdReviewService) {
        this.brdReviewService = brdReviewService;
    }

    @PostMapping("/review")
    public ResponseEntity<BrdReviewResponse> reviewBrd(
            @Valid @RequestBody BrdReviewRequest request) {

        BrdReviewResponse response = brdReviewService.reviewBrd(request);
        return ResponseEntity.ok(response);
    }
}
