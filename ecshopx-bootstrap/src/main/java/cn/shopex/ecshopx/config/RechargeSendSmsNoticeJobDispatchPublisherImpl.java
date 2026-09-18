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

import cn.shopex.ecshopx.common.dispatch.DepositDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.RechargeSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
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
public class RechargeSendSmsNoticeJobDispatchPublisherImpl implements RechargeSendSmsNoticeJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public RechargeSendSmsNoticeJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishRechargeSendSmsNotice(long companyId, long userId, String mobile, long totalFeeFen) {
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("mobile must not be null or blank");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("companyId", companyId);
		payload.put("userId", userId);
		payload.put("mobile", mobile.trim());
		payload.put("totalFee", totalFeeFen);
		dispatchFacade.dispatchJob(
				DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));
	}
}
