
package com.company.agenticai.brdreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BrdReviewAgentApplication {

	public static void main(String[] args) {
		String apiKey = System.getenv("OPENROUTER_API_KEY");
		System.out.println(
				"OpenRouter API key configured: "
						+ (apiKey != null && !apiKey.isBlank())
		);
		SpringApplication.run(BrdReviewAgentApplication.class, args);
	}


}
