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
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderQrWriteoffService {

	private final OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public AdminOrderQrWriteoffService(
			OrderZitiQrCodeRedisService orderZitiQrCodeRedisService,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.orderZitiQrCodeRedisService = orderZitiQrCodeRedisService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	public Map<String, Object> orderWriteoffQR(
			long companyId,
			long operatorId,
			List<Long> shopIds,
			List<Long> distributorIds,
			String code) {
		long orderId = orderZitiQrCodeRedisService.resolveOrderIdOrThrow(code);

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}

		Long sid = order.getShopId();
		if (sid != null && sid > 0L && !shopIds.contains(sid)) {
			throw new ResourceException("请确认是否有店铺核销权限！");
		}
		Long did = order.getDistributorId();
		if (did != null && did > 0L && !distributorIds.contains(did)) {
			throw new ResourceException("请确认是否有店铺核销权限！");
		}

		String prefix = code.length() >= 6 ? code.substring(0, 6) : code;
		long codePrefixNum = LeadingNumberParser.parseAsLong(prefix);
		long orderZiti = order.getZitiCode() == null ? 0L : order.getZitiCode();
		if (codePrefixNum != orderZiti) {
			throw new ResourceException("核销自提订单有误");
		}

		if ("DONE".equals(trim(order.getZitiStatus())) && "DONE".equals(trim(order.getOrderStatus()))) {
			throw new ResourceException("该订单已完成自提，请重新确认");
		}
		if ("WAIT_PROCESS".equals(trim(order.getCancelStatus()))) {
			throw new ResourceException("订单有未处理的取消申请，不能核销");
		}

		normalOrderZitiWriteoffService.orderZitiWriteoffForAdmin(companyId, orderId, operatorId);

		NormalOrders fresh =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("订单不存在");
		}
		return normalOrdersServiceOrderDataAssembler.toServiceOrderData(fresh);
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
