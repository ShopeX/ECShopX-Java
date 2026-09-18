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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.port.goods.TradeFinishItemSalesIncrementPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeFinishItemSalesIncrementPortImpl implements TradeFinishItemSalesIncrementPort {

	private static final Logger log = LoggerFactory.getLogger(TradeFinishItemSalesIncrementPortImpl.class);

	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final ItemsMapper itemsMapper;

	public TradeFinishItemSalesIncrementPortImpl(
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort, ItemsMapper itemsMapper) {
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.itemsMapper = itemsMapper;
	}

	@Override
	public void incrementSalesForNormalOrder(long companyId, long orderId) {
		List<Map<String, Object>> lines = orderNormalOrderItemsReadPort.listItems(companyId, orderId);
		if (lines.isEmpty()) {
			log.debug("TradeFinishItemSales: no order lines companyId={} orderId={}", companyId, orderId);
			return;
		}
		for (Map<String, Object> line : lines) {
			long itemId = parsePositiveLong(line.get("item_id"));
			int num = parsePositiveInt(line.get("num"));
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
			u.eq(Items::getItemId, itemId)
					.eq(Items::getCompanyId, companyId)
					.setSql("sales = IFNULL(sales,0) + " + num);
			int updated = itemsMapper.update(null, u);
			if (updated <= 0) {
				log.debug(
						"TradeFinishItemSales: no items row updated companyId={} orderId={} itemId={}",
						companyId,
						orderId,
						itemId);
			}
		}
	}

	private static long parsePositiveLong(Object raw) {
		Long v = parseLong(raw);
		return v == null ? 0L : v;
	}

	private static Long parseLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePositiveInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return Math.max(v, 0);
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Math.max(Integer.parseInt(s), 0);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
