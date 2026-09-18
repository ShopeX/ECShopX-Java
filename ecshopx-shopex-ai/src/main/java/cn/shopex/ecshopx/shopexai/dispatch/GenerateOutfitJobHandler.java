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

package cn.shopex.ecshopx.shopexai.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.shopexai.service.OutfitAnyoneGenerationService;
import cn.shopex.ecshopx.shopexai.service.OutfitGenerationPendingCacheValue;
import cn.shopex.ecshopx.shopexai.service.OutfitGenerationResultCacheService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GenerateOutfitJobHandler implements DispatchHandler {

	private final OutfitAnyoneGenerationService outfitAnyoneGenerationService;
	private final OutfitGenerationResultCacheService outfitGenerationResultCacheService;

	public GenerateOutfitJobHandler(
			OutfitAnyoneGenerationService outfitAnyoneGenerationService,
			OutfitGenerationResultCacheService outfitGenerationResultCacheService) {
		this.outfitAnyoneGenerationService = outfitAnyoneGenerationService;
		this.outfitGenerationResultCacheService = outfitGenerationResultCacheService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		String personImageUrl = asString(payload.get("person_image_url"));
		String topGarmentUrl = asString(payload.get("top_garment_url"));
		String bottomGarmentUrl = asString(payload.get("bottom_garment_url"));
		String cacheKey = asString(payload.get("cache_key"));
		int cacheTtl = toInt(payload.get("cache_ttl"));

		try {
			Map<String, Object> generated =
					outfitAnyoneGenerationService.generateOutfit(
							personImageUrl, topGarmentUrl, bottomGarmentUrl);
			Map<String, Object> merged = new LinkedHashMap<>(generated);
			merged.put("job_completed", true);
			merged.put("completed_at", OutfitGenerationPendingCacheValue.formatNow());
			outfitGenerationResultCacheService.saveResult(cacheKey, merged, cacheTtl);
		} catch (RuntimeException e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("job_completed", true);
			err.put("error", true);
			err.put("person_image_url", personImageUrl);
			err.put("top_garment_url", topGarmentUrl);
			err.put("bottom_garment_url", bottomGarmentUrl);
			outfitGenerationResultCacheService.saveResult(cacheKey, err, cacheTtl);
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String asString(Object o) {
		return o == null ? "" : o.toString();
	}
}
