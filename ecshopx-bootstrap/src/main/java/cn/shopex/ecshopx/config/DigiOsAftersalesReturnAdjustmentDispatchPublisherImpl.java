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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.DigiOsAftersalesReturnAdjustmentDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DigiOsDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DigiOsAftersalesReturnAdjustmentDispatchPublisherImpl
		implements DigiOsAftersalesReturnAdjustmentDispatchPublisher {

	private static final DispatchOptions ASYNC_OPTIONS = new DispatchOptions(
			DispatchMode.ASYNC, DispatchDriverType.REDIS, null, null, RetryPolicy.platformDefault());

	private final DispatchFacade dispatchFacade;

	public DigiOsAftersalesReturnAdjustmentDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(long companyId, long aftersalesBn) {
		if (companyId < 1L || aftersalesBn < 1L) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("aftersales_bn", aftersalesBn);
		dispatchFacade.publishEvent(
				DigiOsDispatchEventNames.EVENT_AFTERSALES_RETURN_ADJUSTMENT, payload, ASYNC_OPTIONS);
	}
}
