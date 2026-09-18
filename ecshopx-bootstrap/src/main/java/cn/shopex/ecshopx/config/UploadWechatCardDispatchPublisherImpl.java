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

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchJobNames;
import cn.shopex.ecshopx.common.kaquan.port.UploadWechatCardDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UploadWechatCardDispatchPublisherImpl implements UploadWechatCardDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public UploadWechatCardDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(String authorizerAppId, long companyId, List<Long> cardIds) {
		Map<String, Object> payload = new LinkedHashMap<>(4);
		payload.put("authorizer_appid", authorizerAppId);
		payload.put("company_id", companyId);
		payload.put("card_ids", cardIds);
		dispatchFacade.dispatchJob(
				KaquanDispatchJobNames.UPLOAD_WECHAT_CARD,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));
	}
}
