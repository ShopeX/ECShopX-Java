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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("marketingCenterTasksCompleteHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.marketing-center.tasks-complete",
		name = "http-enabled",
		havingValue = "true")
public class MarketingCenterTasksCompleteHttpService implements MarketingCenterTasksCompletePort {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public MarketingCenterTasksCompleteHttpService(MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	@Override
	public void completeTasks(long companyId, Map<String, Object> params) {
		try {
			marketingCenterOpenApiSignedFormClient.post(companyId, "tasks.tasks.complete", params);
		} catch (Exception e) {
			// 营销中心 OpenAPI 调用或本地签名、序列化等环节发生异常时在此收口，不向调用方抛出业务异常；
			// 本方法无返回值，失败视为静默完成，导购主流程不受影响。
		}
	}
}
