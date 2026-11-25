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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legacy TradeFinish hook for canceled normal orders after offline/Alipay success. Refund creation is
 * delegated to {@link cn.shopex.ecshopx.orders.service.tradefinish.OrdersTradeFinishReconcileService} (Spec §3.4).
 */
@Service
public class OfflinePayTradeFinishCanceledNormalOrderRefundService {

	private static final Logger log =
			LoggerFactory.getLogger(OfflinePayTradeFinishCanceledNormalOrderRefundService.class);

	private final NormalOrdersMapper normalOrdersMapper;

	public OfflinePayTradeFinishCanceledNormalOrderRefundService(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeIfApplicable(Map<String, Object> tradeFinishRow) {
		if (!isOfflinePayTradeSuccess(tradeFinishRow)) {
			return;
		}
		long companyId = longFromRow(tradeFinishRow.get("company_id"));
		long orderId = longFromRow(tradeFinishRow.get("order_id"));
		if (companyId <= 0 || orderId <= 0) {
			return;
		}
		if (!isCanceledNormalOrderEligible(companyId, orderId)) {
			return;
		}
		String tradeId = safe(String.valueOf(tradeFinishRow.get("trade_id")));
		log.info(
				"trade_finish_canceled_refund_disabled order_id={} company_id={} trade_id={} reason=delegated_to_trade_finish_reconcile",
				orderId,
				companyId,
				tradeId);
	}

	private boolean isOfflinePayTradeSuccess(Map<String, Object> tradeFinishRow) {
		String tradeState = safe(String.valueOf(tradeFinishRow.get("trade_state")));
		if (!"SUCCESS".equals(tradeState)) {
			return false;
		}
		String payType = safe(String.valueOf(tradeFinishRow.get("pay_type")));
		return isPayTypeAllowedForCanceledRefundPath(payType);
	}

	/**
	 * Trade-finish rows allowed into the canceled normal-order refund + refund OPL path (mirrors legacy
	 * offline audit and Alipay notify success semantics). Do not broaden beyond analyzed pay types.
	 */
	private boolean isPayTypeAllowedForCanceledRefundPath(String payType) {
		return "offline_pay".equalsIgnoreCase(payType) || "alipay".equalsIgnoreCase(payType);
	}

	private boolean isCanceledNormalOrderEligible(long companyId, long orderId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			return false;
		}
		String orderClass = order.getOrderClass() == null ? "normal" : safe(order.getOrderClass());
		if (!"normal".equalsIgnoreCase(orderClass)) {
			return false;
		}
		String orderStatus = safe(order.getOrderStatus());
		if (!"CANCEL".equalsIgnoreCase(orderStatus)) {
			return false;
		}
		return !"NOTPAY".equalsIgnoreCase(orderStatus) && !"PART_PAYMENT".equalsIgnoreCase(orderStatus);
	}

	private static long longFromRow(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}