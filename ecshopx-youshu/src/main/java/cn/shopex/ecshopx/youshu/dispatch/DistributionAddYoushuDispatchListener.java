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

package cn.shopex.ecshopx.youshu.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuDistributionAddSrDataSyncService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DistributionAddYoushuDispatchListener implements DispatchListener {

	private final YoushuDistributionAddSrDataSyncService youshuDistributionAddSrDataSyncService;

	public DistributionAddYoushuDispatchListener(
			YoushuDistributionAddSrDataSyncService youshuDistributionAddSrDataSyncService) {
		this.youshuDistributionAddSrDataSyncService = youshuDistributionAddSrDataSyncService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Object raw = payload.get("entities");
		if (!(raw instanceof Map<?, ?> map)) {
			throw new BadRequestException("entities must be a map");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) map;
		youshuDistributionAddSrDataSyncService.syncAfterDistributionAdd(entities);
	}
}
