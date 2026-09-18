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

import cn.shopex.ecshopx.ali.service.alitemplate.AliOpenTemplateLibraryRedisAccessor;
import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.promotions.port.AliTemplateMsgSendDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AliTemplateMsgSendDispatchPublisherImpl implements AliTemplateMsgSendDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final AliOpenTemplateLibraryRedisAccessor aliOpenTemplateLibraryRedisAccessor;

	public AliTemplateMsgSendDispatchPublisherImpl(
			DispatchFacade dispatchFacade, AliOpenTemplateLibraryRedisAccessor aliOpenTemplateLibraryRedisAccessor) {
		this.dispatchFacade = dispatchFacade;
		this.aliOpenTemplateLibraryRedisAccessor = aliOpenTemplateLibraryRedisAccessor;
	}

	@Override
	public void publish(Map<String, Object> aliSendPayload, boolean listenerForceFire) {
		Map<String, Object> payload = new LinkedHashMap<>(aliSendPayload);
		int companyId = (int) Math.min(Math.max(toLong(payload.get("company_id")), 0L), Integer.MAX_VALUE);
		String scenesName = String.valueOf(payload.getOrDefault("scenes_name", "")).trim();
		java.time.Duration delay = null;
		if (!listenerForceFire && companyId > 0 && StringUtils.hasText(scenesName)) {
			delay =
					aliOpenTemplateLibraryRedisAccessor
							.resolveDispatchDelayMinutesFromTemplate(companyId, scenesName)
							.orElse(null);
		}
		payload.put("is_force_fire", Boolean.TRUE);
		dispatchFacade.dispatchJob(
				PromotionsDispatchJobNames.ALI_TEMPLATE_MSG_SEND,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						delay,
						RetryPolicy.platformDefault()));
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
