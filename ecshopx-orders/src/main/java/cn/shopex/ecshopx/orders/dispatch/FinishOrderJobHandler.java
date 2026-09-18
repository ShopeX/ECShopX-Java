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
import cn.shopex.ecshopx.orders.service.finish.FinishOrderJobService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FinishOrderJobHandler implements DispatchHandler {

	private final FinishOrderJobService finishOrderJobService;

	public FinishOrderJobHandler(FinishOrderJobService finishOrderJobService) {
		this.finishOrderJobService = finishOrderJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> effective = payload == null ? Map.of() : payload;
		finishOrderJobService.execute(effective);
	}
}
