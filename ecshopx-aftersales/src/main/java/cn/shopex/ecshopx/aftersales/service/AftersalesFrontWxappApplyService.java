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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Wxapp aftersales apply: validates the request, builds {@link AftersalesApplyParams}, and delegates
 * persistence to {@link AftersalesApplyShopApplyByNumService#shopApplyByNum}.
 * <p>
 * After the database transaction for that apply commits, {@link AftersalesApplyShopApplyByNumHandleService}
 * runs ordered post-commit publishers; eligible paths include {@link cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher}
 * fan-out (async trade-refund dispatch) alongside other integrations.
 */
@Service
public class AftersalesFrontWxappApplyService {

	private static final char FW_COMMA = '\uFF0C';

	private final ShopMenuService shopMenuService;
	private final OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort;
	private final OrderAssociationReadPort orderAssociationReadPort;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final AftersalesApplyShopApplyByNumService aftersalesApplyShopApplyByNumService;
	private final ObjectMapper objectMapper;

	public AftersalesFrontWxappApplyService(
			ShopMenuService shopMenuService,
			OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort,
			OrderAssociationReadPort orderAssociationReadPort,
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			AftersalesApplyShopApplyByNumService aftersalesApplyShopApplyByNumService,
			ObjectMapper objectMapper) {
		this.shopMenuService = shopMenuService;
		this.orderValidityPlatformSettingReadPort = orderValidityPlatformSettingReadPort;
		this.orderAssociationReadPort = orderAssociationReadPort;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.aftersalesApplyShopApplyByNumService = aftersalesApplyShopApplyByNumService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Runs the wxapp apply path and calls {@link AftersalesApplyShopApplyByNumService#shopApplyByNum}.
	 *
	 * @apiNote TradeRefund-oriented async fan-out and other post-commit publishers are invoked from the
	 * shop-by-num handle layer after commit, not from this facade.
	 */
	public Map<String, Object> apply(HttpServletRequest request, LinkedHashMap<String, Object> merged) {
		Map<String, Object> auth = mergeAuth(request);
		long companyId = parseLongStrict(auth.get("company_id"), "企业id必填");
		long authUserId = parseLongStrict(auth.get("user_id"), "会员id必填");
		merged.put("company_id", companyId);
		if (merged.containsKey("self_delivery_operator_id")
				&& selfDeliveryOperatorTruthy(merged.get("self_delivery_operator_id"))) {
			long uidFromBody = parseLongStrict(merged.get("user_id"), "会员id必填");
			merged.put("self_delivery_operator_id", uidFromBody);
		} else {
			merged.put("user_id", authUserId);
		}

		validateApplyMatrix(merged);

		parseAndAttachDetail(merged);

		String returnType = str(merged.get("return_type"));
		if ("offline".equalsIgnoreCase(returnType)) {
			String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
			if (!"platform".equals(productModel)) {
				Map<String, Object> setting = orderValidityPlatformSettingReadPort.readPlatformSetting(companyId);
				if (!truthyOfflineAftersales(setting.get("offline_aftersales"))) {
					throw new ResourceException("未开启到店退货");
				}
			}
		}

		long orderId = longVal(merged.get("order_id"));
		Optional<Map<String, Object>> assoc = orderAssociationReadPort.getAssociation(companyId, orderId);
		if (assoc.isEmpty()) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		String orderType = str(assoc.get().getOrDefault("order_type", ""));
		if (!"normal".equals(orderType)) {
			throw new ResourceException("实体类订单才能申请售后！");
		}

		Optional<Map<String, Object>> headerOpt = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		long distributorId = 0L;
		if (headerOpt.isPresent()) {
			distributorId = longVal(headerOpt.get().get("distributor_id"));
		}

		AftersalesApplyParams params = buildApplyParams(merged, companyId, distributorId);
		aftersalesApplyShopApplyByNumService.shopApplyByNum(params);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", Boolean.TRUE);
		ok.put("result", Boolean.TRUE);
		return ok;
	}

	private AftersalesApplyParams buildApplyParams(
			LinkedHashMap<String, Object> merged, long companyId, long distributorId) {
		AftersalesApplyParams p = new AftersalesApplyParams();
		p.setCompanyId(companyId);
		p.setOrderId(longVal(merged.get("order_id")));
		p.setUserId(longVal(merged.get("user_id")));
		p.setAftersalesType(str(merged.get("aftersales_type")));
		p.setReason(str(merged.get("reason")));
		p.setDescription(merged.get("description") == null ? null : str(merged.get("description")));
		p.setEvidencePic(merged.get("evidence_pic"));
		if (merged.containsKey("refund_fee")) {
			p.setRefundFeeRaw(str(merged.get("refund_fee")));
		} else {
			p.setRefundFeeRaw("");
		}
		if (merged.containsKey("refund_point")) {
			p.setRefundPointRaw(str(merged.get("refund_point")));
		} else {
			p.setRefundPointRaw("");
		}
		p.setFreight(intVal(merged.get("freight")));
		p.setDistributorId(distributorId);
		p.setReturnType(str(merged.get("return_type")));
		p.setContact(str(merged.get("contact")));
		p.setMobile(str(merged.get("mobile")));
		p.setAftersalesAddressId(longVal(merged.get("aftersales_address_id")));
		if (merged.containsKey("self_delivery_operator_id")) {
			long sdo = longVal(merged.get("self_delivery_operator_id"));
			p.setSelfDeliveryOperatorId(sdo > 0 ? sdo : null);
		}
		if ("REFUND_GOODS".equalsIgnoreCase(p.getAftersalesType() == null ? "" : p.getAftersalesType().trim())) {
			p.setGoodsReturnedRaw(Boolean.FALSE);
			p.setGoodsReturned(false);
		} else {
			p.setGoodsReturnedRaw(null);
		}
		p.setDetail(merged.get("detail"));
		if (p.getDetail() instanceof List<?>) {
			List<Map<String, Object>> rows = new ArrayList<>();
			for (Object o : (List<?>) p.getDetail()) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					rows.add(row);
				}
			}
			p.setDetailRows(rows);
		}
		p.syncDetailLinesFromRows();
		return p;
	}

	private Map<String, Object> mergeAuth(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private void validateApplyMatrix(LinkedHashMap<String, Object> merged) {
		List<String> segments = new ArrayList<>();
		if (isBlank(merged.get("order_id"))) {
			segments.add("订单号必填,必须为整数");
		}
		Object detailRaw = merged.get("detail");
		if (detailRaw == null) {
			segments.add("售后商品明细必填");
		} else if (detailRaw instanceof String ds && !StringUtils.hasText(ds)) {
			segments.add("售后商品明细必填");
		} else if (detailRaw instanceof List<?> dl && dl.isEmpty()) {
			segments.add("售后商品明细必填");
		}
		if (isBlank(merged.get("company_id"))) {
			segments.add("企业id必填");
		}
		if (isBlank(merged.get("user_id"))) {
			segments.add("会员id必填");
		}
		if (isBlank(merged.get("aftersales_type"))) {
			segments.add("售后类型必选");
		}
		if (isBlank(merged.get("reason"))) {
			segments.add("售后原因必选");
		}
		String rt = str(merged.get("return_type"));
		if ("offline".equalsIgnoreCase(rt)) {
			if (isBlank(merged.get("aftersales_address_id"))) {
				segments.add("请选择退货门店");
			}
			if (isBlank(merged.get("contact"))) {
				segments.add("请填写联系人姓名");
			}
			if (isBlank(merged.get("mobile"))) {
				segments.add("请填写联系人手机号码");
			}
		}
		if (segments.isEmpty()) {
			return;
		}
		throw new ResourceException(trimEdgeFullWidthComma(String.join(String.valueOf(FW_COMMA), segments)));
	}

	private void parseAndAttachDetail(LinkedHashMap<String, Object> merged) {
		Object raw = merged.get("detail");
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				throw new ResourceException("请提交审核售后的商品");
			}
			merged.put("detail", raw);
			return;
		}
		if (!(raw instanceof String s) || !StringUtils.hasText(s)) {
			throw new ResourceException("请提交审核售后的商品");
		}
		JsonNode node;
		try {
			node = objectMapper.readTree(s);
		} catch (Exception e) {
			throw new ResourceException("请提交审核售后的商品");
		}
		if (node == null || !node.isArray() || node.size() == 0) {
			throw new ResourceException("请提交审核售后的商品");
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
		if (rows.isEmpty()) {
			throw new ResourceException("请提交审核售后的商品");
		}
		merged.put("detail", rows);
	}

	private static boolean selfDeliveryOperatorTruthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return true;
	}

	private static long parseLongStrict(Object o, String absentMessage) {
		if (o == null) {
			throw new ResourceException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(absentMessage);
		}
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

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static boolean isBlank(Object o) {
		return o == null || !StringUtils.hasText(String.valueOf(o).trim());
	}

	private static String trimEdgeFullWidthComma(String s) {
		if (s == null) {
			return "";
		}
		String r = s;
		while (r.startsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(1);
		}
		while (r.endsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(0, r.length() - 1);
		}
		return r;
	}

	private static boolean truthyOfflineAftersales(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return true;
		}
		try {
			return Integer.parseInt(s) != 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
