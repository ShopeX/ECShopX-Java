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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderListService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[345789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;

	public OpenapiThirdApiV2OrderListService(
			MemberAccountService memberAccountService,
			MembersAssociationsMapper membersAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper) {
		this.memberAccountService = memberAccountService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
	}

	public Map<String, Object> executeOpenapiOrderList(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			String unionidRaw,
			int page,
			Integer pageSize,
			boolean pageOverridden) {
		validateParams(mobileQueryParam, unionidQueryParam, body, mobilePresent, mobileRaw);

		long userId = resolveMemberUserId(companyId, mobileRaw, mobileTruthy, unionidRaw);

		LambdaQueryWrapper<NormalOrders> baseWrapper =
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getUserId, userId);

		long count = normalOrdersMapper.selectCount(baseWrapper);
		Long totalFeeCents = normalOrdersMapper.sumTotalFeeCentsByFilter(companyId, userId);
		long totalFeeCentsVal = totalFeeCents != null ? totalFeeCents : 0L;

		if (count <= 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("count", 0);
			empty.put("total_fee", 0);
			empty.put("order_avg", 0);
			empty.put("list", List.of());
			return empty;
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		String totalFeeYuan = centsToYuanString(totalFeeCentsVal);
		String orderAvgYuan = divideYuanString(totalFeeYuan, count);
		result.put("count", count);
		result.put("total_fee", totalFeeYuan);
		result.put("order_avg", orderAvgYuan);

		LambdaQueryWrapper<NormalOrders> listWrapper =
				baseWrapper.clone().orderByDesc(NormalOrders::getCreateTime);
		if (pageOverridden) {
			long offset = (long) (page - 1) * (pageSize != null ? pageSize : 0);
			if (pageSize != null && pageSize > 0) {
				listWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
			}
		} else {
			listWrapper.last("LIMIT 10 OFFSET 0");
		}

		List<NormalOrders> orders = normalOrdersMapper.selectList(listWrapper);
		List<Long> orderIds =
				orders.stream()
						.map(NormalOrders::getOrderId)
						.filter(id -> id != null && id > 0L)
						.toList();
		Map<Long, String> tradeIndex = buildTradeIndex(companyId, orderIds);

		List<Map<String, Object>> flatList = new ArrayList<>();
		for (NormalOrders order : orders) {
			LinkedHashMap<String, Object> orderFields = new LinkedHashMap<>();
			orderFields.put("order_id", String.valueOf(order.getOrderId()));
			orderFields.put("trade_no", tradeIndex.getOrDefault(order.getOrderId(), "-"));
			orderFields.put("order_status", order.getOrderStatus());
			orderFields.put("total_fee", centsToYuanString(longVal(order.getTotalFee())));
			orderFields.put("create_time", order.getCreateTime());

			List<Map<String, Object>> items =
					normalOrdersItemsMapper.selectItemsByCompanyAndOrderId(
							companyId, order.getOrderId());
			if (items == null || items.isEmpty()) {
				continue;
			}
			for (Map<String, Object> item : items) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>(orderFields);
				row.put("item_name", item.get("item_name"));
				row.put("item_id", item.get("item_id"));
				row.put("item_bn", str(item.get("item_bn")));
				row.put("price", centsToYuanString(intVal(item.get("price"))));
				row.put("total_fee", centsToYuanString(intVal(item.get("total_fee"))));
				row.put("num", item.get("num"));
				row.put("item_spec_desc", str(item.get("item_spec_desc")));
				row.put("pic", str(item.get("pic")));
				flatList.add(row);
			}
		}
		result.put("list", flatList);
		return result;
	}

	private void validateParams(
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw) {
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
	}

	private long resolveMemberUserId(
			long companyId, String mobileRaw, boolean mobileTruthy, String unionidRaw) {
		if (mobileTruthy) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw);
			if (member == null) {
				throw new ResourceException("会员信息获取失败");
			}
			return member.getUserId();
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
		return assoc.getUserId();
	}

	private Map<Long, String> buildTradeIndex(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> rows = tradeMapper.selectSuccessTradeIndexRows(companyId, orderIds);
		Map<Long, String> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long oid = longVal(row.get("order_id"));
			String tn = str(row.get("trade_no"));
			out.put(oid, (StringUtils.hasText(tn) && !"0".equals(tn)) ? tn : "-");
		}
		return out;
	}

	private static String centsToYuanString(long cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String divideYuanString(String yuanTotal, long count) {
		return new BigDecimal(yuanTotal)
				.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

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

	private static int intVal(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
