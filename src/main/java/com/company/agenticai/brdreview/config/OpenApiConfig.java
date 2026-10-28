
package com.company.agenticai.brdreview.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BRD Review Agent API",
                version = "v1",
                description = """
                        API for reviewing Business Requirements Documents (BRDs).
                        Identifies requirement gaps, inconsistencies, risks,
                        and clarification questions.
                        """
        )
)
@SecurityScheme(
        name = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "Enter your configured BRD API key."
)
public class OpenApiConfig {
}
