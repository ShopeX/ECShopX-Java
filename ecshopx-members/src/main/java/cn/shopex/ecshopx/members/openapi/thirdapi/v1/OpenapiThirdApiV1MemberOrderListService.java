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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.integration.orders.OpenapiMemberOrderListOrdersPort;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberOrderListService {

	private static final Logger log = LoggerFactory.getLogger(OpenapiThirdApiV1MemberOrderListService.class);
	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort;

	public OpenapiThirdApiV1MemberOrderListService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.openapiMemberOrderListOrdersPort = openapiMemberOrderListOrdersPort;
	}

	public Map<String, Object> executeOpenapiMemberOrderLists(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			String externalMemberIdQueryParam,
			String orderClassQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			boolean externalMemberIdPresent,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			boolean unionidPresent,
			String unionidRaw,
			String orderClassRaw,
			int page,
			int pageSize) {
		OrderFilterKey filterKey =
				resolveOrderFilter(
						companyId,
						mobileQueryParam,
						unionidQueryParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidRaw);

		Map<String, Object> orderFilter = new LinkedHashMap<>();
		orderFilter.put("company_id", companyId);
		orderFilter.put("user_id", filterKey.userId());
		orderFilter.put("order_type", "normal");
		orderFilter.put("order_status|notin", List.of("NOTPAY", "CANCEL"));
		if (orderClassRaw != null) {
			orderFilter.put("order_class", orderClassRaw);
		}

		log.info("[getMemberOrderLists] orderFilter={}", orderFilter);

		Map<String, Object> result =
				openapiMemberOrderListOrdersPort.queryOrderItemLists(orderFilter, page, pageSize);
		Map<Object, Long> totalAmountMap = openapiMemberOrderListOrdersPort.sumTotalFeeByUserId(orderFilter);

		@SuppressWarnings("unchecked")
		Map<String, Object> pager = (Map<String, Object>) result.get("pager");
		pager.put("total_amount", totalAmountMap);

		long totalCount = longVal(pager.get("count"));
		result.put("total_count", totalCount);

		long scalarTotal = lookupTotalAmount(totalAmountMap, filterKey.userId());
		result.put("total_amount", scalarTotal);

		long avgAmount = 0L;
		if (totalCount > 0L && scalarTotal != 0L) {
			avgAmount =
					BigDecimal.valueOf(scalarTotal)
							.divide(BigDecimal.valueOf(totalCount), 0, RoundingMode.DOWN)
							.longValue();
		}
		result.put("avg_amount", avgAmount);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		if (list == null || list.isEmpty()) {
			return result;
		}
		for (int i = 0; i < list.size(); i++) {
			list.set(i, trimSingleOrder(list.get(i)));
		}
		return result;
	}

	private OrderFilterKey resolveOrderFilter(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			String externalMemberIdRaw,
			boolean externalMemberIdTruthy,
			String unionidRaw) {
		log.info("[getFilter] mobilePresent={}, mobileTruthy={}", mobilePresent, mobileTruthy);

		if (externalMemberIdTruthy) {
			return new OrderFilterKey(companyId, externalMemberIdRaw);
		}

		if (body != null
				&& body.containsKey("unionid")
				&& body.get("unionid") != null
				&& !(body.get("unionid") instanceof String)) {
			throw new ResourceException("请填写unionid");
		}

		if (mobilePresent && (mobileRaw == null || !MOBILE_PATTERN.matcher(mobileRaw).matches())) {
			throw new ResourceException("请填写正确的手机号");
		}

		if (isPhpEmptyInline(mobileQueryParam, body, "mobile")
				&& isPhpEmptyInline(unionidQueryParam, body, "unionid")) {
			throw new ResourceException("unionid或者手机号必填");
		}

		if (mobileTruthy) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw);
			if (member == null) {
				throw new ResourceException("会员信息获取失败");
			}
			return new OrderFilterKey(companyId, member.getUserId());
		}

		MembersAssociations assoc =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUnionid, unionidRaw)
								.eq(MembersAssociations::getUserType, "wechat")
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("会员信息获取失败");
		}
		return new OrderFilterKey(companyId, assoc.getUserId());
	}

	private static Map<String, Object> trimSingleOrder(Map<String, Object> order) {
		Object orderId = order.get("order_id");
		LinkedHashMap<String, Object> trimmed = new LinkedHashMap<>();
		trimmed.put("order_id", orderId);
		trimmed.put("order_status", order.get("order_status"));
		trimmed.put("order_status_msg", order.get("order_status_msg"));
		trimmed.put("order_status_des", order.get("order_status_des"));
		trimmed.put("pay_status", order.get("pay_status"));
		trimmed.put("total_fee", order.get("total_fee"));
		trimmed.put("user_id", order.get("user_id"));
		trimmed.put("pay_time", formatEpoch(order.get("create_time")));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items =
				order.get("items") instanceof List<?> raw ? (List<Map<String, Object>>) raw : List.of();
		long buyNum = 0L;
		boolean hasGift = false;
		List<Map<String, Object>> trimmedItems = new ArrayList<>();
		for (Map<String, Object> item : items) {
			if ("gift".equals(str(item.get("order_item_type")))) {
				hasGift = true;
			}
			buyNum += longVal(item.get("num"));
			trimmedItems.add(trimItem(item, orderId));
		}
		trimmed.put("buy_num", buyNum);
		trimmed.put("gift", hasGift ? "gift" : "normal");
		trimmed.put("items", trimmedItems);
		return trimmed;
	}

	private static Map<String, Object> trimItem(Map<String, Object> item, Object orderId) {
		LinkedHashMap<String, Object> trimmed = new LinkedHashMap<>();
		trimmed.put("id", item.get("id"));
		trimmed.put("order_id", orderId);
		trimmed.put("user_id", item.get("user_id"));
		trimmed.put("item_id", item.get("item_id"));
		trimmed.put("item_bn", item.get("item_bn"));
		trimmed.put("item_name", item.get("item_name"));
		trimmed.put("pic", item.get("pic"));
		trimmed.put("num", item.get("num"));
		trimmed.put("price", item.get("price"));
		trimmed.put("item_fee", item.get("item_fee"));
		trimmed.put("order_item_type", item.get("order_item_type"));

		String itemSpecDesc = str(item.get("item_spec_desc"));
		if (!itemSpecDesc.isEmpty()) {
			String[] parts = itemSpecDesc.split(":", 2);
			trimmed.put("item_spec", parts.length > 1 ? parts[1] : null);
		} else {
			trimmed.put("item_spec", "");
		}
		return trimmed;
	}

	private record OrderFilterKey(long companyId, Object userId) {}

	private static boolean isPhpEmptyInline(String queryParam, Map<String, Object> body, String key) {
		Object raw = null;
		if (body != null && body.containsKey(key)) {
			raw = body.get(key);
		} else if (queryParam != null) {
			raw = queryParam;
		}
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	private static long lookupTotalAmount(Map<Object, Long> map, Object userId) {
		if (map == null || map.isEmpty() || userId == null) {
			return 0L;
		}
		Long direct = map.get(userId);
		if (direct != null) {
			return direct;
		}
		try {
			long parsed = longVal(userId);
			if (parsed > 0L) {
				Long byLong = map.get(parsed);
				if (byLong != null) {
					return byLong;
				}
			}
		} catch (RuntimeException ignored) {
			// fall through
		}
		Long byString = map.get(String.valueOf(userId));
		return byString != null ? byString : 0L;
	}

	private static String formatEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
