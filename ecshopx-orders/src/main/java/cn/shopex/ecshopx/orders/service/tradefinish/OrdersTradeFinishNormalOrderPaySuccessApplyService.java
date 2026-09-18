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

package cn.shopex.ecshopx.orders.service.tradefinish;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdersTradeFinishNormalOrderPaySuccessApplyService {

	private static final Set<String> NORMAL_TRADE_SOURCE_TYPES = Set.of(
			"normal",
			"normal_normal",
			"normal_groups",
			"normal_seckill",
			"normal_community",
			"bargain",
			"normal_shopguide",
			"normal_pointsmall",
			"normal_employee_purchase");

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	public OrdersTradeFinishNormalOrderPaySuccessApplyService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			SupplierOrderMapper supplierOrderMapper,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
	}

	/**
	 * When trade finish indicates a successful mall normal-order payment, transitions {@code NOTPAY} rows to
	 * the trade-success status and returns the async follow-up payload; otherwise empty.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Optional<Map<String, Object>> markPayedIfNeeded(Map<String, Object> tradeFinishPayload) {
		if (tradeFinishPayload == null || tradeFinishPayload.isEmpty()) {
			return Optional.empty();
		}
		if (!"SUCCESS".equals(trim(tradeFinishPayload.get("trade_state")))) {
			return Optional.empty();
		}
		Long companyId = longFrom(tradeFinishPayload.get("company_id"));
		Long orderId = longFrom(tradeFinishPayload.get("order_id"));
		if (companyId == null || companyId <= 0L || orderId == null || orderId <= 0L) {
			return Optional.empty();
		}
		String tradeSource = normalizeSourceType(tradeFinishPayload.get("trade_source_type"));
		if (tradeSource.isEmpty() || !NORMAL_TRADE_SOURCE_TYPES.contains(tradeSource)) {
			return Optional.empty();
		}

		NormalOrders existing =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (existing == null) {
			return Optional.empty();
		}

		String targetOrderStatus = "normal_groups".equals(tradeSource) ? "WAIT_GROUPS_SUCCESS" : "PAYED";
		if (!"WAIT_GROUPS_SUCCESS".equals(targetOrderStatus)
				&& "shopadmin".equals(trim(existing.getOrderClass()))) {
			targetOrderStatus = "DONE";
		}

		int updatedNormal =
				normalOrdersMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getOrderStatus, "NOTPAY")
								.set(NormalOrders::getOrderStatus, targetOrderStatus)
								.set(NormalOrders::getPayStatus, "PAYED"));
		if (updatedNormal <= 0) {
			return Optional.empty();
		}

		orderAssociationsMapper.update(
				null,
				new LambdaUpdateWrapper<OrderAssociations>()
						.eq(OrderAssociations::getCompanyId, companyId)
						.eq(OrderAssociations::getOrderId, orderId)
						.eq(OrderAssociations::getOrderStatus, "NOTPAY")
						.set(OrderAssociations::getOrderStatus, targetOrderStatus));

		if ("PAYED".equals(targetOrderStatus) || "DONE".equals(targetOrderStatus)) {
			supplierOrderMapper.update(
					null,
					new LambdaUpdateWrapper<SupplierOrder>()
							.eq(SupplierOrder::getCompanyId, companyId)
							.eq(SupplierOrder::getOrderId, orderId)
							.in(SupplierOrder::getOrderStatus, "NOTPAY", "PART_PAYMENT")
							.set(SupplierOrder::getOrderStatus, targetOrderStatus)
							.set(SupplierOrder::getPayStatus, "PAYED"));
		}

		existing.setOrderStatus(targetOrderStatus);
		existing.setPayStatus("PAYED");
		maybeAutoCompleteShopadminOrShopscreenZiti(companyId, orderId, existing);

		return Optional.of(buildPaySuccessPayload(tradeFinishPayload, companyId, orderId));
	}

	/** 店务代客 / 大屏自提单支付成功后自动完成自提与收货。 */
	private void maybeAutoCompleteShopadminOrShopscreenZiti(
			long companyId, long orderId, NormalOrders order) {
		boolean isShopscreen = Boolean.TRUE.equals(order.getIsShopscreen());
		boolean isShopadmin = "shopadmin".equals(trim(order.getOrderClass()));
		if (!isShopscreen && !isShopadmin) {
			return;
		}
		if (!"PAYED".equals(trim(order.getPayStatus()))) {
			return;
		}
		if (!"ziti".equals(trim(order.getReceiptType()))) {
			return;
		}

		boolean isLogistics = Boolean.TRUE.equals(order.getIsLogistics());
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		int aftersalesDays = intVal(orderValiditySettingRedisReadService.readPlatformSetting(companyId)
				.get("latest_aftersale_time"));
		int autoCloseAftersalesTime = nowSec + aftersalesDays * 86400;

		boolean partialDelivery = markNonLogisticsItemsDelivered(companyId, orderId, autoCloseAftersalesTime);

		LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getZitiStatus, "DONE")
				.set(NormalOrders::getUpdateTime, nowSec);
		if (!isLogistics) {
			int leftAftersalesNum = sumItemNums(companyId, orderId);
			uw.set(NormalOrders::getOrderStatus, "DONE")
					.set(NormalOrders::getDeliveryStatus, "DONE")
					.set(NormalOrders::getDeliveryTime, nowSec)
					.set(NormalOrders::getEndTime, (long) nowSec)
					.set(NormalOrders::getOrderAutoCloseAftersalesTime, autoCloseAftersalesTime)
					.set(NormalOrders::getLeftAftersalesNum, leftAftersalesNum);
		} else if (partialDelivery) {
			uw.set(NormalOrders::getDeliveryStatus, "PARTAIL");
		}
		normalOrdersMapper.update(null, uw);
	}

	private boolean markNonLogisticsItemsDelivered(long companyId, long orderId, int autoCloseAftersalesTime) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.and(w -> w.eq(NormalOrdersItems::getIsLogistics, false)
										.or()
										.isNull(NormalOrdersItems::getIsLogistics)));
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (NormalOrdersItems item : items) {
			if (item == null || item.getId() == null) {
				continue;
			}
			int num = item.getNum() == null ? 0 : item.getNum();
			normalOrdersItemsMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrdersItems>()
							.eq(NormalOrdersItems::getId, item.getId())
							.set(NormalOrdersItems::getDeliveryStatus, "DONE")
							.set(NormalOrdersItems::getDeliveryItemNum, num)
							.set(NormalOrdersItems::getAutoCloseAftersalesTime, autoCloseAftersalesTime));
		}
		return true;
	}

	private int sumItemNums(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		int sum = 0;
		if (items == null) {
			return 0;
		}
		for (NormalOrdersItems item : items) {
			if (item == null || item.getNum() == null) {
				continue;
			}
			sum += item.getNum();
		}
		return sum;
	}

	private static Map<String, Object> buildPaySuccessPayload(
			Map<String, Object> tradeFinishPayload, long companyId, long orderId) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", companyId);
		out.put("order_id", orderId);
		copyIfPresent(tradeFinishPayload, out, "user_id");
		copyIfPresent(tradeFinishPayload, out, "pay_type");
		copyIfPresent(tradeFinishPayload, out, "trade_source_type");
		copyIfPresent(tradeFinishPayload, out, "distributor_id");
		copyIfPresent(tradeFinishPayload, out, "merchant_id");
		copyIfPresent(tradeFinishPayload, out, "transaction_id");
		copyIfPresent(tradeFinishPayload, out, "trade_id");
		copyIfPresent(tradeFinishPayload, out, "total_fee");
		copyIfPresent(tradeFinishPayload, out, "pay_fee");
		copyIfPresent(tradeFinishPayload, out, "discount_fee");
		return out;
	}

	private static void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String key) {
		if (from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}

	private static String normalizeSourceType(Object raw) {
		String s = trim(raw);
		return s.isEmpty() ? "" : s.toLowerCase(Locale.ROOT);
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static int intVal(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
