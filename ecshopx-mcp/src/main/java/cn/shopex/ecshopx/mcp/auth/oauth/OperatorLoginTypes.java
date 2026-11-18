package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Operator login types for ecshopx-admin MCP, keyed by {@code operator_type}/{@code logintype}.
 */
public final class OperatorLoginTypes {

	public static final String DEFAULT = "admin";

	private static final Map<String, String> LABELS = new LinkedHashMap<>();

	static {
		LABELS.put("admin", "超级管理员");
		LABELS.put("staff", "平台管理员");
		LABELS.put("merchant", "商户");
		LABELS.put("distributor", "店员");
		LABELS.put("supplier", "供应商");
	}

	private OperatorLoginTypes() {
	}

	public static Map<String, String> labels() {
		return Collections.unmodifiableMap(LABELS);
	}

	public static Set<String> allowed() {
		return LABELS.keySet();
	}

	public static String normalize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return DEFAULT;
		}
		String value = raw.trim().toLowerCase();
		if (!LABELS.containsKey(value)) {
			throw new IllegalArgumentException("Unsupported logintype: " + raw);
		}
		return value;
	}
}
