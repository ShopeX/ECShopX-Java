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

package cn.shopex.ecshopx.datacube.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.datacube.service.GoodsDataService;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoodsStatisticJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(GoodsStatisticJobHandler.class);

	private final GoodsDataService goodsDataService;

	public GoodsStatisticJobHandler(GoodsDataService goodsDataService) {
		this.goodsDataService = goodsDataService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			LocalDate countDate = parseCountDate(payload.get("count_date"));
			List<GoodsDailyStatLineKey> lineKeys = parseLines(payload.get("lines"));
			GoodsStatisticsBatch batch = new GoodsStatisticsBatch(lineKeys);
			String orderClass = stringValue(payload.get("order_class"));
			long actId = longValue(payload.get("act_id"));
			if ("employee_purchase".equalsIgnoreCase(orderClass.trim()) && actId > 0) {
				goodsDataService.runStatistics(batch, countDate, "employee_purchase", actId);
			} else {
				goodsDataService.runStatistics(batch, countDate);
			}
		} catch (Exception e) {
			log.debug("队列统计: 执行商品日统计时失败", e);
		}
	}

	private static LocalDate parseCountDate(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof LocalDate d) {
			return d;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return LocalDate.parse(s);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<GoodsDailyStatLineKey> parseLines(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<GoodsDailyStatLineKey> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Long orderId = longFromMap(m, "order_id");
			Long lineId = longFromMap(m, "line_id");
			out.add(new GoodsDailyStatLineKey(orderId, lineId));
		}
		return out;
	}

	private static Long longFromMap(Map<?, ?> m, String key) {
		Object v = m.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringValue(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static long longValue(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}
}
