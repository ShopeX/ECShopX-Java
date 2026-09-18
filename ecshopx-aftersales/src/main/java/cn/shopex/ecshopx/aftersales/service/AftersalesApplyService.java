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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesApplyService {

	private final ObjectMapper objectMapper;
	private final OrderAssociationReadPort orderAssociationReadPort;
	private final AftersalesApplyShopApplyByNumService aftersalesApplyShopApplyByNumService;
	private final Validator validator;

	public AftersalesApplyService(
			ObjectMapper objectMapper,
			OrderAssociationReadPort orderAssociationReadPort,
			AftersalesApplyShopApplyByNumService aftersalesApplyShopApplyByNumService,
			Validator validator) {
		this.objectMapper = objectMapper;
		this.orderAssociationReadPort = orderAssociationReadPort;
		this.aftersalesApplyShopApplyByNumService = aftersalesApplyShopApplyByNumService;
		this.validator = validator;
	}

	/**
	 * Admin aftersales apply: validates and enriches {@link AftersalesApplyParams}, then routes to
	 * {@link AftersalesApplyShopApplyByNumService#shopApplyByNum} and {@link AftersalesApplyShopApplyByNumHandleService#shopApplyByNumHandle}.
	 *
	 * <p>For most types, after the surrounding transaction commits, {@link cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher}
	 * performs asynchronous trade-refund listener fan-out on the dispatch bus.
	 *
	 * <p>When {@code aftersales_type} is {@code REFUND_GOODS} or {@code EXCHANGING_GOODS}, post-commit ordered work instead
	 * publishes system-link trade-aftersales via {@link cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher},
	 * then third-party trade-aftersales SaaS ERP via {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher}
	 * ({@code DispatchFacade.publishEvent}).
	 *
	 * <p>Shopping-guide (salesperson) flows that use the same admin apply endpoint follow the same stack:
	 * {@code apply} → {@link AftersalesApplyShopApplyByNumService#shopApplyByNum}, then the same post-commit dispatch rules.
	 */
	public Map<String, Object> apply(HttpServletRequest request, AftersalesApplyParams params) {
		mergeJwt(request, params);
		parseAndSetDetail(params);
		validateApplyParams(params);
		params.setGoodsReturned(parseGoodsReturned(params));

		long companyId = params.getCompanyId();
		long orderId = params.getOrderId();
		Optional<Map<String, Object>> assoc = orderAssociationReadPort.getAssociation(companyId, orderId);
		if (assoc.isEmpty()) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		String orderType = String.valueOf(assoc.get().getOrDefault("order_type", ""));
		if (!"normal".equals(orderType)) {
			throw new ResourceException("实体类订单才能申请售后！");
		}

		long userId = longVal(assoc.get().get("user_id"));
		params.setUserId(userId);
		boolean goodsReturned = Boolean.TRUE.equals(params.getGoodsReturned());

		String distParam = request.getParameter("distributor_id");
		if (StringUtils.hasText(distParam)) {
			try {
				params.setDistributorId(Long.parseLong(distParam.trim()));
			} catch (NumberFormatException ignored) {
				params.setDistributorId(0L);
			}
		} else if (params.getDistributorId() == null) {
			params.setDistributorId(0L);
		}

		if (goodsReturned) {
			params.setReturnType("offline");
		}

		long uid = userId;
		if (uid == 0L && "REFUND_GOODS".equalsIgnoreCase(params.getAftersalesType()) && !goodsReturned) {
			throw new ResourceException("匿名订单只能申请仅退款或者到店退货");
		}

		aftersalesApplyShopApplyByNumService.shopApplyByNum(params);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", true);
		ok.put("result", true);
		return ok;
	}

	private void mergeJwt(HttpServletRequest request, AftersalesApplyParams params) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		params.setCompanyId(longVal(companyIdObj));
		params.setOperatorType(str(jwt.get("operator_type")));
		params.setOperatorId(longVal(jwt.get("operator_id")));
	}

	private void parseAndSetDetail(AftersalesApplyParams params) {
		Object raw = params.getDetail();
		if (raw == null) {
			params.setDetailRows(new ArrayList<>());
			params.syncDetailLinesFromRows();
			return;
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> rows = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					rows.add(row);
				}
			}
			params.setDetailRows(rows);
			params.syncDetailLinesFromRows();
			return;
		}
		if (!(raw instanceof String s) || !StringUtils.hasText(s)) {
			params.setDetailRows(new ArrayList<>());
			params.syncDetailLinesFromRows();
			return;
		}
		JsonNode node;
		try {
			node = objectMapper.readTree(s);
		} catch (Exception e) {
			throw new BadRequestException("售后明细商品ID必填");
		}
		if (node == null || !node.isArray()) {
			throw new BadRequestException("售后明细商品ID必填");
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (JsonNode el : node) {
			if (!el.isObject()) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			if (el.has("id")) {
				row.put("id", el.get("id").asText());
			}
			if (el.has("num")) {
				row.put("num", el.get("num").asText());
			}
			if (el.has("total_fee")) {
				row.put("total_fee", el.get("total_fee").asInt());
			}
			if (el.has("total_point")) {
				row.put("total_point", el.get("total_point").asInt());
			}
			rows.add(row);
		}
		params.setDetailRows(rows);
		params.syncDetailLinesFromRows();
	}

	private void validateApplyParams(AftersalesApplyParams params) {
		if (isFullyEmptyApplyRequest(params)) {
			throw new BadRequestException(
					"订单号必填,必须为整数，售后类型必选，售后原因必选，退款金额必填，退还积分必填");
		}
		Set<ConstraintViolation<AftersalesApplyParams>> violations = validator.validate(params);
		if (!violations.isEmpty()) {
			String joined =
					violations.stream()
							.sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
							.map(ConstraintViolation::getMessage)
							.collect(Collectors.joining("，"));
			throw new BadRequestException(joined);
		}
		String fee = params.getRefundFeeRaw() == null ? "" : params.getRefundFeeRaw().trim();
		String pt = params.getRefundPointRaw() == null ? "" : params.getRefundPointRaw().trim();
		List<String> missing = new ArrayList<>();
		if (fee.isEmpty()) {
			missing.add("退款金额必填");
		}
		if (pt.isEmpty()) {
			missing.add("退还积分必填");
		}
		if (!missing.isEmpty()) {
			throw new BadRequestException(String.join("，", missing));
		}
	}

	private static boolean isFullyEmptyApplyRequest(AftersalesApplyParams p) {
		boolean orderInvalid = p.getOrderId() == null || p.getOrderId() < 1L;
		if (!orderInvalid) {
			return false;
		}
		if (StringUtils.hasText(p.getAftersalesType())) {
			return false;
		}
		if (StringUtils.hasText(p.getReason())) {
			return false;
		}
		if (StringUtils.hasText(p.getRefundFeeRaw())) {
			return false;
		}
		if (StringUtils.hasText(p.getRefundPointRaw())) {
			return false;
		}
		Object d = p.getDetail();
		if (d == null) {
			return true;
		}
		if (d instanceof String s && !StringUtils.hasText(s)) {
			return true;
		}
		return d instanceof List<?> list && list.isEmpty();
	}

	private static boolean parseGoodsReturned(AftersalesApplyParams params) {
		Object g = params.getGoodsReturnedRaw();
		if (g instanceof Boolean b) {
			return b;
		}
		String s = str(g);
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
