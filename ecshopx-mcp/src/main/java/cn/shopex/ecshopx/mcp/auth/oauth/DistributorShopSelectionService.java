package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loopback helpers mirroring admin shop-list + select-shop after distributor login.
 */
@Service
public class DistributorShopSelectionService {

	static final String LIST_PATH = "/api/v1/distributors";

	static final String SELECT_PATH = "/api/v1/operator/select/distributor";

	private final ApiLoopbackClient loopbackClient;

	private final ObjectMapper objectMapper;

	public DistributorShopSelectionService(ApiLoopbackClient loopbackClient, ObjectMapper objectMapper) {
		this.loopbackClient = loopbackClient;
		this.objectMapper = objectMapper;
	}

	public ListResult listShops(String bearerJwt) {
		if (!StringUtils.hasText(bearerJwt)) {
			return ListResult.failure("missing bearer");
		}
		Map<String, String> headers = bearerHeaders(bearerJwt);
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("page", 1);
		query.put("pageSize", 100);
		query.put("is_all", true);
		LoopbackResponse response = loopbackClient.execute(
				new LoopbackRequest("GET", LIST_PATH, query, headers, null));
		if (response.status() < 200 || response.status() >= 300) {
			return ListResult.failure("List shops failed (HTTP " + response.status() + ")");
		}
		try {
			JsonNode root = objectMapper.readTree(response.body() == null ? "{}" : response.body());
			int code = root.path("code").asInt(200);
			if (code != 200 && code != 0) {
				return ListResult.failure(root.path("msg").asText("List shops failed"));
			}
			JsonNode data = root.path("data");
			JsonNode listNode = data.path("list");
			if (!listNode.isArray()) {
				listNode = data.path("items");
			}
			List<PendingDistributorAuthStore.ShopOption> shops = new ArrayList<>();
			if (listNode.isArray()) {
				for (JsonNode row : listNode) {
					long id = readDistributorId(row);
					if (id <= 0L) {
						continue;
					}
					String name = firstText(row, "name", "distributor_name", "shop_name");
					if (!StringUtils.hasText(name)) {
						name = "店铺 " + id;
					}
					shops.add(new PendingDistributorAuthStore.ShopOption(id, name));
				}
			}
			return ListResult.success(shops);
		}
		catch (Exception ex) {
			return ListResult.failure("Unable to parse shop list");
		}
	}

	public SelectResult selectShop(String bearerJwt, long distributorId) {
		if (!StringUtils.hasText(bearerJwt)) {
			return SelectResult.failure("missing bearer");
		}
		if (distributorId <= 0L) {
			return SelectResult.failure("set_distributor_id is required");
		}
		Map<String, String> headers = bearerHeaders(bearerJwt);
		String body;
		try {
			body = objectMapper.writeValueAsString(Map.of("set_distributor_id", distributorId));
		}
		catch (Exception ex) {
			return SelectResult.failure("Unable to serialize select body");
		}
		LoopbackResponse response = loopbackClient.execute(
				new LoopbackRequest("POST", SELECT_PATH, Map.of(), headers, body));
		if (response.status() < 200 || response.status() >= 300) {
			return SelectResult.failure("Select shop failed (HTTP " + response.status() + ")");
		}
		try {
			JsonNode root = objectMapper.readTree(response.body() == null ? "{}" : response.body());
			int code = root.path("code").asInt(200);
			if (code != 200 && code != 0) {
				return SelectResult.failure(root.path("msg").asText("Select shop failed"));
			}
			JsonNode status = root.path("data").path("status");
			if (status.isBoolean() && !status.booleanValue()) {
				return SelectResult.failure("Select shop returned status=false");
			}
			return SelectResult.ok();
		}
		catch (Exception ex) {
			return SelectResult.failure("Unable to parse select shop response");
		}
	}

	private static Map<String, String> bearerHeaders(String bearerJwt) {
		String token = bearerJwt.strip();
		if (!token.regionMatches(true, 0, "Bearer ", 0, 7)) {
			token = "Bearer " + token;
		}
		return Map.of(HttpHeaders.AUTHORIZATION, token);
	}

	private static long readDistributorId(JsonNode row) {
		JsonNode id = row.get("distributor_id");
		if (id == null || id.isNull()) {
			id = row.get("shop_id");
		}
		if (id == null || id.isNull()) {
			return 0L;
		}
		if (id.isNumber()) {
			return id.asLong();
		}
		String text = id.asText("").trim();
		if (text.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(text);
		}
		catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String firstText(JsonNode row, String... keys) {
		for (String key : keys) {
			JsonNode n = row.get(key);
			if (n != null && n.isTextual() && StringUtils.hasText(n.asText())) {
				return n.asText().trim();
			}
		}
		return "";
	}

	public record ListResult(boolean success, List<PendingDistributorAuthStore.ShopOption> shops, String errorMessage) {

		public static ListResult success(List<PendingDistributorAuthStore.ShopOption> shops) {
			return new ListResult(true, List.copyOf(shops), null);
		}

		public static ListResult failure(String message) {
			return new ListResult(false, List.of(), message);
		}
	}

	public record SelectResult(boolean success, String errorMessage) {

		public static SelectResult ok() {
			return new SelectResult(true, null);
		}

		public static SelectResult failure(String message) {
			return new SelectResult(false, message);
		}
	}
}
