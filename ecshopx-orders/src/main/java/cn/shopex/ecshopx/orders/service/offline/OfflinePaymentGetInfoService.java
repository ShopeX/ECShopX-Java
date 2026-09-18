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

package cn.shopex.ecshopx.orders.service.offline;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.orders.config.OrderAppPayTypeDescHolder;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.admin.OrderTradeInfoPhpParityMaps;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OfflinePaymentGetInfoService {

	private static final String DEFAULT_APP_PAY_TYPE_DESC = "微信小程序";
	private static final String OFFLINE_PAY_LANG = "zh-CN";

	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final StringRedisTemplate companysRedisTemplate;
	private final OrderAppPayTypeDescHolder orderAppPayTypeDescHolder;
	private final ObjectMapper objectMapper;

	public OfflinePaymentGetInfoService(
			OfflinePaymentMapper offlinePaymentMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			OrderAppPayTypeDescHolder orderAppPayTypeDescHolder,
			ObjectMapper objectMapper) {
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.orderAppPayTypeDescHolder = orderAppPayTypeDescHolder;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInfo(@SuppressWarnings("unused") long companyId, Map<String, Object> queryParams) {
		Object idObj = queryParams.get("id");
		if (idObj == null) {
			throw new BadRequestException("ID缺少！");
		}
		String raw;
		if (idObj instanceof List<?> list && !list.isEmpty()) {
			raw = String.valueOf(list.get(0)).trim();
		} else {
			raw = String.valueOf(idObj).trim();
		}
		if (raw == null || raw.isEmpty()) {
			throw new BadRequestException("ID缺少！");
		}
		long id;
		try {
			id = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("凭证不存在");
		}

		OfflinePayment row = offlinePaymentMapper.selectById(id);
		if (row == null) {
			throw new ResourceException("凭证不存在");
		}

		OrderAssociations assoc = orderAssociationsMapper.selectOne(
				new LambdaQueryWrapper<OrderAssociations>()
						.eq(OrderAssociations::getCompanyId, row.getCompanyId())
						.eq(OrderAssociations::getOrderId, row.getOrderId())
						.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (!supportsOfflinePaymentDetailBundle(effective)) {
			throw new ResourceException("无此类型订单！");
		}

		String orderIdStr = String.valueOf(row.getOrderId());
		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(row.getCompanyId(), orderIdStr, false);
		} catch (ResourceException e) {
			throw new ResourceException("此订单不存在！");
		} catch (BadRequestException e) {
			throw e;
		}

		Object orderInfoObj = bundle.get("orderInfo");
		LinkedHashMap<String, Object> orderInfoMap;
		if (orderInfoObj instanceof Map<?, ?> om) {
			orderInfoMap = deepCopyToMutableMap(om);
		} else {
			orderInfoMap = new LinkedHashMap<>();
		}
		enrichOrderInfoForPhpParity(row.getCompanyId(), orderInfoMap);

		Object tradeInfoObj = bundle.get("tradeInfo");
		Object tradeInfoFinal;
		if (tradeInfoObj == null
				|| (tradeInfoObj instanceof Map<?, ?> tm && tm.isEmpty())) {
			tradeInfoFinal = new ArrayList<Object>();
		} else if (tradeInfoObj instanceof Map<?, ?> tm) {
			LinkedHashMap<String, Object> snakeTrade = deepCopyToMutableMap(tm);
			tradeInfoFinal = OrderTradeInfoPhpParityMaps.tradeSnakeToPhpCamel(snakeTrade, objectMapper);
		} else {
			tradeInfoFinal = new ArrayList<Object>();
		}

		ObjectMapper rowMapper =
				objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		Map<String, Object> snakeRow =
				rowMapper.convertValue(row, new TypeReference<Map<String, Object>>() {});
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(snakeRow);

		String voucherRaw = row.getVoucherPic();
		List<Object> voucherList;
		if (voucherRaw == null || voucherRaw.isEmpty()) {
			voucherList = Collections.emptyList();
		} else {
			try {
				voucherList = objectMapper.readValue(voucherRaw, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				voucherList = Collections.emptyList();
			}
		}
		out.put("voucher_pic", voucherList);
		out.remove("orderInfo");
		out.put("order_info", orderInfoMap);
		out.put("tradeInfo", tradeInfoFinal);

		return out;
	}

	private void enrichOrderInfoForPhpParity(long companyId, Map<String, Object> orderInfo) {
		attachDistributor(companyId, orderInfo);
		applyOfflinePayName(companyId, orderInfo);
		applyAppPayTypeDesc(orderInfo);
		applyNewDeliveryDefaults(orderInfo);
	}

	private void attachDistributor(long companyId, Map<String, Object> orderInfo) {
		long distId = longVal(orderInfo.get("distributor_id"));
		Map<String, Object> distributorInfo = new LinkedHashMap<>();
		if (distId > 0L) {
			distributorInfo.putAll(
					adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(
							companyId, String.valueOf(distId)));
		} else {
			distributorInfo.putAll(
					adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
		}
		Map<String, Object> platform =
				adminOrderDetailDistributionSupportPort.readOrderValidityPlatformSetting(companyId);
		if (platform != null && intVal(platform.get("is_refund_freight")) == 0) {
			distributorInfo.put("is_refund_freight", 0);
		}
		Object rs = distributorInfo.get("review_status");
		if (rs instanceof Boolean b) {
			distributorInfo.put("review_status", b ? 1 : 0);
		}
		orderInfo.put("distributor_name", str(distributorInfo.get("name")));
		orderInfo.put("distributor_info", distributorInfo);
	}

	private void applyOfflinePayName(long companyId, Map<String, Object> orderInfo) {
		if (!"offline_pay".equals(str(orderInfo.get("pay_type")))) {
			return;
		}
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, OFFLINE_PAY_LANG));
		Map<String, Object> setting = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		String nameOverride =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.offlinePayNameLangKey(companyId, OFFLINE_PAY_LANG));
		if (StringUtils.hasText(nameOverride)) {
			setting.put("pay_name", nameOverride);
		}
		Object payName = setting.get("pay_name");
		orderInfo.put("offline_pay_name", payName != null ? String.valueOf(payName) : null);
	}

	private void applyAppPayTypeDesc(Map<String, Object> orderInfo) {
		String appPayType = str(orderInfo.get("app_pay_type"));
		String configured = orderAppPayTypeDescHolder.descForAppPayTypeOrNull(appPayType);
		if (StringUtils.hasText(configured)) {
			orderInfo.put("app_pay_type_desc", configured);
			return;
		}
		String current = str(orderInfo.get("app_pay_type_desc"));
		if (!StringUtils.hasText(current) || "{}".equals(current.trim())) {
			orderInfo.put("app_pay_type_desc", DEFAULT_APP_PAY_TYPE_DESC);
		}
	}

	private static void applyNewDeliveryDefaults(Map<String, Object> orderInfo) {
		if (!"new".equals(str(orderInfo.get("delivery_type")))) {
			return;
		}
		orderInfo.putIfAbsent("orders_delivery_id", "");
		orderInfo.putIfAbsent("delivery_corp_name", "");
		orderInfo.putIfAbsent("is_all_delivery", false);
	}

	private static boolean supportsOfflinePaymentDetailBundle(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "service".equals(effective)
				|| effective.startsWith("service_")
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_");
	}

	private static LinkedHashMap<String, Object> deepCopyToMutableMap(Map<?, ?> src) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			result.put(String.valueOf(e.getKey()), deepCopyValue(e.getValue()));
		}
		return result;
	}

	private static Object deepCopyValue(Object v) {
		if (v instanceof Map<?, ?> m) {
			return deepCopyToMutableMap(m);
		}
		if (v instanceof Collection<?> c) {
			List<Object> nl = new ArrayList<>();
			for (Object x : c) {
				nl.add(deepCopyValue(x));
			}
			return nl;
		}
		return v;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
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
