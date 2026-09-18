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

import cn.shopex.ecshopx.common.dispatch.BargainFinishSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BargainFinishSendSmsNoticeDispatchPublisherImpl implements BargainFinishSendSmsNoticeJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public BargainFinishSendSmsNoticeDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishBargainFinishSendSmsNotice(
			long companyId,
			long bargainOwnerUserId,
			Map<String, Object> bargainSnapshotFields,
			Long promotionEndTimeSec,
			Locale locale) {
		if (bargainSnapshotFields == null) {
			throw new BadRequestException("bargainSnapshotFields is required");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("bargain_owner_user_id", bargainOwnerUserId);
		if (promotionEndTimeSec != null) {
			payload.put("promotion_end_time_sec", promotionEndTimeSec);
		}
		payload.put("locale_language_tag", locale == null ? "" : locale.toLanguageTag());
		payload.putAll(bargainSnapshotFields);
		dispatchFacade.dispatchJob(
				PromotionsDispatchJobNames.BARGAIN_FINISH_SEND_SMS_NOTICE,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));
	}
}
