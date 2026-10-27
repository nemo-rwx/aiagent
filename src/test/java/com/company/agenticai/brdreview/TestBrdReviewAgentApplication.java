package com.company.agenticai.brdreview;

import org.springframework.boot.SpringApplication;

public class TestBrdReviewAgentApplication {

	public static void main(String[] args) {
		SpringApplication.from(BrdReviewAgentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
