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

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.normal.OrderPickupSmsRedisVerifyService;
import cn.shopex.ecshopx.orders.service.serviceorder.ServiceOrderZitiWriteoffService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderWriteoffService {

	private static final Set<String> SERVICE_TYPES =
			Set.of("service", "service_groups", "groups", "service_seckill");

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;
	private final ServiceOrderZitiWriteoffService serviceOrderZitiWriteoffService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final ServiceOrdersMapper serviceOrdersMapper;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;

	public AdminOrderWriteoffService(
			OrderAssociationsMapper orderAssociationsMapper,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService,
			ServiceOrderZitiWriteoffService serviceOrderZitiWriteoffService,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			ServiceOrdersMapper serviceOrdersMapper,
			AdminNormalOrderDetailService adminNormalOrderDetailService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.orderPickupSmsRedisVerifyService = orderPickupSmsRedisVerifyService;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
		this.serviceOrderZitiWriteoffService = serviceOrderZitiWriteoffService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
	}

	public Map<String, Object> orderWriteoff(
			long companyId, long orderId, long operatorId, String pickupcodeRaw) {
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		Map<String, Object> pickupSetting = pickupcodeSettingRedisService.handle(companyId, null);
		boolean pickupOn = toPickupStatus(pickupSetting.get("pickupcode_status"));
		if ("community".equalsIgnoreCase(trimToEmpty(assoc.getOrderClass()))) {
			pickupOn = false;
		}

		if (pickupOn && missingPickupcodeValue(pickupcodeRaw)) {
			throw new ResourceException("提货码必填!");
		}

		String effectiveType = resolveEffectiveOrderType(assoc);
		if (effectiveType.isEmpty()) {
			throw new ResourceException("无此类型订单！");
		}

		if ("supplier_order".equals(effectiveType) || "membercard".equals(effectiveType)) {
			throw new ResourceException("无此类型订单！");
		}

		String pickupForLog = pickupcodeRaw == null ? "" : pickupcodeRaw.trim();

		if (SERVICE_TYPES.contains(effectiveType)) {
			if (pickupOn) {
				String mobile = resolveServiceOrderMobile(companyId, orderId, assoc);
				if (!StringUtils.hasText(mobile)) {
					throw new ResourceException("提货人手机号不存在");
				}
				orderPickupSmsRedisVerifyService.verifyAndConsumePickupCode(orderId, mobile, pickupcodeRaw);
			}
			return serviceOrderZitiWriteoffService.orderZitiWriteoffForAdmin(
					companyId, orderId, operatorId, pickupOn, pickupForLog);
		}

		if (pickupOn) {
			NormalOrders normal =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (normal == null) {
				throw new ResourceException("此订单不存在！");
			}
			String mobile = trimToEmpty(normal.getMobile());
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("提货人手机号不存在");
			}
			orderPickupSmsRedisVerifyService.verifyAndConsumePickupCode(orderId, mobile, pickupcodeRaw);
		}

		normalOrderZitiWriteoffService.orderZitiWriteoffForAdmin(
				companyId, orderId, operatorId, pickupOn, pickupForLog);

		NormalOrders fresh =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("此订单不存在！");
		}
		return normalOrdersServiceOrderDataAssembler.toServiceOrderData(fresh);
	}

	public Map<String, Object> getOrderWriteoffInfo(long companyId, long orderId) {
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		String effectiveType = resolveEffectiveOrderType(assoc);
		if (effectiveType.isEmpty()) {
			throw new ResourceException("无此类型订单！");
		}
		if ("supplier_order".equals(effectiveType) || "membercard".equals(effectiveType)) {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> pickupSetting = pickupcodeSettingRedisService.handle(companyId, null);
		boolean pickupOn = toPickupStatus(pickupSetting.get("pickupcode_status"));
		if ("community".equalsIgnoreCase(trimToEmpty(assoc.getOrderClass()))) {
			pickupOn = false;
		}

		String orderIdStr;
		Object itemsObj;

		if (SERVICE_TYPES.contains(effectiveType)) {
			ServiceOrders so =
					serviceOrdersMapper.selectOne(
							new LambdaQueryWrapper<ServiceOrders>()
									.eq(ServiceOrders::getCompanyId, companyId)
									.eq(ServiceOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (so == null) {
				throw new ResourceException("此订单不存在！");
			}
			orderIdStr = String.valueOf(orderId);
			itemsObj = null;
		} else {
			Map<String, Object> bundle;
			try {
				bundle =
						adminNormalOrderDetailService.buildOrderBundle(
								companyId, String.valueOf(orderId), false);
			} catch (BadRequestException e) {
				throw new ResourceException("此订单不存在！");
			}
			Object oi = bundle.get("orderInfo");
			if (!(oi instanceof Map<?, ?>)) {
				throw new ResourceException("此订单不存在！");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> orderInfo = (Map<String, Object>) oi;
			Object oid = orderInfo.get("order_id");
			if (oid != null) {
				String t = String.valueOf(oid).trim();
				orderIdStr =
						(!t.isEmpty() && !"null".equals(t)) ? t : String.valueOf(orderId);
			} else {
				orderIdStr = String.valueOf(orderId);
			}
			itemsObj = orderInfo.get("items");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", orderIdStr);
		out.put("items", itemsObj);
		out.put("pickupcode_status", pickupOn);
		return out;
	}

	private String resolveServiceOrderMobile(long companyId, long orderId, OrderAssociations assoc) {
		ServiceOrders so =
				serviceOrdersMapper.selectOne(
						new LambdaQueryWrapper<ServiceOrders>()
								.eq(ServiceOrders::getCompanyId, companyId)
								.eq(ServiceOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		String fromSvc = so == null ? "" : trimToEmpty(so.getMobile());
		if (StringUtils.hasText(fromSvc)) {
			return fromSvc;
		}
		return trimToEmpty(assoc.getMobile());
	}

	private String resolveEffectiveOrderType(OrderAssociations assoc) {
		String ot = trimToEmpty(assoc.getOrderType());
		String oc = trimToEmpty(assoc.getOrderClass());
		if (ot.isEmpty()) {
			return "";
		}
		if (("normal".equals(ot) || "service".equals(ot))
				&& !ot.equals(oc)
				&& !"normal".equals(oc)
				&& !"service".equals(oc)) {
			return (ot + "_" + oc).toLowerCase();
		}
		return ot.toLowerCase();
	}

	private static boolean missingPickupcodeValue(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return true;
		}
		if ("0".equals(t)) {
			return true;
		}
		try {
			return Long.parseLong(t) == 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean toPickupStatus(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
