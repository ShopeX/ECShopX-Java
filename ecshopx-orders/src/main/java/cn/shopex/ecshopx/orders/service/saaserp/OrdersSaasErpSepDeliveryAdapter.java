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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpSepDeliveryPort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrdersSaasErpSepDeliveryAdapter implements SaasErpSepDeliveryPort {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;

	public OrdersSaasErpSepDeliveryAdapter(
			OrderAssociationsMapper orderAssociationsMapper,
			AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminNormalOrderDeliveryCoreService = adminNormalOrderDeliveryCoreService;
	}

	@Override
	public Object deliver(Map<String, Object> sepParams) {
		long companyId = parseLong(sepParams == null ? null : sepParams.get("company_id"));
		long orderId = parseLong(sepParams == null ? null : sepParams.get("order_id"));
		if (orderId <= 0L) {
			throw new ResourceException("订单号缺少！");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		adminNormalOrderDeliveryCoreService.deliveryNormalPhysical(sepParams, assoc, "normal");
		return Boolean.TRUE;
	}

	private static long parseLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
