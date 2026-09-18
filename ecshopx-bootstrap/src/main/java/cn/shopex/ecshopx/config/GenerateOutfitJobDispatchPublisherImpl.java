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

import cn.shopex.ecshopx.common.dispatch.ShopexAiBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.shopexai.dispatch.GenerateOutfitJobDispatchPublisher;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GenerateOutfitJobDispatchPublisherImpl implements GenerateOutfitJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public GenerateOutfitJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueGenerateOutfit(
			String personImageUrl,
			String topGarmentUrl,
			String bottomGarmentUrl,
			String cacheKey,
			int cacheTtlSeconds,
			long companyId,
			long operatorId,
			long distributorId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("person_image_url", personImageUrl);
		map.put("top_garment_url", topGarmentUrl);
		map.put("bottom_garment_url", bottomGarmentUrl);
		map.put("cache_key", cacheKey);
		map.put("cache_ttl", cacheTtlSeconds);
		map.put("company_id", companyId);
		map.put("operator_id", operatorId);
		map.put("distributor_id", distributorId);

		dispatchFacade.dispatchJob(
				ShopexAiBundleDispatchJobNames.GENERATE_OUTFIT_JOB,
				map,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						new RetryPolicy(2, Duration.ofSeconds(3))));
	}
}
