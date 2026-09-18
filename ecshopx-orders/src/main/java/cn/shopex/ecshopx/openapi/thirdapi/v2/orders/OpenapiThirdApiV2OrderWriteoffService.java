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

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffV2FailException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.OrderPickupSmsRedisVerifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderWriteoffService {

	private static final Set<String> SERVICE_TYPES =
			Set.of("service", "service_groups", "groups", "service_seckill");

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	public OpenapiThirdApiV2OrderWriteoffService(
			OrderAssociationsMapper orderAssociationsMapper,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.orderPickupSmsRedisVerifyService = orderPickupSmsRedisVerifyService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
	}

	/**
	 * OpenAPI 2.0 {@code ecx.order.writeoff} orchestration: validates association / pickup rules,
	 * then delegates to {@link NormalOrderZitiWriteoffService#orderZitiWriteoffForOpenapi}.
	 */
	public void executeOpenapiWriteoff(long companyId, String orderIdRaw, String pickupcodeRaw) {
		if (!StringUtils.hasText(trimToEmpty(orderIdRaw))) {
			throw missingParams("请填写订单编号");
		}

		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw orderNotFound();
		}
		if (orderId <= 0L) {
			throw orderNotFound();
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw orderNotFound();
		}

		Map<String, Object> pickupSetting = pickupcodeSettingRedisService.handle(companyId, null);
		boolean pickupOn = toPickupStatus(pickupSetting.get("pickupcode_status"));
		if ("community".equalsIgnoreCase(trimToEmpty(assoc.getOrderClass()))) {
			pickupOn = false;
		}

		if (pickupOn && missingPickupcodeValue(pickupcodeRaw)) {
			throw missingParams("请填写提货码");
		}

		String effectiveType = resolveEffectiveOrderType(assoc);
		if (effectiveType.isEmpty()) {
			throw handleError("无此类型订单！");
		}
		if ("supplier_order".equals(effectiveType) || "membercard".equals(effectiveType)) {
			throw handleError("无此类型订单！");
		}
		if (SERVICE_TYPES.contains(effectiveType)) {
			throw handleError("无此类型订单！");
		}

		NormalOrders normal =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (normal == null) {
			throw orderNotFound();
		}

		validateEntityOpenApiWriteoffPreconditions(normal);

		String pickupForLog = pickupcodeRaw == null ? "" : pickupcodeRaw.trim();

		if (pickupOn) {
			String mobile = trimToEmpty(normal.getMobile());
			if (!StringUtils.hasText(mobile)) {
				throw handleError("未查询到提货人联系手机！");
			}
			try {
				orderPickupSmsRedisVerifyService.verifyAndConsumePickupCode(
						orderId, mobile, pickupcodeRaw);
			} catch (ResourceException e) {
				throw handleError("提货码验证错误！");
			}
		}

		try {
			normalOrderZitiWriteoffService.orderZitiWriteoffForOpenapi(
					companyId, orderId, pickupOn, pickupForLog);
		} catch (OpenapiOrderWriteoffV2FailException e) {
			throw e;
		} catch (ResourceException e) {
			throw handleError(e.getMessage());
		} catch (Exception e) {
			throw handleError(e.getMessage());
		}
	}

	private static void validateEntityOpenApiWriteoffPreconditions(NormalOrders order) {
		if (!"ziti".equalsIgnoreCase(trimToEmpty(order.getReceiptType()))) {
			throw zitiOrderNotFound();
		}
		if (!"PAYED".equalsIgnoreCase(trimToEmpty(order.getOrderStatus()))
				|| !"PENDING".equalsIgnoreCase(trimToEmpty(order.getZitiStatus()))) {
			throw statusNotAllowed();
		}
		if (!"PAYED".equalsIgnoreCase(trimToEmpty(order.getPayStatus()))) {
			throw statusNotAllowed();
		}
		String cs = trimToEmpty(order.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equals(cs) && !"FAILS".equals(cs)) {
			throw statusNotAllowed();
		}
	}

	private static String resolveEffectiveOrderType(OrderAssociations assoc) {
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
		return raw.trim().isEmpty();
	}

	private static boolean toPickupStatus(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	private static OpenapiOrderWriteoffV2FailException missingParams(String message) {
		return new OpenapiOrderWriteoffV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiOrderWriteoffV2FailException orderNotFound() {
		return new OpenapiOrderWriteoffV2FailException(
				OpenapiErrorCode.ORDER_NOT_FOUND, "订单相应的明细不存在");
	}

	private static OpenapiOrderWriteoffV2FailException zitiOrderNotFound() {
		return new OpenapiOrderWriteoffV2FailException(
				OpenapiErrorCode.ORDER_NOT_FOUND, "自提订单相应的明细不存在");
	}

	private static OpenapiOrderWriteoffV2FailException statusNotAllowed() {
		return new OpenapiOrderWriteoffV2FailException(
				OpenapiErrorCode.ORDER_HANDLE_ERROR, "自提订单状态不正确，不能进行操作");
	}

	private static OpenapiOrderWriteoffV2FailException handleError(String message) {
		return new OpenapiOrderWriteoffV2FailException(
				OpenapiErrorCode.ORDER_HANDLE_ERROR, message);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
