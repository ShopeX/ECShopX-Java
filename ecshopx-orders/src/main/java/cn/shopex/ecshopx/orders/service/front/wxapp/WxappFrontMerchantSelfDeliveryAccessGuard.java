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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.WxappMemberSelfDeliveryStaffListPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * C 端自配送写操作门禁：仅允许 {@code receipt_type=merchant} 订单，且会员手机号对应的
 * {@code self_delivery_staff} 运营商户店覆盖该订单 {@code distributor_id}。
 */
@Service
public class WxappFrontMerchantSelfDeliveryAccessGuard {

	private static final String OPERATOR_TYPE_SELF_DELIVERY = "self_delivery_staff";

	private final MemberAccountService memberAccountService;
	private final WxappMemberSelfDeliveryStaffListPort selfDeliveryStaffListPort;
	private final OperatorsMapper operatorsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final ObjectMapper objectMapper;

	public WxappFrontMerchantSelfDeliveryAccessGuard(
			MemberAccountService memberAccountService,
			WxappMemberSelfDeliveryStaffListPort selfDeliveryStaffListPort,
			OperatorsMapper operatorsMapper,
			NormalOrdersMapper normalOrdersMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			ObjectMapper objectMapper) {
		this.memberAccountService = memberAccountService;
		this.selfDeliveryStaffListPort = selfDeliveryStaffListPort;
		this.operatorsMapper = operatorsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.objectMapper = objectMapper;
	}

	public void assertAuthorizedForOrder(long companyId, long userId, long orderId) {
		NormalOrders order = loadOrder(companyId, orderId);
		assertMerchantReceipt(order);
		assertSelfDeliveryStaffOwnsDistributor(companyId, userId, nullToZero(order.getDistributorId()));
	}

	public void assertAuthorizedForDeliveryId(long companyId, long userId, String ordersDeliveryId) {
		String pathId = ordersDeliveryId == null ? "" : ordersDeliveryId.trim();
		if (pathId.isEmpty()) {
			throw new ResourceException("发货单不存在");
		}
		OrdersDelivery delivery =
				ordersDeliveryMapper.selectOne(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getOrdersDeliveryId, pathId)
								.eq(OrdersDelivery::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (delivery == null || delivery.getOrderId() == null || delivery.getOrderId() <= 0L) {
			throw new ResourceException("发货单不存在");
		}
		assertAuthorizedForOrder(companyId, userId, delivery.getOrderId());
	}

	private NormalOrders loadOrder(long companyId, long orderId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("此订单不存在！");
		}
		return order;
	}

	private static void assertMerchantReceipt(NormalOrders order) {
		if (!"merchant".equalsIgnoreCase(trim(order.getReceiptType()))) {
			throw new ForbiddenException("无权操作该单据");
		}
	}

	private void assertSelfDeliveryStaffOwnsDistributor(long companyId, long userId, long orderDistributorId) {
		if (userId <= 0L) {
			throw new ForbiddenException("无权操作该单据");
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		String mobile = member == null ? "" : String.valueOf(member.getOrDefault("mobile", "")).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new ForbiddenException("无权操作该单据");
		}
		List<Map<String, Object>> staffRows = selfDeliveryStaffListPort.listByPlainMobile(companyId, mobile);
		if (staffRows == null || staffRows.isEmpty()) {
			throw new ForbiddenException("无权操作该单据");
		}
		List<Long> operatorIds = new ArrayList<>();
		for (Map<String, Object> row : staffRows) {
			long oid = longVal(row == null ? null : row.get("operator_id"));
			if (oid > 0L) {
				operatorIds.add(oid);
			}
		}
		if (operatorIds.isEmpty()) {
			throw new ForbiddenException("无权操作该单据");
		}
		List<Operators> operators =
				operatorsMapper.selectList(
						new LambdaQueryWrapper<Operators>()
								.eq(Operators::getCompanyId, companyId)
								.eq(Operators::getOperatorType, OPERATOR_TYPE_SELF_DELIVERY)
								.in(Operators::getOperatorId, operatorIds));
		Set<Long> allowed = new LinkedHashSet<>();
		if (operators != null) {
			for (Operators op : operators) {
				collectDistributorIds(op == null ? null : op.getDistributorIds(), allowed);
			}
		}
		if (orderDistributorId <= 0L || !allowed.contains(orderDistributorId)) {
			throw new ForbiddenException("无权操作该单据");
		}
	}

	private void collectDistributorIds(String json, Set<Long> out) {
		if (!StringUtils.hasText(json)) {
			return;
		}
		try {
			JsonNode arr = objectMapper.readTree(json);
			if (arr == null || !arr.isArray()) {
				return;
			}
			for (JsonNode el : arr) {
				if (el == null || !el.isObject()) {
					continue;
				}
				JsonNode idNode = el.get("distributor_id");
				if (idNode == null || idNode.isNull()) {
					continue;
				}
				long v = 0L;
				if (idNode.isNumber()) {
					v = idNode.longValue();
				} else if (idNode.isTextual() && StringUtils.hasText(idNode.asText())) {
					try {
						v = Long.parseLong(idNode.asText().trim());
					} catch (NumberFormatException ignored) {
						v = 0L;
					}
				}
				if (v > 0L) {
					out.add(v);
				}
			}
		} catch (Exception ignored) {
			// malformed distributor_ids JSON → no shops authorized for this row
		}
	}

	private static long nullToZero(Long v) {
		return v == null ? 0L : v;
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
