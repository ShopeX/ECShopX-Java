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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleMainItem;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderFreightFeeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsRecommendCheckoutAddService {

	public static final String CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS =
			"_checkout_recommend_merge_request_items";

	private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(5);

	private static final Set<String> SUPPORTED_ORDER_TYPES =
			Set.of(
					"normal",
					"normal_drug",
					"normal_shopguide",
					"normal_community",
					"normal_excard",
					"normal_employee_purchase",
					"normal_shopadmin");

	private final GoodsRecommendRuleMainItemMapper mainItemMapper;

	private final GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	private final ItemsMapper itemsMapper;

	private final GoodsRecommendSalabilityResolver salabilityResolver;

	private final GoodsRecommendGoodsIdResolver goodsIdResolver;

	private final WxappOrderFreightFeeService wxappOrderFreightFeeService;

	private final MessageSource messageSource;

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper;

	public GoodsRecommendCheckoutAddService(
			GoodsRecommendRuleMainItemMapper mainItemMapper,
			GoodsRecommendRuleRecommendItemMapper recommendItemMapper,
			ItemsMapper itemsMapper,
			GoodsRecommendSalabilityResolver salabilityResolver,
			GoodsRecommendGoodsIdResolver goodsIdResolver,
			WxappOrderFreightFeeService wxappOrderFreightFeeService,
			MessageSource messageSource,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.mainItemMapper = mainItemMapper;
		this.recommendItemMapper = recommendItemMapper;
		this.itemsMapper = itemsMapper;
		this.salabilityResolver = salabilityResolver;
		this.goodsIdResolver = goodsIdResolver;
		this.wxappOrderFreightFeeService = wxappOrderFreightFeeService;
		this.messageSource = messageSource;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> checkoutAdd(
			HttpServletRequest request,
			long companyId,
			Map<String, Object> input,
			Map<String, Object> h5AuthClaims,
			long distributorId) {
		String clientRequestId = stringVal(input.get("client_request_id"));
		if (StringUtils.hasText(clientRequestId)) {
			Map<String, Object> cached = loadIdempotentResult(companyId, h5AuthClaims, clientRequestId);
			if (cached != null) {
				return cached;
			}
		}

		String orderType = stringVal(input.get("order_type")).toLowerCase();
		if (!SUPPORTED_ORDER_TYPES.contains(orderType)) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ORDER_TYPE_UNSUPPORTED);
		}

		long mainItemId = longVal(input.get("main_item_id"), 0L);
		long recommendItemId = longVal(input.get("recommend_item_id"), 0L);
		int num = (int) Math.max(1, longVal(input.get("num"), 1L));

		if (mainItemId <= 0 || recommendItemId <= 0) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}

		validateRecommendRelation(companyId, mainItemId, recommendItemId);

		Items mainItem = loadItem(companyId, mainItemId);
		Items recommendItem = loadItem(companyId, recommendItemId);
		Map<Long, Items> defaultSkuBySpuId =
				salabilityResolver.loadDefaultSkus(companyId, List.of(mainItem, recommendItem));
		Map<Long, Integer> maxSkuStoreBySpuId =
				salabilityResolver.loadMaxSkuStoreBySpuId(companyId, List.of(mainItem, recommendItem));
		GoodsRecommendSalabilityResolver.DistributorContext distributorContext =
				salabilityResolver.loadDistributorContext(
						companyId, distributorId, List.of(mainItemId, recommendItemId));
		Map<Long, DistributorItems> distributorItems = distributorContext.byItemId();
		if (!salabilityResolver.isPairSellable(
				mainItem,
				recommendItem,
				companyId,
				distributorId,
				distributorItems,
				defaultSkuBySpuId,
				maxSkuStoreBySpuId,
				distributorContext.storeBySpuId())) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}

		Map<String, Object> params = new LinkedHashMap<>(input);
		params.remove("main_item_id");
		params.remove("recommend_item_id");
		params.remove("num");
		params.remove("client_request_id");

		List<Map<String, Object>> items = extractItemLines(params.get("items"));
		boolean memberCartCheckout = isMemberCartCheckout(params);
		if (items.isEmpty() && !memberCartCheckout) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_CONTEXT_INVALID);
		}

		long checkoutSkuId =
				salabilityResolver.resolveCheckoutSkuItemId(
						recommendItem,
						companyId,
						distributorContext,
						maxSkuStoreBySpuId,
						distributorContext.storeBySpuId());

		if (memberCartCheckout) {
			// cart_type=cart：与 getFreightFee 一致，请求体可无 items；由 OrderCheckoutCartPort 拉购物车再 merge 推荐行
			List<Map<String, Object>> mergeRows = new ArrayList<>(items);
			mergeRecommendLine(mergeRows, checkoutSkuId, num);
			params.put("items", mergeRows);
		} else {
			mergeRecommendLine(items, checkoutSkuId, num);
			params.put("items", items);
		}
		params.put(CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS, Boolean.TRUE);

		Map<String, Object> result =
				wxappOrderFreightFeeService.getOrderFreightFeeInfo(request, params, h5AuthClaims);
		if (StringUtils.hasText(clientRequestId)) {
			storeIdempotentResult(companyId, h5AuthClaims, clientRequestId, result);
		}
		return result;
	}

	private Map<String, Object> loadIdempotentResult(
			long companyId, Map<String, Object> h5AuthClaims, String clientRequestId) {
		try {
			String cached = stringRedisTemplate.opsForValue().get(idempotencyKey(companyId, h5AuthClaims, clientRequestId));
			if (!StringUtils.hasText(cached)) {
				return null;
			}
			return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private void storeIdempotentResult(
			long companyId, Map<String, Object> h5AuthClaims, String clientRequestId, Map<String, Object> result) {
		try {
			stringRedisTemplate
					.opsForValue()
					.set(
							idempotencyKey(companyId, h5AuthClaims, clientRequestId),
							objectMapper.writeValueAsString(result),
							IDEMPOTENCY_TTL);
		} catch (Exception ignored) {
			// 幂等缓存失败不影响主流程
		}
	}

	private static String idempotencyKey(
			long companyId, Map<String, Object> h5AuthClaims, String clientRequestId) {
		long userId = longVal(h5AuthClaims != null ? h5AuthClaims.get("user_id") : null, 0L);
		return "goods_recommend:checkout_add:" + companyId + ":" + userId + ":" + clientRequestId.trim();
	}

	private void validateRecommendRelation(long companyId, long mainItemId, long recommendItemId) {
		Items mainItem = loadItem(companyId, mainItemId);
		Items recommendItem = loadItem(companyId, recommendItemId);
		long mainGoodsId = goodsIdResolver.resolveGoodsId(mainItem);
		long recommendGoodsId = goodsIdResolver.resolveGoodsId(recommendItem);
		GoodsRecommendRuleMainItem mainRow =
				mainItemMapper.selectOne(
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleMainItem::getGoodsId, mainGoodsId)
								.last("LIMIT 1"));
		if (mainRow == null) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
		long count =
				recommendItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleRecommendItem::getRuleId, mainRow.getRuleId())
								.eq(GoodsRecommendRuleRecommendItem::getGoodsId, recommendGoodsId));
		if (count == 0) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
	}

	private Items loadItem(long companyId, long itemId) {
		Items item =
				itemsMapper.selectOne(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.eq(Items::getItemId, itemId)
								.last("LIMIT 1"));
		if (item == null) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
		return item;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> extractItemLines(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof Map<?, ?> map) {
				out.add(new LinkedHashMap<>((Map<String, Object>) map));
			}
		}
		return out;
	}

	private static boolean isMemberCartCheckout(Map<String, Object> params) {
		return "cart".equalsIgnoreCase(stringVal(params.get("cart_type")));
	}

	private static void mergeRecommendLine(List<Map<String, Object>> lines, long checkoutSkuId, int num) {
		for (Map<String, Object> line : lines) {
			if (longVal(line.get("item_id"), 0L) == checkoutSkuId) {
				int existing = (int) longVal(line.get("num"), 0L);
				line.put("num", existing + num);
				return;
			}
		}
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", checkoutSkuId);
		line.put("num", num);
		line.put("activity_type", "normal");
		lines.add(line);
	}

	private BadRequestException badRequest(String errorCode) {
		return new BadRequestException(GoodsRecommendErrorMessages.message(messageSource, errorCode));
	}

	private static long longVal(Object raw, long def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}
}
