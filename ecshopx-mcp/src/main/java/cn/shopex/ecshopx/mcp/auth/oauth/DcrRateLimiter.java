package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DcrRateLimiter {

	private static final long WINDOW_MS = 60_000L;

	private final McpProperties mcpProperties;

	private final ConcurrentMap<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();

	public DcrRateLimiter(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	public boolean tryAcquire(String clientKey) {
		String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
		int limit = Math.max(1, mcpProperties.getDcr().getRateLimitPerMinute());
		long now = System.currentTimeMillis();
		long windowStart = now - WINDOW_MS;
		Deque<Long> window = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (window) {
			while (!window.isEmpty() && window.peekFirst() < windowStart) {
				window.removeFirst();
			}
			if (window.size() >= limit) {
				return false;
			}
			window.addLast(now);
			return true;
		}
	}
}