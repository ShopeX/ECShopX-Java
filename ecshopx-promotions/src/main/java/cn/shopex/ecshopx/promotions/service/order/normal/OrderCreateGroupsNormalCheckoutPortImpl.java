/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.promotions.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsNormalCheckoutPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsReadService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateGroupsNormalCheckoutPortImpl implements OrderCreateGroupsNormalCheckoutPort {

	private static final String ORDER_VALIDITY_REDIS_KEY = "order_validity_setting";

	private final PromotionGroupsActivityCheckCreateGroupOrderService checkCreateGroupOrderService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService;

	public OrderCreateGroupsNormalCheckoutPortImpl(
			PromotionGroupsActivityCheckCreateGroupOrderService checkCreateGroupOrderService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService) {
		this.checkCreateGroupOrderService = checkCreateGroupOrderService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.promotionGroupsRelGoodsReadService = promotionGroupsRelGoodsReadService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void applyAfterFormat(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_groups".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		PromotionGroupsActivity groupInfo =
				checkCreateGroupOrderService.checkCreateGroupOrder(pr, p.getOrderData());
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long actId = groupInfo.getGroupsActivityId() == null ? 0L : groupInfo.getGroupsActivityId();
		String actName = stringVal(groupInfo.getActName());
		long companyId = longVal(pr.get("company_id"), longVal(od.get("company_id"), 0L));

		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || itemList.isEmpty()) {
			return;
		}
		long sumItemFee = 0L;
		long sumTotalFee = 0L;
		long sumDiscountFee = 0L;
		List<Map<String, Object>> orderDiscountInfo = new ArrayList<>();
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> rawLine)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) rawLine;
			int num = intVal(line.get("num"), 1);
			long itemId = longVal(line.get("item_id"), 0L);
			long actPrice = resolveActPrice(groupInfo, companyId, actId, itemId);
			long itemPrice = resolveItemOriginalPrice(line);
			long itemFee = itemPrice * num;
			long lineTotalFee = actPrice * num;
			long lineDiscountFee = itemFee - lineTotalFee;
			Map<String, Object> discountInfo = new LinkedHashMap<>();
			discountInfo.put("id", String.valueOf(actId));
			discountInfo.put("type", "groups");
			discountInfo.put("info", "拼团团购");
			discountInfo.put("rule", actName);
			discountInfo.put("discount_fee", lineDiscountFee);
			line.put("activity_price", String.valueOf(actPrice));
			line.put("price", itemPrice);
			line.put("item_fee", itemFee);
			line.put("total_fee", lineTotalFee);
			line.put("discount_fee", (int) Math.min(lineDiscountFee, Integer.MAX_VALUE));
			line.put("discount_info", List.of(discountInfo));
			sumItemFee += itemFee;
			sumTotalFee += lineTotalFee;
			sumDiscountFee += lineDiscountFee;
			if (orderDiscountInfo.isEmpty()) {
				orderDiscountInfo.add(discountInfo);
			}
		}
		od.put("item_fee", String.valueOf(sumItemFee));
		od.put("market_fee", String.valueOf(sumItemFee));
		od.put("total_fee", sumTotalFee);
		od.put("discount_fee", (int) Math.min(sumDiscountFee, Integer.MAX_VALUE));
		od.put("discount_info", orderDiscountInfo);
		od.put("act_id", String.valueOf(longVal(pr.get("bargain_id"), actId)));
		long now = Instant.now().getEpochSecond();
		int cancelMinutes = resolveCancelMinutes(companyId);
		long cancelDeadline = now + cancelMinutes * 60L;
		long groupEnd = groupInfo.getEndTime() == null ? cancelDeadline : groupInfo.getEndTime();
		od.put("auto_cancel_time", groupEnd > cancelDeadline ? cancelDeadline : groupEnd);
		if (Boolean.TRUE.equals(groupInfo.getFreePost())) {
			pr.put("_groups_free_post", true);
		}
	}

	private int resolveCancelMinutes(long companyId) {
		if (companyId <= 0L) {
			return 15;
		}
		String raw =
				companysRedisTemplate
						.<String, String>opsForHash()
						.get(ORDER_VALIDITY_REDIS_KEY, String.valueOf(companyId));
		if (!StringUtils.hasText(raw)) {
			return 15;
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			Object cancel = parsed.get("order_cancel_time");
			if (cancel instanceof Number n) {
				return Math.max(1, n.intValue());
			}
			return Math.max(1, Integer.parseInt(String.valueOf(cancel).trim()));
		} catch (Exception e) {
			return 15;
		}
	}

	private long resolveActPrice(
			PromotionGroupsActivity groupInfo, long companyId, long actId, long itemId) {
		long fallback = groupInfo.getActPrice() == null ? 0L : groupInfo.getActPrice();
		if (companyId <= 0L || actId <= 0L || itemId <= 0L) {
			return fallback;
		}
		if (!promotionGroupsRelGoodsReadService.hasRelRows(companyId, actId)) {
			return fallback;
		}
		return promotionGroupsRelGoodsReadService
				.findByActivityAndItem(companyId, actId, itemId)
				.map(rel -> rel.getActivityPrice() == null ? fallback : rel.getActivityPrice())
				.orElse(fallback);
	}

	private static long resolveItemOriginalPrice(Map<String, Object> line) {
		long price = longVal(line.get("price"), 0L);
		if (price > 0L) {
			return price;
		}
		return longVal(line.get("sale_price"), 0L);
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
