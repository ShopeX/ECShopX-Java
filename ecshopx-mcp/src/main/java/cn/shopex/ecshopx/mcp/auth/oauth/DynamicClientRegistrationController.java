package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/oauth")
public class DynamicClientRegistrationController {

	private final OAuthClientRegistry clientRegistry;

	private final DcrRateLimiter rateLimiter;

	private final McpProperties mcpProperties;

	public DynamicClientRegistrationController(
			OAuthClientRegistry clientRegistry, DcrRateLimiter rateLimiter, McpProperties mcpProperties) {
		this.clientRegistry = clientRegistry;
		this.rateLimiter = rateLimiter;
		this.mcpProperties = mcpProperties;
	}

	@PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> register(@RequestBody DcrRequest request, HttpServletRequest httpRequest) {
		if (!rateLimiter.tryAcquire(clientIp(httpRequest))) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(Map.of("error", "rate_limit_exceeded",
							"error_description", "DCR rate limit exceeded for this client"));
		}
		try {
			OAuthClientRegistration registration = clientRegistry.register(
					request.redirectUris(),
					request.clientName(),
					request.tokenEndpointAuthMethod());
			return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(registration));
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "invalid_client_metadata", "error_description", ex.getMessage()));
		}
	}

	private static Map<String, Object> toResponse(OAuthClientRegistration registration) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("client_id", registration.clientId());
		body.put("redirect_uris", registration.redirectUris());
		body.put("client_id_issued_at", registration.clientIdIssuedAt());
		if (registration.clientName() != null && !registration.clientName().isBlank()) {
			body.put("client_name", registration.clientName());
		}
		body.put("token_endpoint_auth_method", registration.tokenEndpointAuthMethod());
		return body;
	}

	private String clientIp(HttpServletRequest request) {
		if (mcpProperties.getDcr().isTrustForwardedHeaders()) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return request.getRemoteAddr();
	}

	record DcrRequest(
			@JsonProperty("redirect_uris") List<String> redirectUris,
			@JsonProperty("client_name") String clientName,
			@JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod) {
	}
}