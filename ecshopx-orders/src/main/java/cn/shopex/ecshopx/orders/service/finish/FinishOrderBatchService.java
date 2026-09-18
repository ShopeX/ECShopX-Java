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

package cn.shopex.ecshopx.orders.service.finish;

import cn.shopex.ecshopx.common.cron.port.TurntablePayGetTimesOnOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.normal.OrderInvoiceEndTimeOnOrderFinishService;
import cn.shopex.ecshopx.orders.service.normal.OrderProfitPlanCloseTimeWriteService;
import cn.shopex.ecshopx.orders.service.normal.OrdersRelChinaumspayDivisionWriteService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Paginated auto-confirm-receipt batch: candidate paging and per-order side-effect ordering match the
 * FinishOrderJob handler batch semantics used on the slow queue worker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinishOrderBatchService {

	private static final int PAGE_SIZE = 20;
	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final List<String> AFTERSALES_BLOCKING =
			List.of("WAIT_SELLER_AGREE", "WAIT_BUYER_RETURN_GOODS", "WAIT_SELLER_CONFIRM_GOODS");

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final NormalOrderConfirmReceiptTransactionService normalOrderConfirmReceiptTransactionService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;
	private final OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;
	private final TurntablePayGetTimesOnOrderPort turntablePayGetTimesOnOrderPort;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final OrderProfitPlanCloseTimeWriteService orderProfitPlanCloseTimeWriteService;
	private final OrderInvoiceEndTimeOnOrderFinishService orderInvoiceEndTimeOnOrderFinishService;

	public int runEquivalentToFinishOrderJobHandle() {
		long finishBeforeSec = System.currentTimeMillis() / 1000L + 60L;
		long totalCount = normalOrdersMapper.countAutoFinishCandidates(finishBeforeSec);
		int totalPage = (int) Math.ceil((double) totalCount / (double) PAGE_SIZE);

		String datePrefix = LocalDate.now(APP_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE) + ":";
		StringBuilder successMessage = new StringBuilder(datePrefix);
		StringBuilder failMessage = new StringBuilder(datePrefix);

		int successOrders = 0;
		for (int i = 0; i < totalPage; i++) {
			long offset = (long) i * PAGE_SIZE;
			List<NormalOrders> page =
					normalOrdersMapper.selectAutoFinishCandidatePage(finishBeforeSec, offset, PAGE_SIZE);
			if (page == null || page.isEmpty()) {
				continue;
			}
			List<Long> orderIds = new ArrayList<>();
			for (NormalOrders row : page) {
				if (row.getOrderId() != null && row.getOrderId() > 0L) {
					orderIds.add(row.getOrderId());
				}
			}
			Map<Long, List<NormalOrdersItems>> orderItemsList = loadOrderItemsMap(orderIds);
			for (NormalOrders order : page) {
				int n = processOneOrder(order, orderItemsList, successMessage, failMessage);
				successOrders += n;
			}
		}

		log.debug("成功执行自动确认收货{}", successMessage);
		log.debug("未执行自动确认收货{}", failMessage);
		return successOrders;
	}

	private Map<Long, List<NormalOrdersItems>> loadOrderItemsMap(List<Long> orderIds) {
		Map<Long, List<NormalOrdersItems>> map = new HashMap<>();
		if (orderIds == null || orderIds.isEmpty()) {
			return map;
		}
		List<NormalOrdersItems> rows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>().in(NormalOrdersItems::getOrderId, orderIds));
		if (rows == null) {
			return map;
		}
		for (NormalOrdersItems line : rows) {
			if (line == null || line.getOrderId() == null) {
				continue;
			}
			map.computeIfAbsent(line.getOrderId(), k -> new ArrayList<>()).add(line);
		}
		return map;
	}

	/**
	 * @return 本单是否会计入成功数（走通主路径且未抛错）
	 */
	private int processOneOrder(
			NormalOrders order,
			Map<Long, List<NormalOrdersItems>> orderItemsList,
			StringBuilder successMessage,
			StringBuilder failMessage) {
		long oid = order.getOrderId() == null ? 0L : order.getOrderId();
		if (!"WAIT_BUYER_CONFIRM".equals(safe(order.getOrderStatus()))) {
			failMessage.append(oid).append("未发货；");
			log.debug("autoFinish skip orderId={} reason=order_status", oid);
			return 0;
		}
		if (!"DONE".equals(safe(order.getDeliveryStatus()))) {
			failMessage.append(oid).append("未发货；");
			log.debug("autoFinish skip orderId={} reason=delivery_status", oid);
			return 0;
		}
		String cs = safe(order.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equals(cs) && !"FAILS".equals(cs)) {
			failMessage.append(oid).append("已申请取消");
			log.debug("autoFinish skip orderId={} reason=cancel_status", oid);
			return 0;
		}
		List<NormalOrdersItems> lines = orderItemsList.getOrDefault(oid, List.of());
		for (NormalOrdersItems line : lines) {
			String st = safe(line.getAftersalesStatus());
			if (AFTERSALES_BLOCKING.contains(st)) {
				failMessage.append(oid).append("售后未处理");
				log.debug("autoFinish skip orderId={} reason=aftersales", oid);
				return 0;
			}
		}
		long companyId = order.getCompanyId() == null ? 0L : order.getCompanyId();
		Map<String, Object> validity = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		int days = intVal(validity.get("latest_aftersale_time"));
		long nowSec = System.currentTimeMillis() / 1000L;
		long autoClose = nowSec + days * 86400L;
		int nowSecInt = (int) nowSec;

		String orderIdStr = String.valueOf(oid);
		NormalOrders fresh =
				normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
						companyId, oid, orderIdStr, order, autoClose, nowSecInt);

		LinkedHashMap<String, Object> processLog = new LinkedHashMap<>();
		processLog.put("order_id", oid);
		processLog.put("company_id", companyId);
		processLog.put("operator_type", "system");
		processLog.put("operator_id", 0L);
		processLog.put("remarks", "订单完成");
		processLog.put("detail", "订单单号：" + oid + "，订单自动完成");
		orderProcessLogPublishPort.publish(processLog);

		normalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, oid, fresh);
		orderInvoiceEndTimeOnOrderFinishService.updateInvoiceEndTime(companyId, oid, nowSecInt, (int) autoClose);

		if ("chinaums".equals(safe(fresh.getPayType()))
				&& fresh.getDistributorId() != null
				&& fresh.getDistributorId() > 0) {
			ordersRelChinaumspayDivisionWriteService.addRelChinaumsPayDivision(companyId, fresh);
		}

		long uid = fresh.getUserId() == null ? 0L : fresh.getUserId();
		int totalFeeFen = parseTotalFeeFen(fresh.getTotalFee());
		turntablePayGetTimesOnOrderPort.payGetTimes(uid, companyId, totalFeeFen);

		Integer bonus = fresh.getBonusPoints();
		if (bonus != null && bonus > 0 && uid > 0L) {
			pointMemberAddPointService.addPointForNormalOrderBonus(uid, companyId, bonus, oid);
		}

		orderProfitPlanCloseTimeWriteService.orderProfitPlanCloseTime(companyId, oid);
		successMessage.append(oid).append(", ");
		return 1;
	}

	private static int parseTotalFeeFen(String totalFee) {
		if (!StringUtils.hasText(totalFee)) {
			return 0;
		}
		try {
			return Integer.parseInt(totalFee.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
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
}
