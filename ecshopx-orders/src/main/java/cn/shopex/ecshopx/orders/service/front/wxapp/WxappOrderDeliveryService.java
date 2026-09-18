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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryLogisticsNameService;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationAssociationDataAssembler;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * H5 / wxapp 订单发货：合并请求体与登录态后委托 {@link AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}
 * 执行发货。订单过程日志事件由核心服务在事务提交后单点发布，本类不再次调用过程日志发布端口。
 *
 * <p>English: third-party trade update dispatch for this HTTP path is handled only inside
 * {@link AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical} (after the shipping
 * transaction commits). Wxapp code must not call {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher}
 * or any extra publish; reuse the shared core pipeline with entry-01-style probes.
 *
 * <pre>
 * mvn -pl ecshopx-orders test -Dtest=AdminNormalOrderDeliveryCoreServiceThirdPartyTradeUpdatePublishProbeTest
 * </pre>
 *
 * <p>与 wxapp 发货路径相关的 Bus 投递与异步消费形态，由下列模块内单测覆盖（勿再增加并行的 core 级
 * {@code DispatchFacade} 探针或第二套同类 async consume 用例）：
 *
 * <pre>
 * mvn -pl ecshopx-orders test -Dtest=AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryOrderProcessLogDispatchPublishProbeTest,WxappApiOrderDeliveryOrderProcessLogEventAsyncConsumeTest
 * </pre>
 *
 * <p>普通订单发货事件名见 {@link OrdersDispatchEventNames#EVENT_NORMAL_ORDER_DELIVERY}；wxapp 层不投递 Bus，
 * 仅合并参数后进入核心 {@link AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}，由核心在事务提交后
 * 单点发布。下列单测分别锁定「核心 afterCommit 发布一次」与「wxapp 服务层委托 core 一次」：
 *
 * <pre>
 * mvn -pl ecshopx-orders test -Dtest=AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryNormalOrderDeliveryDispatchPublishProbeTest,WxappOrderDeliveryServiceNormalOrderDeliveryTriggerProbeTest
 * </pre>
 *
 * <p>有数侧 {@code LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_DELIVERY} 出队执行见 {@code ecshopx-youshu} 模块内既有
 * {@code NormalOrderDeliveryYoushuDispatchListenerDispatchConsumerRuntimeTest}，勿为本路径复制第二套 listener
 * 注册或 publisher 实现。
 */
@Service
public class WxappOrderDeliveryService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	public WxappOrderDeliveryService(
			OrderAssociationsMapper orderAssociationsMapper,
			AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService,
			OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminDeliveryLogisticsNameService = adminDeliveryLogisticsNameService;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDeliveryCoreService = adminNormalOrderDeliveryCoreService;
		this.orderAssociationAssociationDataAssembler = orderAssociationAssociationDataAssembler;
	}

	public Map<String, Object> delivery(
			HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		assertOrderIdPresent(merged);
		Map<String, Object> p = new LinkedHashMap<>(merged);
		long companyId = longVal(auth.get("company_id"));
		p.put("company_id", companyId);
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long orderId = parseOrderIdAfterRequired(p.get("order_id"));
		p.put("order_id", orderId);

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}
		adminDeliveryLogisticsNameService.fillLogiName(p);
		p.put("operator_type", "user");
		p.put("operator_id", longVal(auth.get("user_id")));
		p.put("supplier_id", 0);

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if ("supplier_order".equals(effective)) {
			throw new ResourceException("当前订单类型不支持发货");
		}
		if ("service".equals(effective)
				|| (effective != null && effective.startsWith("service_"))
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return null;
		}
		if (!isNormalPhysicalFamily(effective)) {
			return null;
		}

		adminNormalOrderDeliveryCoreService.deliveryNormalPhysical(p, assoc, effective);

		OrderAssociations reloaded =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("此订单不存在！");
		}
		return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
	}

	private static void assertOrderIdPresent(Map<String, Object> merged) {
		if (merged == null) {
			throw new ResourceException("订单号缺少！");
		}
		if (!merged.containsKey("order_id")) {
			throw new ResourceException("订单号缺少！");
		}
		Object raw = merged.get("order_id");
		if (raw == null) {
			throw new ResourceException("订单号缺少！");
		}
		String trimmed = String.valueOf(raw).trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("订单号缺少！");
		}
	}

	private static long parseOrderIdAfterRequired(Object raw) {
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
