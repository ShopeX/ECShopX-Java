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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.goods.domain.SearchHotword;
import cn.shopex.ecshopx.goods.mapper.SearchHotwordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 热门搜索查询（ECX-11390 F-005）：按店铺、渠道与当前有效时间返回完整排序结果。
 *
 * <p>有效期过滤在 SQL 完成（{@code start_time <= now <= end_time}），渠道过滤在内存完成；
 * 接口不按 8 条/两行截断，由 vshop/web 按容器自行取舍（PRD F-005/AC-004）。
 */
@Service
public class SearchHotwordQueryService {

	private final SearchHotwordMapper searchHotwordMapper;

	public SearchHotwordQueryService(SearchHotwordMapper searchHotwordMapper) {
		this.searchHotwordMapper = searchHotwordMapper;
	}

	/** 当前有效热词列表，顺序即展示顺序。 */
	public Map<String, Object> listEffective(long companyId, String channelRaw, long nowEpochSecond) {
		String channel = SearchHotwordChannels.normalizeQueryChannel(channelRaw);
		int now = (int) nowEpochSecond;

		List<SearchHotword> rows = searchHotwordMapper.selectList(new LambdaQueryWrapper<SearchHotword>()
				.eq(SearchHotword::getCompanyId, companyId)
				.le(SearchHotword::getStartTime, now)
				.ge(SearchHotword::getEndTime, now)
				.orderByDesc(SearchHotword::getSort)
				.orderByDesc(SearchHotword::getCreated)
				.orderByDesc(SearchHotword::getId));

		List<Map<String, Object>> list = new ArrayList<>();
		for (SearchHotword row : rows) {
			if (!SearchHotwordChannels.matchesChannel(row.getChannel(), channel)) {
				continue;
			}
			list.add(SearchHotwordRows.toPublicRow(row));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (long) list.size());
		out.put("list", list);
		return out;
	}
}
