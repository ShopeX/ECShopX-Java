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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptAfterCommitService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappNormalOrderConfirmReceiptService {

	private static final List<String> AFTERSALES_BLOCKING =
			List.of("WAIT_SELLER_AGREE", "WAIT_BUYER_RETURN_GOODS", "WAIT_SELLER_CONFIRM_GOODS");

	private static <T> T firstRowOrNull(List<T> rows) {
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersServiceOrderDataAssembler assembler;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final NormalOrderConfirmReceiptTransactionService transactionService;
	private final NormalOrderConfirmReceiptAfterCommitService afterCommitService;

	public WxappNormalOrderConfirmReceiptService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersServiceOrderDataAssembler assembler,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			NormalOrderConfirmReceiptTransactionService transactionService,
			NormalOrderConfirmReceiptAfterCommitService afterCommitService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.assembler = assembler;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.transactionService = transactionService;
		this.afterCommitService = afterCommitService;
	}

	public Map<String, Object> confirmReceipt(
			HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		RequireOrderIdParsed parsed = requireOrderIdRawForConfirmReceipt(merged);
		String trimmed = parsed.trimmed();
		long orderIdNum = parsed.orderIdNum();

		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		OrderAssociations assoc =
				firstRowOrNull(
						orderAssociationsMapper.selectList(
								new LambdaQueryWrapper<OrderAssociations>()
										.eq(OrderAssociations::getCompanyId, companyId)
										.eq(OrderAssociations::getOrderId, orderIdNum)
										.last("LIMIT 1")));
		if (assoc == null) {
			throw new ResourceException("订单号为" + trimmed + "的订单不存在");
		}

		if (!userIdsLooselyEqual(assoc.getUserId(), auth.get("user_id"))) {
			throw new ForbiddenException("订单数据异常");
		}

		String ot = assoc.getOrderType() == null ? "" : assoc.getOrderType().trim();
		if (ot.isEmpty() || !"normal".equalsIgnoreCase(ot)) {
			throw new ResourceException("仅实体单可确认");
		}

		NormalOrders order =
				firstRowOrNull(
						normalOrdersMapper.selectList(
								new LambdaQueryWrapper<NormalOrders>()
										.eq(NormalOrders::getCompanyId, companyId)
										.eq(NormalOrders::getOrderId, orderIdNum)
										.last("LIMIT 1")));
		if (order == null) {
			throw new ResourceException("订单号为" + trimmed + "的订单不存在");
		}

		Map<String, Object> orderInfo = assembler.toServiceOrderData(order);
		String orderStatus = stringVal(orderInfo.get("order_status"));
		if (!"WAIT_BUYER_CONFIRM".equals(orderStatus)) {
			throw new ResourceException("没有需要完成的订单!");
		}

		String deliveryStatus = stringVal(orderInfo.get("delivery_status"));
		if (!"DONE".equals(deliveryStatus) && !"PARTAIL".equals(deliveryStatus)) {
			throw new ResourceException("未发货订单不可确认收货");
		}

		String cancelStatus = stringVal(orderInfo.get("cancel_status"));
		if (!"FAILS".equals(cancelStatus) && !"NO_APPLY_CANCEL".equals(cancelStatus)) {
			throw new ResourceException("已取消订单不能确认收货!");
		}

		LambdaQueryWrapper<NormalOrdersItems> itemsQw =
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderIdNum)
						.in(NormalOrdersItems::getAftersalesStatus, AFTERSALES_BLOCKING);
		if (order.getUserId() != null) {
			itemsQw.eq(NormalOrdersItems::getUserId, order.getUserId());
		}
		Long aftersalesCnt = normalOrdersItemsMapper.selectCount(itemsQw);
		if (aftersalesCnt != null && aftersalesCnt > 0) {
			throw new ResourceException("售后中的订单不能确认收货!");
		}

		Map<String, Object> validity = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		int days = intVal(validity.get("latest_aftersale_time"));
		long autoClose = Instant.now().getEpochSecond() + days * 86400L;
		int nowSec = (int) Instant.now().getEpochSecond();

		Integer bonusPoints = order.getBonusPoints();

		NormalOrders fresh =
				transactionService.applyConfirmReceiptInTransaction(
						companyId, orderIdNum, trimmed, order, autoClose, nowSec);

		long memberUserId = longVal(auth.get("user_id"));
		afterCommitService.run(
				companyId, orderIdNum, fresh, nowSec, autoClose, "user", memberUserId, bonusPoints);

		Map<String, Object> out = new LinkedHashMap<>(assembler.toServiceOrderData(fresh));
		out.put("order_id", trimmed);
		out.put("company_id", companyId);
		return out;
	}

	private record RequireOrderIdParsed(String trimmed, long orderIdNum) {}

	private static RequireOrderIdParsed requireOrderIdRawForConfirmReceipt(Map<String, Object> merged) {
		Object raw = merged.get("order_id");
		if (raw == null) {
			throw new ResourceException("订单号必填");
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			throw new ResourceException("订单号必填");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号格式错误");
		}
		return new RequireOrderIdParsed(s, orderIdNum);
	}

	private static boolean userIdsLooselyEqual(Object orderUserId, Object authUserId) {
		return normalizeUserIdString(orderUserId).equals(normalizeUserIdString(authUserId));
	}

	private static String normalizeUserIdString(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return "";
		}
		try {
			return String.valueOf(Long.parseLong(s));
		} catch (NumberFormatException e) {
			return s;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			String s = v.toString().trim();
			if (s.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
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
