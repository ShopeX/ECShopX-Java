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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompanysResourcesListService {

	public enum LeftShopFilterMode {
		NONE,
		BOTH_MIN_MAX,
		MAX_ONLY,
		MIN_ONLY
	}

	private final ResourcesMapper resourcesMapper;

	public CompanysResourcesListService(ResourcesMapper resourcesMapper) {
		this.resourcesMapper = resourcesMapper;
	}

	public Map<String, Object> getResourceList(
			long companyId,
			int page,
			int limit,
			boolean isValidFilter,
			LeftShopFilterMode leftShopMode,
			Integer leftShopMinValue,
			Integer leftShopMaxValue,
			boolean leftDaysFilter,
			Integer leftDaysInt) {
		long nowEpoch = Instant.now().getEpochSecond();
		LambdaQueryWrapper<Resources> w = new LambdaQueryWrapper<>();
		w.eq(Resources::getCompanyId, companyId);
		if (isValidFilter) {
			w.ge(Resources::getLeftShopNum, 0).gt(Resources::getExpiredAt, nowEpoch);
		}
		switch (leftShopMode) {
			case BOTH_MIN_MAX -> {
				w.ge(Resources::getLeftShopNum, leftShopMinValue)
						.lt(Resources::getLeftShopNum, leftShopMaxValue);
			}
			case MAX_ONLY -> w.le(Resources::getLeftShopNum, leftShopMaxValue);
			case MIN_ONLY -> w.ge(Resources::getLeftShopNum, leftShopMinValue);
			case NONE -> {
			}
		}
		if (leftDaysFilter && leftDaysInt != null) {
			w.gt(Resources::getExpiredAt, nowEpoch)
					.lt(Resources::getExpiredAt, nowEpoch + leftDaysInt.longValue() * 86400L);
		}
		w.orderByAsc(Resources::getExpiredAt);
		Long total = resourcesMapper.selectCount(w);
		int totalCount = total == null ? 0 : total.intValue();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		if (totalCount == 0) {
			out.put("list", Collections.emptyList());
			return out;
		}
		Page<Resources> p = new Page<>(page, limit, false);
		resourcesMapper.selectPage(p, w);
		List<Resources> list = p.getRecords();
		out.put("list", list);
		return out;
	}
}
