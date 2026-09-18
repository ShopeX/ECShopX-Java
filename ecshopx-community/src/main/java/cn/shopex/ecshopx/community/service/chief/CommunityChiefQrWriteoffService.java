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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefQrWriteoffService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public CommunityChiefQrWriteoffService(
			NormalOrdersMapper normalOrdersMapper,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			OrderZitiQrCodeRedisService orderZitiQrCodeRedisService,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.orderZitiQrCodeRedisService = orderZitiQrCodeRedisService;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	public Map<String, Object> executeQrWriteoff(long companyId, long chiefId, String code) {
		if (chiefId <= 0L) {
			throw new ForbiddenException("只有团长可以核销订单");
		}
		if (!StringUtils.hasText(code)) {
			throw new BadRequestException("code参数必填");
		}
		long orderId = orderZitiQrCodeRedisService.resolveOrderIdOrThrow(code);

		NormalOrders order = loadOrderOrThrow(companyId, orderId);

		CommunityOrderRelActivity rel =
				communityOrderRelActivityMapper.selectOne(
						new LambdaQueryWrapper<CommunityOrderRelActivity>()
								.eq(CommunityOrderRelActivity::getOrderId, orderId)
								.eq(CommunityOrderRelActivity::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (rel == null) {
			throw new ResourceException("只能核销自己开团的订单");
		}
		if (rel.getChiefId() == null || rel.getChiefId() != chiefId) {
			throw new ResourceException("只能核销自己开团的订单");
		}

		validateZitiCodePrefix(code, order);

		if ("DONE".equals(safeTrim(order.getZitiStatus())) && "DONE".equals(safeTrim(order.getOrderStatus()))) {
			throw new ResourceException("该订单已完成自提，请重新确认");
		}
		if ("WAIT_PROCESS".equals(safeTrim(order.getCancelStatus()))) {
			throw new ResourceException("订单有未处理的取消申请，不能核销");
		}

		normalOrderZitiWriteoffService.orderZitiWriteoffForChief(companyId, orderId, chiefId);

		NormalOrders fresh = loadOrderOrThrow(companyId, orderId);
		return normalOrdersServiceOrderDataAssembler.toServiceOrderData(fresh);
	}

	private void validateZitiCodePrefix(String code, NormalOrders order) {
		String prefix = code.length() >= 6 ? code.substring(0, 6) : code;
		long codePrefixNum = LeadingNumberParser.parseAsLong(prefix);
		long orderZiti = order.getZitiCode() == null ? 0L : order.getZitiCode();
		if (codePrefixNum != orderZiti) {
			throw new ResourceException("核销自提订单有误");
		}
	}

	private NormalOrders loadOrderOrThrow(long companyId, long orderId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}
		return order;
	}

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}
}
