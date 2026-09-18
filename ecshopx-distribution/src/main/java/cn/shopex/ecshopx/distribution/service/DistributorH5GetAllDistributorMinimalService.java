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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorH5GetAllDistributorMinimalService {

	private final DistributorMapper distributorMapper;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;

	public DistributorH5GetAllDistributorMinimalService(
			DistributorMapper distributorMapper,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService) {
		this.distributorMapper = distributorMapper;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
	}

	public Map<String, Object> getAllDistributor(
			long companyId,
			int regionauthId,
			int categoryId,
			long distributorIdFilter,
			String firstLetterRaw,
			int sortType,
			int page,
			int pageSize,
			String countryCode) {
		int p = Math.max(1, page);
		Page<Distributor> pageObj;
		if (pageSize > 0) {
			pageObj = new Page<>(p, pageSize);
		} else {
			pageObj = new Page<>(p, -1L, false);
		}
		pageObj.setSearchCount(false);

		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorSelf, 0)
				.eq(Distributor::getIsValid, "true")
				.select(
						Distributor::getDistributorId,
						Distributor::getName,
						Distributor::getLogo,
						Distributor::getFirstLetter,
						Distributor::getCreated);

		if (distributorIdFilter > 0L) {
			w.eq(Distributor::getDistributorId, distributorIdFilter);
		}
		if (regionauthId > 0) {
			w.eq(Distributor::getRegionauthId, (long) regionauthId);
		}
		if (StringUtils.hasText(firstLetterRaw == null ? "" : firstLetterRaw.trim())) {
			w.eq(Distributor::getFirstLetter, firstLetterRaw.trim());
		}

		if (sortType == 5) {
			w.orderByAsc(Distributor::getFirstLetter).orderByDesc(Distributor::getDistributorId);
		} else {
			w.orderByDesc(Distributor::getCreated).orderByDesc(Distributor::getDistributorId);
		}

		Map<String, Object> filterEcho = new LinkedHashMap<>();
		filterEcho.put("company_id", companyId);
		filterEcho.put("distributor_self", 0);
		filterEcho.put("is_valid", "true");
		if (distributorIdFilter > 0L) {
			filterEcho.put("distributor_id", distributorIdFilter);
		}
		if (categoryId > 0) {
			filterEcho.put("category_id", categoryId);
		}
		if (regionauthId > 0) {
			filterEcho.put("regionauth_id", regionauthId);
		}
		if (StringUtils.hasText(firstLetterRaw == null ? "" : firstLetterRaw.trim())) {
			filterEcho.put("first_letter", firstLetterRaw.trim());
		}

		Page<Distributor> result = distributorMapper.selectPage(pageObj, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Distributor v : result.getRecords()) {
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("distributor_id", v.getDistributorId());
			one.put("name", v.getName());
			one.put("logo", v.getLogo());
			one.put("first_letter", v.getFirstLetter());
			list.add(one);
		}

		distributorListOutsideLangReadService.overlayNameLogoFromOutsideLang(companyId, countryCode, list);

		Map<String, Object> body = new LinkedHashMap<>();
		// Response contract: total_count is always zero; page and page_size only bound the list query.
		body.put("total_count", 0L);
		body.put("list", list);
		body.put("tagList", List.of());
		body.put("filter", filterEcho);
		return body;
	}
}
