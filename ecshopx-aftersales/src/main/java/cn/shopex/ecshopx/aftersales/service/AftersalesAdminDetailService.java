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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.common.port.companys.OperatorInfoReadPort;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderServiceOrderDataReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorRowByOperatorIdsReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesAdminDetailService {

	private static final ObjectMapper EVIDENCE_PIC_MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> ADDRESS_MAP_TYPE = new TypeReference<>() {};

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort;
	private final AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort;
	private final OrderNormalOrderServiceOrderDataReadPort orderNormalOrderServiceOrderDataReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final JushuitanSettingReadPort jushuitanSettingReadPort;
	private final OperatorInfoReadPort operatorInfoReadPort;
	private final SupplierOperatorRowByOperatorIdsReadPort supplierOperatorRowByOperatorIdsReadPort;
	private final AftersalesAdminAppInfoBuilder aftersalesAdminAppInfoBuilder;
	private final AftersalesAdminDetailPhpParityApplier aftersalesAdminDetailPhpParityApplier;

	public AftersalesAdminDetailService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundService aftersalesRefundService,
			DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort,
			AdminOrderDetailSalespersonLookupPort adminOrderDetailSalespersonLookupPort,
			OrderNormalOrderServiceOrderDataReadPort orderNormalOrderServiceOrderDataReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			JushuitanSettingReadPort jushuitanSettingReadPort,
			OperatorInfoReadPort operatorInfoReadPort,
			SupplierOperatorRowByOperatorIdsReadPort supplierOperatorRowByOperatorIdsReadPort,
			AftersalesAdminAppInfoBuilder aftersalesAdminAppInfoBuilder,
			AftersalesAdminDetailPhpParityApplier aftersalesAdminDetailPhpParityApplier) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.distributorDefaultAftersalesAddressReadPort = distributorDefaultAftersalesAddressReadPort;
		this.adminOrderDetailSalespersonLookupPort = adminOrderDetailSalespersonLookupPort;
		this.orderNormalOrderServiceOrderDataReadPort = orderNormalOrderServiceOrderDataReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.jushuitanSettingReadPort = jushuitanSettingReadPort;
		this.operatorInfoReadPort = operatorInfoReadPort;
		this.supplierOperatorRowByOperatorIdsReadPort = supplierOperatorRowByOperatorIdsReadPort;
		this.aftersalesAdminAppInfoBuilder = aftersalesAdminAppInfoBuilder;
		this.aftersalesAdminDetailPhpParityApplier = aftersalesAdminDetailPhpParityApplier;
	}

	public Map<String, Object> loadFullAftersalesForAdmin(long companyId, long aftersalesBn) {
		return getAftersalesDetail(companyId, String.valueOf(aftersalesBn));
	}

	public Map<String, Object> getAftersalesDetail(long companyId, String aftersalesBn) {
		Aftersales a;
		try {
			a =
					aftersalesMapper.selectOne(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.apply("aftersales_bn = {0}", aftersalesBn));
		} catch (TooManyResultsException e) {
			throw new ResourceException("没有售后信息");
		}
		if (a == null) {
			throw new ResourceException("没有售后信息");
		}
		return buildFullDetailFromMainRow(a);
	}

	public Map<String, Object> loadFullAftersalesForMember(
			long companyId, long aftersalesBn, long userIdForScope) {
		Aftersales a;
		try {
			LambdaQueryWrapper<Aftersales> w =
					new LambdaQueryWrapper<Aftersales>()
							.eq(Aftersales::getCompanyId, companyId)
							.apply("aftersales_bn = {0}", String.valueOf(aftersalesBn));
			if (userIdForScope != 0L) {
				w.eq(Aftersales::getUserId, userIdForScope);
			}
			a = aftersalesMapper.selectOne(w);
		} catch (TooManyResultsException e) {
			throw new ResourceException("没有售后信息");
		}
		if (a == null) {
			throw new ResourceException("没有售后信息");
		}
		return buildFullDetailFromMainRow(a);
	}

	public Map<String, Object> loadFullAftersalesForMember(
			long companyId, String aftersalesBnRaw, long userIdForScope) {
		Aftersales a;
		try {
			LambdaQueryWrapper<Aftersales> w =
					new LambdaQueryWrapper<Aftersales>()
							.eq(Aftersales::getCompanyId, companyId)
							.apply("aftersales_bn = {0}", aftersalesBnRaw);
			if (userIdForScope != 0L) {
				w.eq(Aftersales::getUserId, userIdForScope);
			}
			a = aftersalesMapper.selectOne(w);
		} catch (TooManyResultsException e) {
			throw new ResourceException("没有售后信息");
		}
		if (a == null) {
			throw new ResourceException("没有售后信息");
		}
		return buildFullDetailFromMainRow(a);
	}

	private Map<String, Object> buildFullDetailFromMainRow(Aftersales a) {
		long companyId = a.getCompanyId() == null ? 0L : a.getCompanyId();
		long aftersalesBnLong = a.getAftersalesBn();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.putAll(aftersalesToSnakeMap(a));

		String addrField = a.getAftersalesAddress();
		long distributorId = a.getDistributorId() == null ? 0L : a.getDistributorId();
		if (needDefaultAddress(addrField)) {
			Optional<Map<String, Object>> def =
					distributorDefaultAftersalesAddressReadPort.findDefaultAddress(companyId, distributorId);
			if (def.isPresent()) {
				out.put("aftersales_address", def.get());
			} else {
				out.put("aftersales_address", addrField == null ? "" : addrField);
			}
		} else {
			out.put("aftersales_address", parseAftersalesAddressJson(addrField));
		}

		Long salesmanId = a.getSalesmanId();
		if (salesmanId != null && salesmanId != 0L) {
			out.put(
					"salesman",
					adminOrderDetailSalespersonLookupPort.loadSalespersonForOrderDetail(companyId, salesmanId));
		} else {
			out.put("salesman", new ArrayList<>());
		}

		long orderId = a.getOrderId() == null ? 0L : a.getOrderId();
		List<Map<String, Object>> items =
				orderId > 0L
						? orderNormalOrderItemsReadPort.listServiceOrderItems(companyId, orderId)
						: List.of();

		List<AftersalesDetail> detailRows =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBnLong));

		List<Map<String, Object>> detailMaps = new ArrayList<>();
		for (AftersalesDetail d : detailRows) {
			detailMaps.add(aftersalesDetailToApiRow(d));
		}
		if (orderId > 0L) {
			Map<Long, List<Map<String, Object>>> orderItemsByOrderId = new HashMap<>();
			orderItemsByOrderId.put(orderId, items);
			attachDetailForAdmin(companyId, orderId, detailMaps, orderItemsByOrderId);
		}

		AftersalesRefund refund = aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBnLong);
		Map<String, Object> refundInfo =
				refund == null ? new LinkedHashMap<>() : new LinkedHashMap<>(refundToSnakeMap(refund));
		out.put("refund_info", refundInfo);

		if (detailMaps.size() == 1 && refund != null) {
			Map<String, Object> subRi = new LinkedHashMap<>();
			subRi.put("refunded_fee", refund.getRefundedFee());
			subRi.put("refund_point", refund.getRefundedPoint());
			detailMaps.get(0).put("refund_info", subRi);
		}

		out.put("detail", detailMaps);

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		if (orderId > 0L) {
			Optional<Map<String, Object>> headerOpt =
					orderNormalOrderServiceOrderDataReadPort.getServiceOrderData(companyId, orderId);
			if (headerOpt.isPresent()) {
				orderInfo.putAll(headerOpt.get());
			}
		}
		out.put("order_info", orderInfo);

		Map<Long, Optional<Map<String, Object>>> appOrderHeaderCache = new HashMap<>();
		out.put(
				"app_info",
				aftersalesAdminAppInfoBuilder.buildAppInfo(out, orderId, companyId, appOrderHeaderCache, true));

		Integer supId = a.getSupplierId();
		if (supId != null && supId != 0) {
			out.put("supplier_info", operatorInfoReadPort.getInfo(companyId, supId.longValue(), "supplier"));
		}

		Map<String, Object> distInfo =
				(distributorId <= 0L)
						? new LinkedHashMap<>()
						: distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
								companyId, distributorId);
		out.put("distributor_info", distInfo);

		Map<String, Object> jt = jushuitanSettingReadPort.readJushuitanSetting(companyId);
		out.put("is_jushuitan", Boolean.TRUE.equals(jt.get("is_open")));

		aftersalesAdminDetailPhpParityApplier.apply(out);
		return out;
	}

	public Map<String, Object> mapAftersalesMainRow(Aftersales a) {
		return new LinkedHashMap<>(aftersalesToSnakeMap(a));
	}

	private void attachDetailForAdmin(
			long companyId,
			long orderId,
			List<Map<String, Object>> detailMaps,
			Map<Long, List<Map<String, Object>>> orderItemsByOrderId) {
		List<Map<String, Object>> orderitemList = orderItemsByOrderId.getOrDefault(orderId, List.of());
		Map<Long, Map<String, Object>> byItemId = new HashMap<>();
		Set<Long> supplierIds = new LinkedHashSet<>();
		for (Map<String, Object> oi : orderitemList) {
			byItemId.put(longVal(oi.get("item_id")), oi);
			long sup = longVal(oi.get("supplier_id"));
			if (sup > 0L) {
				supplierIds.add(sup);
			}
		}
		Map<Long, Map<String, Object>> supplierRowByOp =
				supplierOperatorRowByOperatorIdsReadPort.mapRowByOperatorIds(companyId, supplierIds);
		for (Map<String, Object> d : detailMaps) {
			long iid = longVal(d.get("item_id"));
			Map<String, Object> oi = byItemId.get(iid);
			if (oi != null) {
				Map<String, Object> oiMap = new LinkedHashMap<>(oi);
				long supOp = longVal(oi.get("supplier_id"));
				Object supplierName =
						supOp > 0L ? supplierRowByOp.getOrDefault(supOp, emptySupplierNameRow(supOp)) : "";
				oiMap.put("supplier_name", supplierName);
				d.put("orderItem", oiMap);
				d.put("supplier_name", supplierName);
			} else {
				d.put("orderItem", null);
				d.put("supplier_name", "");
			}
		}
	}

	private static Map<String, Object> emptySupplierNameRow(long operatorId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", operatorId);
		m.put("supplier_name", "");
		return m;
	}

	private static Map<String, Object> aftersalesDetailToApiRow(AftersalesDetail d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("detail_id", d.getDetailId());
		m.put("company_id", d.getCompanyId());
		m.put("distributor_id", d.getDistributorId());
		m.put("user_id", d.getUserId());
		m.put("aftersales_bn", d.getAftersalesBn());
		m.put("order_id", d.getOrderId());
		m.put("sub_order_id", d.getSubOrderId());
		m.put("goods_id", d.getGoodsId());
		m.put("item_id", d.getItemId());
		m.put("item_bn", d.getItemBn());
		m.put("item_name", d.getItemName());
		m.put("order_item_type", d.getOrderItemType());
		m.put("item_pic", d.getItemPic());
		m.put("num", d.getNum());
		m.put("refund_fee", d.getRefundFee());
		m.put("refund_point", d.getRefundPoint());
		m.put("aftersales_type", d.getAftersalesType());
		m.put("progress", d.getProgress());
		m.put("aftersales_status", d.getAftersalesStatus());
		m.put("create_time", d.getCreateTime());
		m.put("update_time", d.getUpdateTime());
		m.put("auto_refuse_time", d.getAutoRefuseTime());
		m.put("refunded_num", d.getRefundedNum());
		return m;
	}

	private Map<String, Object> aftersalesToSnakeMap(Aftersales a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", a.getAftersalesBn());
		m.put("order_id", a.getOrderId());
		m.put("company_id", a.getCompanyId());
		m.put("user_id", a.getUserId());
		m.put("salesman_id", a.getSalesmanId());
		m.put("item_bn", a.getItemBn());
		m.put("shop_id", a.getShopId());
		m.put("distributor_id", a.getDistributorId());
		m.put("supplier_id", a.getSupplierId());
		m.put("aftersales_type", a.getAftersalesType());
		m.put("aftersales_status", a.getAftersalesStatus());
		m.put("progress", a.getProgress());
		m.put("refund_fee", a.getRefundFee());
		m.put("refund_point", a.getRefundPoint());
		m.put("reason", a.getReason());
		m.put("description", a.getDescription());
		m.put("evidence_pic", evidencePicForApi(a.getEvidencePic()));
		m.put("refuse_reason", a.getRefuseReason());
		m.put("memo", a.getMemo());
		m.put("sendback_data", jsonFieldOrEmptyArray(a.getSendbackData()));
		m.put("sendconfirm_data", jsonFieldOrEmptyArray(a.getSendconfirmData()));
		m.put("third_data", a.getThirdData());
		m.put("aftersales_address", a.getAftersalesAddress());
		m.put("distributor_remark", a.getDistributorRemark());
		m.put("create_time", a.getCreateTime());
		m.put("update_time", a.getUpdateTime());
		m.put("contact", a.getContact());
		m.put("mobile", a.getMobile());
		m.put("merchant_id", a.getMerchantId());
		m.put("is_partial_cancel", a.getIsPartialCancel());
		m.put("return_type", a.getReturnType());
		m.put("return_distributor_id", a.getReturnDistributorId());
		m.put("self_delivery_operator_id", a.getSelfDeliveryOperatorId());
		m.put("freight", a.getFreight());
		m.put("freight_type", a.getFreightType());
		return m;
	}

	private static List<String> evidencePicForApi(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<>();
		}
		String trimmed = raw.trim();
		try {
			JsonNode node = EVIDENCE_PIC_MAPPER.readTree(trimmed);
			if (node.isArray()) {
				List<String> pics = new ArrayList<>();
				for (JsonNode item : node) {
					if (item.isTextual()) {
						pics.add(item.asText());
					} else if (!item.isNull()) {
						pics.add(item.asText());
					}
				}
				return pics;
			}
		} catch (JsonProcessingException ignored) {
			// fall through
		}
		return new ArrayList<>();
	}

	private static Object jsonFieldOrEmptyArray(String raw) {
		Map<String, Object> parsed = parseJsonObject(raw);
		if (parsed != null && !parsed.isEmpty()) {
			return parsed;
		}
		return new ArrayList<>();
	}

	private static Map<String, Object> parseJsonObject(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String trimmed = raw.trim();
		if ("0".equals(trimmed)) {
			return null;
		}
		try {
			return EVIDENCE_PIC_MAPPER.readValue(trimmed, ADDRESS_MAP_TYPE);
		} catch (JsonProcessingException ignored) {
			return null;
		}
	}

	private static Map<String, Object> refundToSnakeMap(AftersalesRefund r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("refund_bn", r.getRefundBn());
		m.put("aftersales_bn", r.getAftersalesBn());
		m.put("order_id", r.getOrderId());
		m.put("trade_id", r.getTradeId());
		m.put("company_id", r.getCompanyId());
		m.put("supplier_id", r.getSupplierId());
		m.put("user_id", r.getUserId());
		m.put("shop_id", r.getShopId());
		m.put("distributor_id", r.getDistributorId());
		m.put("refund_type", r.getRefundType());
		m.put("refund_channel", r.getRefundChannel());
		m.put("refund_status", r.getRefundStatus());
		m.put("refund_fee", r.getRefundFee());
		m.put("refunded_fee", r.getRefundedFee());
		m.put("refund_point", r.getRefundPoint());
		m.put("refunded_point", r.getRefundedPoint());
		m.put("return_point", r.getReturnPoint());
		m.put("return_freight", r.getReturnFreight());
		m.put("pay_type", r.getPayType());
		m.put("currency", r.getCurrency());
		m.put("refunds_memo", r.getRefundsMemo());
		m.put("refund_success_time", r.getRefundSuccessTime());
		m.put("refund_id", r.getRefundId());
		m.put("create_time", r.getCreateTime());
		m.put("update_time", r.getUpdateTime());
		m.put("cur_fee_type", r.getCurFeeType());
		m.put("cur_fee_rate", r.getCurFeeRate());
		m.put("cur_fee_symbol", r.getCurFeeSymbol());
		m.put("cur_pay_fee", r.getCurPayFee());
		m.put("hf_order_id", r.getHfOrderId());
		m.put("merchant_id", r.getMerchantId());
		m.put("freight", r.getFreight());
		m.put("freight_type", r.getFreightType());
		return m;
	}

	private static boolean needDefaultAddress(String addr) {
		if (addr == null) {
			return true;
		}
		if (addr.isEmpty()) {
			return true;
		}
		return "0".equals(addr.trim());
	}

	private static Map<String, Object> parseAftersalesAddressJson(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> parsed = EVIDENCE_PIC_MAPPER.readValue(raw.trim(), ADDRESS_MAP_TYPE);
			return parsed != null ? parsed : new LinkedHashMap<>();
		} catch (JsonProcessingException e) {
			return new LinkedHashMap<>();
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
}
