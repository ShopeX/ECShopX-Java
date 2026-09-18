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

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import cn.shopex.ecshopx.aftersales.service.bind.AftersalesOrderBindUserService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;

@Service
public class WxappOrderBindUserOrderService {

	private final AftersalesOrderBindUserService aftersalesOrderBindUserService;

	private final MemberAccountService memberAccountService;

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	private final NormalOrdersMapper normalOrdersMapper;

	private final OrderAssociationsMapper orderAssociationsMapper;

	private final TradeMapper tradeMapper;

	public WxappOrderBindUserOrderService(
			AftersalesOrderBindUserService aftersalesOrderBindUserService,
			MemberAccountService memberAccountService,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersMapper normalOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			TradeMapper tradeMapper) {
		this.aftersalesOrderBindUserService = aftersalesOrderBindUserService;
		this.memberAccountService = memberAccountService;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.tradeMapper = tradeMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void bindUserOrder(long companyId, long jwtUserId, String orderIdRaw, String authCode) {
		String trimmedOrderId = orderIdRaw == null ? "" : orderIdRaw.trim();
		final long orderIdLong;
		try {
			orderIdLong = Long.parseLong(trimmedOrderId);
		} catch (NumberFormatException e) {
			throw new ResourceException("订单不存在或者验证码错误");
		}

		NormalOrders orderRow = normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderIdLong)
						.eq(NormalOrders::getBindAuthCode, authCode)
						.last("LIMIT 1"));
		if (orderRow == null) {
			throw new ResourceException("订单不存在或者验证码错误");
		}

		Long uid = orderRow.getUserId();
		if (uid != null && uid > 0L) {
			throw new ResourceException("该订单已绑定用户");
		}

		Map<String, Object> member = memberAccountService.getMemberInfo(jwtUserId, companyId);
		if (member == null || member.isEmpty()) {
			throw new ResourceException("绑定的用户不存在");
		}
		long boundUserId = longMemberUserId(member);
		if (boundUserId <= 0) {
			throw new ResourceException("绑定的用户不存在");
		}

		Object mo = member.get("mobile");
		String mobile = mo == null ? "" : String.valueOf(mo);

		Trade tradeRow = tradeMapper.selectOne(
				new LambdaQueryWrapper<Trade>()
						.eq(Trade::getCompanyId, String.valueOf(companyId))
						.eq(Trade::getOrderId, String.valueOf(orderIdLong))
						.last("LIMIT 1"));

		NormalOrders patch = new NormalOrders();
		patch.setUserId(boundUserId);
		patch.setMobile(mobile);
		int n0 = normalOrdersMapper.update(
				patch,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderIdLong));
		if (n0 == 0) {
			throw new ResourceException("订单不存在");
		}

		OrderAssociations assocPatch = new OrderAssociations();
		assocPatch.setUserId(boundUserId);
		assocPatch.setMobile(mobile);
		int n1 = orderAssociationsMapper.update(
				assocPatch,
				new LambdaUpdateWrapper<OrderAssociations>()
						.eq(OrderAssociations::getCompanyId, companyId)
						.eq(OrderAssociations::getOrderId, orderIdLong));
		if (n1 == 0) {
			throw new ResourceException("订单关联信息不存在");
		}

		NormalOrdersItems itemPatch = new NormalOrdersItems();
		itemPatch.setUserId(boundUserId);
		int n2 = normalOrdersItemsMapper.update(
				itemPatch,
				new LambdaUpdateWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderIdLong));
		if (n2 == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		if (tradeRow != null) {
			LambdaUpdateWrapper<Trade> tw = new LambdaUpdateWrapper<Trade>()
					.eq(Trade::getCompanyId, String.valueOf(companyId))
					.eq(Trade::getOrderId, String.valueOf(orderIdLong));
			Trade tpatch = new Trade();
			tpatch.setUserId(String.valueOf(boundUserId));
			tpatch.setMobile(mobile);
			int n3 = tradeMapper.update(tpatch, tw);
			if (n3 == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		aftersalesOrderBindUserService.bindUserAftersales(companyId, orderIdLong, boundUserId, mobile);
	}

	private static long longMemberUserId(Map<String, Object> member) {
		Object v = member.get("user_id");
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
