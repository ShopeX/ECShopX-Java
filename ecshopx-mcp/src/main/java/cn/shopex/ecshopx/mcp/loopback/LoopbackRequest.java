package cn.shopex.ecshopx.mcp.loopback;

import java.util.Map;

public record LoopbackRequest(
		String method,
		String path,
		Map<String, ?> query,
		Map<String, String> headers,
		String body) {
}