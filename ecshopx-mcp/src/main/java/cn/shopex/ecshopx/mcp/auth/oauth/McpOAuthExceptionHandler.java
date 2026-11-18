package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth endpoints must return RFC 6749-shaped errors ({@code error} string).
 * Otherwise Cursor rejects the response as "Invalid OAuth error response".
 */
@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.mcp.auth.oauth")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class McpOAuthExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
		Throwable cause = ex.getMostSpecificCause();
		String detail = cause != null && cause.getMessage() != null ? cause.getMessage() : ex.getMessage();
		return oauthError(HttpStatus.BAD_REQUEST, "invalid_request",
				"Request body is missing or not valid JSON: " + detail);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
		return oauthError(HttpStatus.BAD_REQUEST, "invalid_request",
				"Missing required parameter: " + ex.getParameterName());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex) {
		return oauthError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid_request",
				"Unsupported Content-Type: " + ex.getContentType());
	}

	private static ResponseEntity<Map<String, String>> oauthError(
			HttpStatus status, String error, String description) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("error", error);
		body.put("error_description", description);
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
	}
}
