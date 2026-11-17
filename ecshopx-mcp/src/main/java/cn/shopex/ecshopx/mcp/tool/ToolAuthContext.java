package cn.shopex.ecshopx.mcp.tool;

import org.springframework.util.StringUtils;

/**
 * Optional request-scoped bearer; {@link LoopbackToolExecutor} prefers an explicit bearer argument.
 */
public final class ToolAuthContext {

	private static final ThreadLocal<String> BEARER = new ThreadLocal<>();

	private ToolAuthContext() {
	}

	public static void setBearer(String bearerToken) {
		if (StringUtils.hasText(bearerToken)) {
			BEARER.set(bearerToken);
		}
	}

	public static String getBearer() {
		return BEARER.get();
	}

	public static void clear() {
		BEARER.remove();
	}
}