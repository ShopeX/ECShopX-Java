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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderRefundCompleteJobBusinessPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptAfterCommitService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns with legacy refund-complete queue worker: gated auto confirm-receipt and coupon restore on
 * delivery totals and remaining after-sales applicability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalOrderRefundCompleteJobBusinessService implements OrderRefundCompleteJobBusinessPort {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final AftersalesDetailAggregateService aftersalesDetailAggregateService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final NormalOrderConfirmReceiptTransactionService normalOrderConfirmReceiptTransactionService;
	private final NormalOrderConfirmReceiptAfterCommitService normalOrderConfirmReceiptAfterCommitService;
	private final OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final ObjectMapper objectMapper;

	private static <T> T firstRowOrNull(List<T> rows) {
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	@Override
	public void execute(long companyId, long orderId) {
		NormalOrders order =
				firstRowOrNull(
						normalOrdersMapper.selectList(
								new LambdaQueryWrapper<NormalOrders>()
										.eq(NormalOrders::getCompanyId, companyId)
										.eq(NormalOrders::getOrderId, orderId)
										.last("LIMIT 1")));
		if (order == null) {
			log.debug("[orderRefundComplete] order missing companyId={} orderId={}", companyId, orderId);
			return;
		}

		String status = safe(order.getOrderStatus());
		if (!"WAIT_BUYER_CONFIRM".equals(status) && !"DONE".equals(status)) {
			return;
		}

		String orderIdStr = String.valueOf(orderId);
		boolean zitiAdjust =
				"ziti".equalsIgnoreCase(safe(order.getReceiptType()))
						&& ("DONE".equalsIgnoreCase(safe(order.getZitiStatus()))
								|| "NOTZITI".equalsIgnoreCase(safe(order.getZitiStatus())));

		List<NormalOrdersItems> lines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));

		int totalNum = 0;
		int deliveryNum = 0;
		int canApplyAgg = 0;
		int nowSecWall = (int) Instant.now().getEpochSecond();

		for (NormalOrdersItems line : lines) {
			if (line == null || line.getId() == null) {
				continue;
			}
			int num = nz(line.getNum());
			totalNum += num;
			int deliveryItemNum = nz(line.getDeliveryItemNum());
			if (zitiAdjust) {
				deliveryItemNum = num;
			}
			deliveryNum += deliveryItemNum;

			long subId = line.getId();
			int appliedNum =
					aftersalesDetailAggregateService.getAppliedNum(companyId, orderIdStr, subId);
			int cancelItemNum = nz(line.getCancelItemNum());
			int left = deliveryItemNum + cancelItemNum - appliedNum;
			if (left > 0) {
				int autoClose = nz(line.getAutoCloseAftersalesTime());
				if (autoClose <= 0 || autoClose >= nowSecWall) {
					canApplyAgg += left;
				}
			}
		}

		if (canApplyAgg != 0 || deliveryNum != totalNum) {
			return;
		}

		boolean waitBuyerConfirm = "WAIT_BUYER_CONFIRM".equalsIgnoreCase(safe(order.getOrderStatus()));
		if (waitBuyerConfirm) {
			String cancelStatus = safe(order.getCancelStatus());
			if (!"FAILS".equalsIgnoreCase(cancelStatus) && !"NO_APPLY_CANCEL".equalsIgnoreCase(cancelStatus)) {
				return;
			}
			String deliveryStatus = safe(order.getDeliveryStatus());
			if (!"DONE".equalsIgnoreCase(deliveryStatus) && !"PARTAIL".equalsIgnoreCase(deliveryStatus)) {
				return;
			}

			Map<String, Object> validity = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
			int days = intVal(validity.get("latest_aftersale_time"));
			long autoClose = Instant.now().getEpochSecond() + days * 86400L;
			int nowSec = (int) Instant.now().getEpochSecond();
			Integer bonusPoints = order.getBonusPoints();

			try {
				NormalOrders fresh =
						normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
								companyId, orderId, orderIdStr, order, autoClose, nowSec);
				long operatorId =
						fresh.getUserId() == null || fresh.getUserId() <= 0L ? 0L : fresh.getUserId();
				normalOrderConfirmReceiptAfterCommitService.run(
						companyId, orderId, fresh, nowSec, autoClose, "user", operatorId, bonusPoints);
				order = fresh;
			} catch (ResourceException ex) {
				log.debug(
						"[orderRefundComplete] confirmReceipt skipped companyId={} orderId={} msg={}",
						companyId,
						orderId,
						ex.getMessage());
			}
		}

		if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return;
		}

		String discountInfoJson = order.getDiscountInfo();
		if (!StringUtils.hasText(discountInfoJson)) {
			return;
		}
		List<Map<String, Object>> discountList = parseDiscountInfo(discountInfoJson, orderId);
		if (discountList == null || discountList.isEmpty()) {
			return;
		}
		for (Map<String, Object> discountEntry : discountList) {
			Object couponCodes = discountEntry.get("coupon_code");
			if (couponCodes instanceof List<?> codes) {
				for (Object codeObj : codes) {
					String couponCode = codeObj == null ? null : String.valueOf(codeObj).trim();
					if (!StringUtils.hasText(couponCode)) {
						continue;
					}
					try {
						orderCancelUserDiscountRestorePort.callbackUserCard(couponCode, orderId);
					} catch (Exception e) {
						log.debug("[orderRefundComplete] callbackUserCard failed, code={}", couponCode, e);
					}
				}
			}
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
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

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseDiscountInfo(String discountInfoJson, long orderId) {
		try {
			Object parsed = objectMapper.readValue(discountInfoJson, Object.class);
			if (parsed instanceof List<?> list) {
				List<Map<String, Object>> result = new ArrayList<>();
				for (Object item : list) {
					if (item instanceof Map<?, ?> m) {
						result.add((Map<String, Object>) m);
					}
				}
				return result;
			}
			if (parsed instanceof Map<?, ?> m) {
				return List.of((Map<String, Object>) m);
			}
			return List.of();
		} catch (Exception e) {
			log.debug("[orderRefundComplete] parseDiscountInfo failed, orderId={}", orderId, e);
			return null;
		}
	}
}
