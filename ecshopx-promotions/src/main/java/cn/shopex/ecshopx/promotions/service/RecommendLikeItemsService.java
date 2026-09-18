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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.promotions.RecommendLikeAdminItemsFullListPort;
import cn.shopex.ecshopx.promotions.domain.RecommendLike;
import cn.shopex.ecshopx.promotions.mapper.RecommendLikeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecommendLikeItemsService {

	private final RecommendLikeMapper recommendLikeMapper;
	private final RecommendLikeAdminItemsFullListPort recommendLikeAdminItemsFullListPort;

	public RecommendLikeItemsService(
			RecommendLikeMapper recommendLikeMapper,
			RecommendLikeAdminItemsFullListPort recommendLikeAdminItemsFullListPort) {
		this.recommendLikeMapper = recommendLikeMapper;
		this.recommendLikeAdminItemsFullListPort = recommendLikeAdminItemsFullListPort;
	}

	public Map<String, Object> getRecommendLikeItems(long companyId, boolean isAllFullItems, String acceptLanguageHeader) {
		List<RecommendLike> rows =
				recommendLikeMapper.selectList(
						new LambdaQueryWrapper<RecommendLike>().eq(RecommendLike::getCompanyId, companyId));
		List<Long> itemIds = rows.stream().map(RecommendLike::getItemId).filter(Objects::nonNull).toList();
		if (!isAllFullItems) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("item_ids", itemIds);
			return out;
		}
		if (itemIds.isEmpty()) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		return recommendLikeAdminItemsFullListPort.queryFullItemsList(companyId, itemIds, acceptLanguageHeader);
	}
}
