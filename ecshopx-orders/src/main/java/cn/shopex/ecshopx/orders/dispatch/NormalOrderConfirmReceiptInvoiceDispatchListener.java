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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.OrderInvoiceEndTimeOnOrderFinishService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NormalOrderConfirmReceiptInvoiceDispatchListener implements DispatchListener {

	private static final String LOG_TAG = "[OrderFinishInvoiceListener]";

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderInvoiceEndTimeOnOrderFinishService orderInvoiceEndTimeOnOrderFinishService;

	public NormalOrderConfirmReceiptInvoiceDispatchListener(
			NormalOrdersMapper normalOrdersMapper,
			OrderInvoiceEndTimeOnOrderFinishService orderInvoiceEndTimeOnOrderFinishService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderInvoiceEndTimeOnOrderFinishService = orderInvoiceEndTimeOnOrderFinishService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		try {
			if (payload == null || payload.isEmpty()) {
				return;
			}
			Long companyId = parseLong(payload.get("company_id"));
			Long orderId = parseLong(payload.get("order_id"));
			if (companyId == null || companyId <= 0L || orderId == null || orderId <= 0L) {
				return;
			}

			List<NormalOrders> rows =
					normalOrdersMapper.selectList(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (rows == null || rows.isEmpty()) {
				log.warn("{} missing order companyId={} orderId={}", LOG_TAG, companyId, orderId);
				return;
			}
			NormalOrders row = rows.get(0);
			String orderStatus = row.getOrderStatus();
			if (orderStatus == null || !"done".equals(orderStatus.trim().toLowerCase(Locale.ROOT))) {
				log.warn("{} order not done companyId={} orderId={} status={}", LOG_TAG, companyId, orderId, orderStatus);
				return;
			}

			int endTimeSec = epochSecondsToInt(row.getEndTime());
			int closeAftersalesTimeSec =
					row.getOrderAutoCloseAftersalesTime() != null ? row.getOrderAutoCloseAftersalesTime() : 0;
			orderInvoiceEndTimeOnOrderFinishService.updateInvoiceEndTime(
					companyId, orderId, endTimeSec, closeAftersalesTimeSec);
		} catch (RuntimeException e) {
			log.error("{} handler failed", LOG_TAG, e);
		}
	}

	private static Long parseLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Long l) {
			return l;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int epochSecondsToInt(Long epochSeconds) {
		if (epochSeconds == null) {
			return 0;
		}
		long v = epochSeconds;
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
