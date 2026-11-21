package com.batuhanturnay.leaveflow.common.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata shown at the top of Swagger UI ({@code /swagger-ui.html}).
 * The endpoints themselves are discovered from the controllers.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

	@Bean
	OpenAPI leaveflowOpenApi() {
		return new OpenAPI()
			.info(new Info().title("LeaveFlow API")
				.version("v1")
				.description("""
						HR leave management REST API.

						Errors are returned as RFC 9457 `application/problem+json`. \
						Validation messages are in English by default; send `Accept-Language: tr` for Turkish."""))
			.externalDocs(new ExternalDocumentation().description("Source code and README")
				.url("https://github.com/lafleur06/leaveflow"));
	}

}
