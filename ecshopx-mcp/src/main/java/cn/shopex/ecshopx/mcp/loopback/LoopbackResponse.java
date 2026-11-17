package cn.shopex.ecshopx.mcp.loopback;

import java.util.Collections;
import java.util.Map;

public record LoopbackResponse(int status, String body, Map<String, String> headers) {

	public LoopbackResponse(int status, String body) {
		this(status, body, Map.of());
	}

	public String header(String name) {
		if (headers == null || headers.isEmpty() || name == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (name.equalsIgnoreCase(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public LoopbackResponse {
		headers = headers == null ? Map.of() : Collections.unmodifiableMap(headers);
	}
}