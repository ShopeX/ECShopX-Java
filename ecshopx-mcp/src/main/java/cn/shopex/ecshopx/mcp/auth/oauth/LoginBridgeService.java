package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LoginBridgeService {

	private final ApiLoopbackClient loopbackClient;

	private final ObjectMapper objectMapper;

	public LoginBridgeService(ApiLoopbackClient loopbackClient, ObjectMapper objectMapper) {
		this.loopbackClient = loopbackClient;
		this.objectMapper = objectMapper;
	}

	public LoginResult login(TokenKind tokenKind, Map<String, String> credentials) {
		String path = loginPath(tokenKind);
		String body = buildLoginBody(tokenKind, credentials);
		LoopbackResponse response = loopbackClient.execute(new LoopbackRequest("POST", path, null, null, body));
		return parseLoginResponse(response);
	}

	static String loginPath(TokenKind tokenKind) {
		return switch (tokenKind) {
			case OPERATOR -> "/api/v1/operator/login";
			case POS -> "/api/v1/pos/auth/login";
			case MEMBER -> "/api/v1/h5app/wxapp/login";
		};
	}

	private String buildLoginBody(TokenKind tokenKind, Map<String, String> credentials) {
		Map<String, Object> body = new LinkedHashMap<>();
		switch (tokenKind) {
			case OPERATOR -> {
				putIfPresent(body, "username", credentials.get("username"));
				putIfPresent(body, "password", credentials.get("password"));
				String logintype = credentials.get("logintype");
				body.put("logintype", StringUtils.hasText(logintype) ? logintype : "localadmin");
			}
			case POS -> {
				putIfPresent(body, "employee_no", credentials.get("employee_no"));
				putIfPresent(body, "password", credentials.get("password"));
			}
			case MEMBER -> {
				putIfPresent(body, "company_id", credentials.get("company_id"));
				putIfPresent(body, "username", credentials.get("username"));
				putIfPresent(body, "password", credentials.get("password"));
				String authType = credentials.get("auth_type");
				body.put("auth_type", StringUtils.hasText(authType) ? authType : "local");
				putIfPresent(body, "check_type", credentials.get("check_type"));
			}
		}
		try {
			return objectMapper.writeValueAsString(body);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("unable to serialize login body", ex);
		}
	}

	private static void putIfPresent(Map<String, Object> body, String key, String value) {
		if (StringUtils.hasText(value)) {
			body.put(key, value);
		}
	}

	LoginResult parseLoginResponse(LoopbackResponse response) {
		if (response.status() < 200 || response.status() >= 300) {
			return LoginResult.failure("Login failed (HTTP " + response.status() + ")");
		}
		try {
			JsonNode root = objectMapper.readTree(response.body());
			int code = root.path("code").asInt(-1);
			if (code != 200) {
				String msg = root.path("msg").asText("Login failed");
				return LoginResult.failure(msg);
			}
			JsonNode tokenNode = root.path("data").path("token");
			if (tokenNode.isTextual() && StringUtils.hasText(tokenNode.asText())) {
				return LoginResult.success(tokenNode.asText());
			}
			if (tokenNode.isBoolean() && !tokenNode.booleanValue()) {
				return LoginResult.failure("Invalid username or password");
			}
			return LoginResult.failure("Login response did not contain a token");
		}
		catch (JsonProcessingException ex) {
			return LoginResult.failure("Unable to parse login response");
		}
	}

	public record LoginResult(boolean success, String jwt, String errorMessage) {

		public static LoginResult success(String jwt) {
			return new LoginResult(true, jwt, null);
		}

		public static LoginResult failure(String message) {
			return new LoginResult(false, null, message);
		}
	}
}