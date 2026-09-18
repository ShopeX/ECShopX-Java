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
import cn.shopex.ecshopx.orders.service.consumption.ConsumptionOrderJobService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Delegates consumption-order batch processing to {@link ConsumptionOrderJobService}.
 * <p>
 * Downstream member consumption aggregation may, when a member is promoted to a higher grade,
 * enqueue promotion-related follow-up jobs through {@link cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher}
 * and {@link cn.shopex.ecshopx.dispatch.DispatchFacade#dispatchJob(String, java.util.Map, cn.shopex.ecshopx.dispatch.DispatchOptions)},
 * consistent with existing platform wiring. This class does not change handler behavior or the
 * {@link cn.shopex.ecshopx.common.dispatch.DispatchHandler} contract.
 */
@Component
public class ConsumptionOrderJobHandler implements DispatchHandler {

	private final ConsumptionOrderJobService consumptionOrderJobService;

	public ConsumptionOrderJobHandler(ConsumptionOrderJobService consumptionOrderJobService) {
		this.consumptionOrderJobService = consumptionOrderJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> effective = payload == null ? Map.of() : payload;
		consumptionOrderJobService.execute(effective);
	}
}
