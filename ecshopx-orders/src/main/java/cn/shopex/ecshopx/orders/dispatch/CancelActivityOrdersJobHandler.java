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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.orders.service.CommunityActivityCancelOrderExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CancelActivityOrdersJobHandler implements DispatchHandler {

	private final CommunityActivityCancelOrderExecutor communityActivityCancelOrderExecutor;

	public CancelActivityOrdersJobHandler(CommunityActivityCancelOrderExecutor communityActivityCancelOrderExecutor) {
		this.communityActivityCancelOrderExecutor = communityActivityCancelOrderExecutor;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Object rawRows = payload.get("rows");
		if (!(rawRows instanceof List<?> list)) {
			return;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> rawMap)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> rowMap = (Map<String, Object>) rawMap;
			CommunityActivityCancelOrderRow row = rowFromMap(rowMap);
			communityActivityCancelOrderExecutor.execute(row);
		}
	}

	private static CommunityActivityCancelOrderRow rowFromMap(Map<String, Object> m) {
		CommunityActivityCancelOrderRow r = new CommunityActivityCancelOrderRow();
		r.setCompanyId(longVal(m.get("company_id")));
		r.setOrderId(longVal(m.get("order_id")));
		r.setCancelReason(stringVal(m.get("cancel_reason")));
		r.setUserId(longVal(m.get("user_id")));
		r.setMobile(stringVal(m.get("mobile")));
		String cancelFrom = stringVal(m.get("cancel_from"));
		r.setCancelFrom(StringUtils.hasText(cancelFrom) ? cancelFrom : "system");
		Object chiefRaw = m.get("chief_id");
		if (chiefRaw != null) {
			long cid = longVal(chiefRaw);
			if (cid > 0L) {
				r.setChiefId(cid);
			}
		}
		return r;
	}

	private static long longVal(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString();
	}
}
