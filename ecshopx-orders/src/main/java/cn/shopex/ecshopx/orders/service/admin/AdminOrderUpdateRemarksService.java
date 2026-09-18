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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderUpdateRemarksService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public AdminOrderUpdateRemarksService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.orderAssociationAssociationDataAssembler = orderAssociationAssociationDataAssembler;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	public Map<String, Object> updateRemarks(
			long companyId, long operatorId, String pathOrderIdRaw, Map<String, Object> merged) {
		String raw = pathOrderIdRaw == null ? "" : pathOrderIdRaw.trim();
		if (raw.isEmpty()) {
			throw new ResourceException("此订单不存在！");
		}

		String remark = mergedString(merged, "remark");
		if (remark.codePointCount(0, remark.length()) > 150) {
			throw new ResourceException("字数请不要超过150个！");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.apply("CAST(order_id AS CHAR) = {0}", raw)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (!isNormalPhysicalFamily(effective)) {
			throw new ResourceException("当前订单类型不支持修改备注");
		}

		long dbOrderId = assoc.getOrderId();

		boolean isDistribution = parseStrictDistributionFlag(merged.get("is_distribution"));
		if (!isDistribution && remark.isEmpty()) {
			throw new ResourceException("订单备注必填");
		}

		NormalOrders row =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("订单号为" + raw + "的订单不存在");
		}

		LambdaUpdateWrapper<NormalOrders> uw =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, dbOrderId);
		if (isDistribution) {
			uw.set(NormalOrders::getDistributorRemark, remark);
		} else {
			uw.set(NormalOrders::getRemark, remark);
		}
		normalOrdersMapper.update(null, uw);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", dbOrderId);
		log.put("company_id", companyId);
		log.put("operator_type", "admin");
		log.put("operator_id", operatorId);
		log.put("remarks", "订单备注");
		log.put(
				"detail",
				"订单号：" + raw + "，订单" + (isDistribution ? "商家" : "") + "备注修改");
		log.put("params", Map.copyOf(new LinkedHashMap<>(merged)));
		orderProcessLogPublishPort.publish(log);

		OrderAssociations reloaded =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("此订单不存在！");
		}
		return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
	}

	private static String mergedString(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return "";
		}
		Object v = merged.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static boolean parseStrictDistributionFlag(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof Integer i) {
			return i == 1;
		}
		if (v instanceof Long l) {
			return l == 1L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return "true".equalsIgnoreCase(t) || "1".equals(t);
		}
		return false;
	}

	private static boolean isNormalPhysicalFamily(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "normal_groups".equals(effective)
				|| "normal_drug".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_") && !"membercard".equals(effective);
	}
}
