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
import cn.shopex.ecshopx.datacube.service.goodsdata.AdminGoodsDataFilter;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDataCsvExportService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoodsDataJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(GoodsDataJobHandler.class);

	private final GoodsDataCsvExportService goodsDataCsvExportService;

	public GoodsDataJobHandler(GoodsDataCsvExportService goodsDataCsvExportService) {
		this.goodsDataCsvExportService = goodsDataCsvExportService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			goodsDataCsvExportService.runExport(parseFilter(payload));
		} catch (Exception e) {
			log.debug("队列导出: 执行导出时失败", e);
		}
	}

	private static AdminGoodsDataFilter parseFilter(Map<String, Object> payload) {
		long companyId = longValue(payload.get("company_id"));
		String dateStart = stringValue(payload.get("date_start"));
		String dateEnd = stringValue(payload.get("date_end"));
		boolean orderClassRestrict = booleanValue(payload.get("order_class_restrict_to_value"));
		String orderClassValue = stringValue(payload.get("order_class_value"));
		List<Long> actIds = actIdsFromPayload(payload.get("act_ids"));
		Long merchantId = longOrNull(payload.get("merchant_id"));
		long operatorId = longValue(payload.get("operator_id"));
		return new AdminGoodsDataFilter(
				companyId, dateStart, dateEnd, orderClassRestrict, orderClassValue, actIds, merchantId, operatorId);
	}

	private static long longValue(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static Long longOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	private static String stringValue(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static boolean booleanValue(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		return false;
	}

	private static List<Long> actIdsFromPayload(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}
}
