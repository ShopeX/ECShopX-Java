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
import cn.shopex.ecshopx.orders.service.groups.UpdateGroupsActivityOrderBusService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UpdateGroupsActivityOrderDispatchListener implements DispatchListener {

	private final UpdateGroupsActivityOrderBusService updateGroupsActivityOrderBusService;

	public UpdateGroupsActivityOrderDispatchListener(
			UpdateGroupsActivityOrderBusService updateGroupsActivityOrderBusService) {
		this.updateGroupsActivityOrderBusService = updateGroupsActivityOrderBusService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		if (!isNormalGroupsTrade(payload)) {
			return;
		}
		updateGroupsActivityOrderBusService.handleTradeFinishRow(payload);
	}

	private static boolean isNormalGroupsTrade(Map<String, Object> payload) {
		Object raw = payload.get("trade_source_type");
		if (raw == null) {
			return false;
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) && "normal_groups".equalsIgnoreCase(s);
	}
}
