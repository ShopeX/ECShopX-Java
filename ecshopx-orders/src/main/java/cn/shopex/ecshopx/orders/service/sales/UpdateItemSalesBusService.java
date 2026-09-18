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

package cn.shopex.ecshopx.orders.service.sales;

import cn.shopex.ecshopx.common.port.goods.TradeFinishItemSalesIncrementPort;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UpdateItemSalesBusService {

	private static final Logger log = LoggerFactory.getLogger(UpdateItemSalesBusService.class);

	private final TradeFinishItemSalesIncrementPort tradeFinishItemSalesIncrementPort;

	public UpdateItemSalesBusService(TradeFinishItemSalesIncrementPort tradeFinishItemSalesIncrementPort) {
		this.tradeFinishItemSalesIncrementPort = tradeFinishItemSalesIncrementPort;
	}

	public void handleTradeFinishRow(Map<String, Object> snakeCaseTradeRow) {
		if (snakeCaseTradeRow == null || snakeCaseTradeRow.isEmpty()) {
			return;
		}
		Long companyId = parseCompanyId(snakeCaseTradeRow.get("company_id"));
		if (companyId == null) {
			log.debug("UpdateItemSales: skip, invalid or missing company_id");
			return;
		}
		Long orderId = parsePositiveOrderId(snakeCaseTradeRow.get("order_id"));
		if (orderId == null) {
			log.debug("UpdateItemSales: skip, invalid or missing order_id");
			return;
		}
		try {
			tradeFinishItemSalesIncrementPort.incrementSalesForNormalOrder(companyId, orderId);
		} catch (Throwable t) {
			log.debug("UpdateItemSales: increment failed: {}", t.toString());
		}
	}

	private static Long parseCompanyId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Returns non-null only when {@code order_id} parses to a positive long; blank or non-positive values are ignored. */
	private static Long parsePositiveOrderId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
