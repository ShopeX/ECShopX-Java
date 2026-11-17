package cn.shopex.ecshopx.mcp.loopback;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class ApiLoopbackClient {

	private static final Logger log = LoggerFactory.getLogger(ApiLoopbackClient.class);

	private final RestClient restClient;

	public ApiLoopbackClient(McpProperties properties, RestClient.Builder restClientBuilder) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.getConnectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.getReadTimeout());
		this.restClient = restClientBuilder
				.baseUrl(properties.getApiBaseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	public LoopbackResponse execute(LoopbackRequest request) {
		validatePath(request.path());
		long startNanos = System.nanoTime();
		boolean authorizationPresent = hasAuthorization(request.headers());

		LoopbackResponse response = exchange(request);

		long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
		log.info(
				"loopback http.method={} http.path={} http.status={} latency_ms={} authorization_present={}",
				request.method(),
				request.path(),
				response.status(),
				latencyMs,
				authorizationPresent);

		return response;
	}

	private LoopbackResponse exchange(LoopbackRequest request) {
		HttpMethod httpMethod = HttpMethod.valueOf(request.method().toUpperCase());
		RestClient.RequestBodySpec spec = restClient.method(httpMethod)
				.uri(uriBuilder -> {
					uriBuilder.path(request.path());
					if (request.query() != null) {
						request.query().forEach(uriBuilder::queryParam);
					}
					return uriBuilder.build();
				})
				.headers(httpHeaders -> applyHeaders(httpHeaders, request.headers()));
		if (StringUtils.hasText(request.body())) {
			spec = spec.contentType(MediaType.APPLICATION_JSON).body(request.body());
		}
		return spec.exchange((httpRequest, clientResponse) -> {
			String body;
			try (var in = clientResponse.getBody()) {
				body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			return new LoopbackResponse(clientResponse.getStatusCode().value(), body);
		});
	}

	private static void applyHeaders(HttpHeaders httpHeaders, Map<String, String> headers) {
		if (headers == null || headers.isEmpty()) {
			return;
		}
		headers.forEach(httpHeaders::set);
	}

	private static boolean hasAuthorization(Map<String, String> headers) {
		return headers != null
				&& headers.entrySet().stream()
						.anyMatch(e -> HttpHeaders.AUTHORIZATION.equalsIgnoreCase(e.getKey())
								&& StringUtils.hasText(e.getValue()));
	}

	static void validatePath(String path) {
		if (!StringUtils.hasText(path)
				|| !path.startsWith("/")
				|| path.startsWith("//")
				|| path.contains("://")) {
			throw new IllegalArgumentException(
					"Invalid loopback path: must start with '/' and must not be authority-relative or contain '://'");
		}
	}
}