package cn.shopex.ecshopx.promotions.service.membersignin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 领奖流水 reward_items 快照读写 */
public final class MemberSignInRewardSnapshot {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private MemberSignInRewardSnapshot() {}

	public static String writeDailyPoints(ObjectMapper objectMapper, int points) {
		return writeItems(objectMapper, List.of(pointsItem(points)));
	}

	public static String writeTierItems(ObjectMapper objectMapper, List<Map<String, Object>> items) {
		return writeItems(objectMapper, items);
	}

	public static List<Map<String, Object>> parseItems(ObjectMapper objectMapper, String snapshot) {
		if (!StringUtils.hasText(snapshot)) {
			return List.of();
		}
		try {
			Map<String, Object> root = objectMapper.readValue(snapshot, MAP_TYPE);
			Object items = root.get("reward_items");
			if (items instanceof List<?> list) {
				List<Map<String, Object>> out = new ArrayList<>();
				for (Object item : list) {
					if (item instanceof Map<?, ?> map) {
						Map<String, Object> copy = new LinkedHashMap<>();
						map.forEach((k, v) -> copy.put(String.valueOf(k), v));
						out.add(copy);
					}
				}
				return out;
			}
			return parseLegacy(root);
		} catch (JsonProcessingException ex) {
			return List.of();
		}
	}

	private static List<Map<String, Object>> parseLegacy(Map<String, Object> root) {
		List<Map<String, Object>> out = new ArrayList<>();
		Object points = root.get("points");
		if (points instanceof Number number && number.intValue() > 0) {
			out.add(pointsItem(number.intValue()));
		}
		Object couponId = root.get("coupon_id");
		if (couponId instanceof Number number && number.longValue() > 0L) {
			Map<String, Object> coupon = new LinkedHashMap<>();
			coupon.put("type", "coupon");
			coupon.put("coupon_id", number.longValue());
			out.add(coupon);
		}
		return out;
	}

	public static Map<String, Object> pointsItem(int points) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", "points");
		item.put("amount", points);
		return item;
	}

	public static Map<String, Object> couponItem(long couponId, String title) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", "coupon");
		item.put("coupon_id", couponId);
		if (StringUtils.hasText(title)) {
			item.put("title", title);
		}
		return item;
	}

	private static String writeItems(ObjectMapper objectMapper, List<Map<String, Object>> items) {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("reward_items", items);
		try {
			return objectMapper.writeValueAsString(root);
		} catch (JsonProcessingException ex) {
			return root.toString();
		}
	}
}
