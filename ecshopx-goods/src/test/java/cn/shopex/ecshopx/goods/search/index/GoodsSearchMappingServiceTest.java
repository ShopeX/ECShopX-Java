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

package cn.shopex.ecshopx.goods.search.index;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoodsSearchMappingServiceTest {

	@Test
	void mappingsContainNestedSkusAndActivitiesAndIkOnItemName() {
		GoodsSearchMappingService svc = new GoodsSearchMappingService(null, new SearchEsProperties());
		Map<String, Object> m = svc.buildMappingsPayload();
		@SuppressWarnings("unchecked")
		Map<String, Object> props = (Map<String, Object>) m.get("properties");
		assertThat(props).containsKeys(
				"company_id", "distributor_id", "default_item_id",
				"item_name", "sale_price", "member_prices", "skus", "activities", "has_stock");
		@SuppressWarnings("unchecked")
		Map<String, Object> skus = (Map<String, Object>) props.get("skus");
		assertThat(skus.get("type")).isEqualTo("nested");
		@SuppressWarnings("unchecked")
		Map<String, Object> acts = (Map<String, Object>) props.get("activities");
		assertThat(acts.get("type")).isEqualTo("nested");
	}
}
