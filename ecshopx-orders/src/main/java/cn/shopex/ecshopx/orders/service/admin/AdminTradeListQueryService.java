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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.TradeListDistributorRow;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeListDistributorRowMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.support.TradeListTimeStartFromRequest;
import cn.shopex.ecshopx.orders.service.admin.support.TradeListTimeStartParams;
import cn.shopex.ecshopx.orders.service.admin.support.TradeTimeStartColumnConditions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminTradeListQueryService {

	private static final DateTimeFormatter PAY_DATE_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter PAY_DATE_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final ZoneId PAY_DATE_ZONE = ZoneId.of("Asia/Shanghai");

	private final TradeMapper tradeMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final TradeListDistributorRowMapper tradeListDistributorRowMapper;
	private final OperatorsMapper operatorsMapper;
	private final ObjectMapper objectMapper;

	public AdminTradeListQueryService(
			TradeMapper tradeMapper,
			NormalOrdersMapper normalOrdersMapper,
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			TradeListDistributorRowMapper tradeListDistributorRowMapper,
			OperatorsMapper operatorsMapper,
			ObjectMapper objectMapper) {
		this.tradeMapper = tradeMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.tradeListDistributorRowMapper = tradeListDistributorRowMapper;
		this.operatorsMapper = operatorsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getTradelist(Map<String, Object> jwtMap, HttpServletRequest request) {
		int datapassBlock = parseDatapassBlock(request);
		long companyId = parseLongSafe(jwtMap.get("company_id"));
		if (companyId <= 0L) {
			return emptyEnvelope(datapassBlock);
		}

		Long merchantIdFilter = null;
		String operatorType = str(jwtMap.get("operator_type")).trim();
		if ("merchant".equalsIgnoreCase(operatorType)) {
			Long mid = parseLongObject(jwtMap.get("merchant_id"));
			if (mid != null && mid > 0L) {
				merchantIdFilter = mid;
			}
		}

		List<String> shopIdInFromJwt = parseJwtShopIds(jwtMap);
		Set<Long> jwtDistributorIdSet = parseJwtDistributorIds(jwtMap);

		String filterTradeState = null;
		String statusParam = request.getParameter("status");
		if (StringUtils.hasText(statusParam)) {
			filterTradeState = statusParam.trim().toUpperCase(Locale.ROOT);
		}

		String mobileRaw = request.getParameter("mobile");
		String mobilePlain11 = null;
		String tradeIdExact = null;
		if (StringUtils.hasText(mobileRaw)) {
			String m = mobileRaw.trim();
			if (m.length() == 11) {
				mobilePlain11 = m;
			} else {
				tradeIdExact = m;
			}
		}

		String orderIdFilter = null;
		String orderIdParam = request.getParameter("orderId");
		if (StringUtils.hasText(orderIdParam)) {
			orderIdFilter = orderIdParam.trim();
		}

		TradeListTimeStartParams tw = TradeListTimeStartFromRequest.resolve(request);
		String timeStartBegin = tw.timeStartBegin();
		String timeStartEnd = tw.timeStartEnd();

		List<String> shopIdsForFilter = null;
		String shopOverride = request.getParameter("shop_id");
		if (StringUtils.hasText(shopOverride)) {
			shopIdsForFilter = null;
		} else if (!shopIdInFromJwt.isEmpty()) {
			shopIdsForFilter = new ArrayList<>(shopIdInFromJwt);
		}
		String shopIdSingle = null;
		if (StringUtils.hasText(shopOverride)) {
			shopIdSingle = shopOverride.trim();
		}

		Long queryDistributorId = parseQueryDistributorId(request);
		Long selectedDistributorId = parseLongObject(jwtMap.get("distributor_id"));
		List<String> distributorIdInStrings = null;
		Long distributorEq = null;
		if (!jwtDistributorIdSet.isEmpty()) {
			if (queryDistributorId != null) {
				if (!jwtDistributorIdSet.contains(queryDistributorId)) {
					queryDistributorId = null;
				} else {
					distributorEq = queryDistributorId;
				}
			} else if (selectedDistributorId != null && jwtDistributorIdSet.contains(selectedDistributorId)) {
				distributorEq = selectedDistributorId;
			} else {
				distributorIdInStrings =
						jwtDistributorIdSet.stream().map(String::valueOf).collect(Collectors.toList());
			}
		} else if (queryDistributorId != null) {
			distributorEq = queryDistributorId;
		}

		List<String> tradeSourceIn = null;
		String tradeSourceEq = null;
		String orderType = request.getParameter("order_type");
		if (StringUtils.hasText(orderType)) {
			String ot = orderType.trim();
			switch (ot) {
				case "service" ->
					tradeSourceIn = List.of("service", "groups", "seckill");
				case "normal" ->
					tradeSourceIn =
							List.of("normal", "normal_groups", "normal_seckill", "normal_community");
				case "diposit" -> tradeSourceEq = "diposit";
				case "order_pay" -> tradeSourceEq = "order_pay";
				default -> {
					/* no trade_source_type filter */
				}
			}
		}

		int pageNo = parsePageInt(request, "page", 1, 1000);
		int pageSize = parsePageInt(request, "pageSize", 20, 1000);

		Long userIdFromMobile = null;
		if (mobilePlain11 != null) {
			Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, mobilePlain11);
			if (uid != null) {
				userIdFromMobile = uid;
			}
		}

		LambdaQueryWrapper<Trade> w = new LambdaQueryWrapper<>();
		w.eq(Trade::getCompanyId, String.valueOf(companyId));
		if (merchantIdFilter != null) {
			w.eq(Trade::getMerchantId, merchantIdFilter);
		}
		if (filterTradeState != null) {
			w.eq(Trade::getTradeState, filterTradeState);
		}
		if (orderIdFilter != null) {
			w.eq(Trade::getOrderId, orderIdFilter);
		}
		TradeTimeStartColumnConditions.apply(w, timeStartBegin, timeStartEnd);
		if (shopIdSingle != null) {
			w.eq(Trade::getShopId, shopIdSingle);
		} else if (shopIdsForFilter != null && !shopIdsForFilter.isEmpty()) {
			w.in(Trade::getShopId, shopIdsForFilter);
		}
		if (distributorEq != null) {
			w.eq(Trade::getDistributorId, String.valueOf(distributorEq));
		} else if (distributorIdInStrings != null && !distributorIdInStrings.isEmpty()) {
			w.in(Trade::getDistributorId, distributorIdInStrings);
		}
		if (tradeSourceIn != null) {
			w.in(Trade::getTradeSourceType, tradeSourceIn);
		} else if (tradeSourceEq != null) {
			w.eq(Trade::getTradeSourceType, tradeSourceEq);
		}
		if (userIdFromMobile != null) {
			w.eq(Trade::getUserId, String.valueOf(userIdFromMobile));
		} else if (mobilePlain11 != null) {
			w.eq(Trade::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain11));
		} else if (tradeIdExact != null) {
			w.eq(Trade::getTradeId, tradeIdExact);
		}

		boolean orderByExpire = StringUtils.hasText(orderIdFilter);
		if (orderByExpire) {
			w.orderByDesc(Trade::getTimeExpire).orderByDesc(Trade::getTimeStart);
		} else {
			w.orderByDesc(Trade::getTimeStart);
		}

		long total = tradeMapper.selectCount(w);
		Page<Trade> page = new Page<>(pageNo, pageSize, false);
		tradeMapper.selectPage(page, w);

		List<Map<String, Object>> listRows = new ArrayList<>();
		List<Trade> records = page.getRecords();
		for (Trade t : records) {
			listRows.add(tradeToListRow(t));
		}

		if (!listRows.isEmpty()) {
			enrichOrderAndOperators(companyId, records, listRows);
			enrichDistributorNames(companyId, listRows);
			applyMobileMask(listRows, datapassBlock);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", listRows);
		out.put("datapass_block", datapassBlock);
		return out;
	}

	private void applyMobileMask(List<Map<String, Object>> listRows, int datapassBlock) {
		for (Map<String, Object> row : listRows) {
			if (!row.containsKey("mobile")) {
				continue;
			}
			Object raw = row.get("mobile");
			String asString = raw instanceof String s ? s : raw == null ? null : String.valueOf(raw);
			if (raw == null || !StringUtils.hasText(asString) || isNullishToken(asString.trim())) {
				row.put("mobile", datapassBlock != 0 ? "" : null);
				continue;
			}
			String trimmed = asString.trim();
			row.put("mobile", DataMasking.maskMobileIfBlocked(trimmed, datapassBlock));
		}
	}

	private void enrichDistributorNames(long companyId, List<Map<String, Object>> listRows) {
		if (listRows.isEmpty()) {
			return;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : listRows) {
			Object raw = row.get("distributorId");
			if (raw == null) {
				continue;
			}
			Long did = parseLongObject(raw instanceof String s ? s : String.valueOf(raw));
			if (did != null && did > 0L) {
				idSet.add(did);
			}
		}
		if (idSet.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>(idSet);
		List<TradeListDistributorRow> rows =
				tradeListDistributorRowMapper.selectList(
						new LambdaQueryWrapper<TradeListDistributorRow>()
								.eq(TradeListDistributorRow::getCompanyId, companyId)
								.in(TradeListDistributorRow::getDistributorId, ids));
		Map<Long, String> idToName =
				rows.stream()
						.collect(
								Collectors.toMap(
										TradeListDistributorRow::getDistributorId,
										r -> r.getName() == null ? "" : r.getName(),
										(a, b) -> a));
		for (Map<String, Object> row : listRows) {
			Object raw = row.get("distributorId");
			if (raw == null) {
				continue;
			}
			Long did = parseLongObject(raw instanceof String s ? s : String.valueOf(raw));
			if (did == null || did <= 0L) {
				continue;
			}
			if (idToName.containsKey(did)) {
				row.put("distributor_name", Objects.requireNonNullElse(idToName.get(did), ""));
			}
		}
	}

	private void enrichOrderAndOperators(long companyId, List<Trade> records, List<Map<String, Object>> listRows) {
		List<Long> orderIds = new ArrayList<>();
		for (Trade t : records) {
			String oidStr = t.getOrderId();
			if (!StringUtils.hasText(oidStr)) {
				continue;
			}
			try {
				orderIds.add(Long.parseLong(oidStr.trim()));
			} catch (NumberFormatException ignored) {
				// skip invalid id
			}
		}
		if (orderIds.isEmpty()) {
			return;
		}
		List<NormalOrders> orderRows =
				normalOrdersMapper.selectList(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.in(NormalOrders::getOrderId, orderIds)
								.eq(NormalOrders::getOrderType, "normal"));
		Map<Long, NormalOrders> byOrderId =
				orderRows.stream().collect(Collectors.toMap(NormalOrders::getOrderId, o -> o, (a, b) -> a));

		List<Long> operatorIds =
				byOrderId.values().stream()
						.map(NormalOrders::getSelfDeliveryOperatorId)
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();
		Map<Long, Operators> opById = new LinkedHashMap<>();
		if (!operatorIds.isEmpty()) {
			List<Operators> ops =
					operatorsMapper.selectList(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, companyId)
									.in(Operators::getOperatorId, operatorIds));
			for (Operators op : ops) {
				if (op.getOperatorId() != null) {
					opById.put(op.getOperatorId(), op);
				}
			}
		}

		for (int i = 0; i < records.size(); i++) {
			Trade t = records.get(i);
			Map<String, Object> row = listRows.get(i);
			String oidStr = t.getOrderId();
			if (!StringUtils.hasText(oidStr)) {
				continue;
			}
			long oid;
			try {
				oid = Long.parseLong(oidStr.trim());
			} catch (NumberFormatException e) {
				continue;
			}
			NormalOrders o = byOrderId.get(oid);
			if (o == null) {
				continue;
			}
			row.put("receipt_type", o.getReceiptType());
			row.put("order_holder", o.getOrderHolder());
			row.put("self_delivery_fee", o.getSelfDeliveryFee());
			long selfOp = o.getSelfDeliveryOperatorId() == null ? 0L : o.getSelfDeliveryOperatorId();
			row.put("self_delivery_operator_id", selfOp);
			if (selfOp > 0L) {
				Operators op = opById.get(selfOp);
				row.put("self_delivery_operator_mobile", op != null && op.getMobile() != null ? op.getMobile() : "");
				row.put("self_delivery_operator_name", op != null && op.getUsername() != null ? op.getUsername() : "");
			} else {
				row.put("self_delivery_operator_mobile", "");
				row.put("self_delivery_operator_name", "");
			}
		}
	}

	private Map<String, Object> tradeToListRow(Trade t) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		putJsonColumn(row, "discountInfo", t.getDiscountInfo());
		putJsonColumn(row, "couponInfo", t.getCouponInfo());
		putJsonColumn(row, "initalRequest", t.getInitalRequest());
		putInitalResponseStringColumn(row, t.getInitalResponse());
		row.put("paymentParams", t.getPaymentParams());
		row.put("tradeId", t.getTradeId());
		row.put("orderId", t.getOrderId());
		row.put("companyId", t.getCompanyId());
		row.put("shopId", t.getShopId());
		row.put("distributorId", t.getDistributorId());
		row.put("dealerId", t.getDealerId());
		row.put("tradeSourceType", t.getTradeSourceType());
		row.put("userId", t.getUserId());
		row.put("mobile", t.getMobile());
		row.put("openId", t.getOpenId());
		row.put("mchId", t.getMchId());
		row.put("feeType", t.getFeeType());
		row.put("tradeNo", t.getTradeNo());
		row.put("tradeState", t.getTradeState());
		row.put("payType", t.getPayType());
		row.put("payChannel", t.getPayChannel());
		row.put("transactionId", t.getTransactionId());
		row.put("authorizerAppid", t.getAuthorizerAppid());
		row.put("wxaAppid", t.getWxaAppid());
		row.put("bankType", t.getBankType());
		row.put("body", t.getBody());
		row.put("detail", t.getDetail());
		row.put("timeStart", t.getTimeStart());
		row.put("timeExpire", t.getTimeExpire());
		row.put("divMembers", t.getDivMembers());
		row.put("adapayFeeMode", t.getAdapayFeeMode());
		row.put("adapayDivStatus", t.getAdapayDivStatus());
		row.put("curFeeType", t.getCurFeeType());
		row.put("curFeeRate", t.getCurFeeRate());
		row.put("curFeeSymbol", t.getCurFeeSymbol());
		row.put("bspayReqDate", t.getBspayReqDate());
		row.put("bspayDivMembers", t.getBspayDivMembers());
		row.put("bspayDivStatus", t.getBspayDivStatus());
		row.put("bspayFeeMode", t.getBspayFeeMode());
		row.put("totalFee", t.getTotalFee());
		row.put("discountFee", t.getDiscountFee());
		row.put("payFee", t.getPayFee());
		row.put("refundedFee", t.getRefundedFee());
		row.put("adapayFee", t.getAdapayFee());
		row.put("couponFee", t.getCouponFee());
		row.put("curPayFee", t.getCurPayFee());
		row.put("bspayFee", t.getBspayFee());
		row.put("merchantId", t.getMerchantId());
		row.put("supplierId", t.getSupplierId());
		row.put("isSettled", t.getIsSettled());
		row.put("payDate", computePayDate(t.getTimeExpire()));
		return row;
	}

	private void putJsonColumn(LinkedHashMap<String, Object> row, String key, String raw) {
		if (raw == null || raw.isBlank()) {
			row.put(key, null);
			return;
		}
		try {
			row.put(key, objectMapper.readValue(raw.trim(), Object.class));
		} catch (JsonProcessingException e) {
			row.put(key, null);
		}
	}

	private void putInitalResponseStringColumn(LinkedHashMap<String, Object> row, String raw) {
		if (raw == null || raw.isBlank()) {
			row.put("initalResponse", null);
			return;
		}
		row.put("initalResponse", raw.trim());
	}

	/**
	 * Formats {@code timeExpire} as {@code yyyy-MM-dd HH:mm:ss} in the Asia/Shanghai zone.
	 * <p>
	 * Blank input, or the literals {@code null} / {@code undefined} (ignoring case), yield an empty string.
	 * If the trimmed value contains only digits, it is interpreted as a Unix epoch: values with 13 or more
	 * digits are treated as milliseconds, otherwise as seconds, then converted with {@code Instant} in the
	 * zone above. Otherwise the value is parsed as {@code yyyy-MM-dd HH:mm:ss}, then as ISO-8601 local
	 * date-time; any parse failure yields an empty string.
	 */
	private String computePayDate(String timeExpire) {
		if (!StringUtils.hasText(timeExpire)) {
			return "";
		}
		String s = timeExpire.trim();
		if (isNullishToken(s)) {
			return "";
		}
		String fromEpoch = payDateFromNumericEpochString(s);
		if (StringUtils.hasText(fromEpoch)) {
			return fromEpoch;
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, PAY_DATE_IN);
			return ldt.format(PAY_DATE_OUT);
		} catch (DateTimeParseException e) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				return ldt.format(PAY_DATE_OUT);
			} catch (DateTimeParseException e2) {
				return "";
			}
		}
	}

	private static boolean isNullishToken(String s) {
		return "null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s);
	}

	private String payDateFromNumericEpochString(String s) {
		if (s.isEmpty() || !isAsciiDigitsOnly(s)) {
			return "";
		}
		try {
			long n = Long.parseLong(s);
			Instant instant;
			if (s.length() >= 13) {
				instant = Instant.ofEpochMilli(n);
			} else {
				instant = Instant.ofEpochSecond(n);
			}
			return LocalDateTime.ofInstant(instant, PAY_DATE_ZONE).format(PAY_DATE_OUT);
		} catch (DateTimeException | NumberFormatException | ArithmeticException e) {
			return "";
		}
	}

	private static boolean isAsciiDigitsOnly(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private Map<String, Object> emptyEnvelope(int datapassBlock) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0L);
		out.put("list", List.of());
		out.put("datapass_block", datapassBlock);
		return out;
	}

	private static int parseDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}

	private static int parsePageInt(HttpServletRequest request, String name, int defaultValue, int maxPageSize) {
		String raw = request.getParameter(name);
		if (!StringUtils.hasText(raw)) {
			return defaultValue;
		}
		int v;
		try {
			v = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
		if ("pageSize".equals(name)) {
			if (v < 1) {
				return 20;
			}
			return Math.min(v, maxPageSize);
		}
		if (v < 1) {
			return 1;
		}
		return v;
	}

	private static Long parseQueryDistributorId(HttpServletRequest request) {
		String dRaw = request.getParameter("distributor_id");
		if (dRaw == null) {
			return null;
		}
		String t = dRaw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			long v = new BigDecimal(t).longValue();
			if (v == 0L) {
				return null;
			}
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<String> parseJwtShopIds(Map<String, Object> jwtMap) {
		Object raw = jwtMap.get("shop_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Object sid = m.get("shop_id");
			if (sid == null) {
				sid = m.get("shopId");
			}
			if (sid != null) {
				String s = String.valueOf(sid).trim();
				if (StringUtils.hasText(s)) {
					out.add(s);
				}
			}
		}
		return out;
	}

	private Set<Long> parseJwtDistributorIds(Map<String, Object> jwtMap) {
		Object raw = jwtMap.get("distributor_ids");
		List<?> list = null;
		if (raw instanceof List<?> l) {
			list = l;
		} else if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				list = objectMapper.readValue(s, new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException e) {
				list = null;
			}
		}
		if (list == null || list.isEmpty()) {
			return Set.of();
		}
		Set<Long> out = new LinkedHashSet<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Object did = m.get("distributor_id");
			if (did == null) {
				continue;
			}
			Long v = parseLongObject(did instanceof String s ? s : String.valueOf(did));
			if (v != null && v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private static long parseLongSafe(Object v) {
		Long x = parseLongObject(v);
		return x == null ? 0L : x;
	}

	private static Long parseLongObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return new BigDecimal(s).longValue();
			} catch (NumberFormatException e2) {
				return null;
			}
		}
	}

	private static String str(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
