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

package cn.shopex.ecshopx.community.service.chief;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CommunityBuyerWriteoffService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public CommunityBuyerWriteoffService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	public Map<String, Object> executeBuyerWriteoff(long companyId, long memberUserId, long orderId) {
		if (orderId <= 0L) {
			throw new ResourceException("订单不存在");
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}
		if (!Objects.equals(order.getUserId(), memberUserId)) {
			throw new ResourceException("只能核销自己跟团的订单");
		}
		if ("DONE".equals(safeTrim(order.getZitiStatus())) && "DONE".equals(safeTrim(order.getOrderStatus()))) {
			throw new ResourceException("该订单已完成自提，请重新确认");
		}
		if ("WAIT_PROCESS".equals(safeTrim(order.getCancelStatus()))) {
			throw new ResourceException("订单有未处理的取消申请，不能核销");
		}
		normalOrderZitiWriteoffService.orderZitiWriteoffForBuyerOperator(companyId, orderId, memberUserId);
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

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}
}
